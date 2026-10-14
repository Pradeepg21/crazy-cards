package com.satya.uno.data

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.satya.uno.game.Action
import com.satya.uno.game.Codec
import com.satya.uno.game.Deck
import com.satya.uno.game.GameState
import com.satya.uno.game.Phase
import com.satya.uno.game.Rules
import com.satya.uno.game.Seat
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

data class Room(
    val code: String,
    val hostUid: String,
    val status: String,
    val seats: List<Seat>,
    val uids: List<String>,
    val state: GameState?,
    val reason: String,
    val touchedAt: Long,
) {
    /** Seats currently at the table (from the live game once it has started). */
    val table: List<Seat> get() = state?.seats ?: seats
    val ended get() = status == STATUS_ENDED

    companion object {
        const val STATUS_LOBBY = "lobby"
        const val STATUS_PLAYING = "playing"
        const val STATUS_ENDED = "ended"
    }
}

/** Firestore-backed rooms. Every change runs in a transaction so concurrent players never clobber each other. */
object Online {
    const val LOBBY_IDLE_MS = 15 * 60_000L
    const val GAME_IDLE_MS = 5 * 60_000L
    private const val CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"

    private val db get() = FirebaseFirestore.getInstance()
    private fun ref(code: String) = db.collection("games").document(code)

    fun normalize(code: String) = code.uppercase().filter { it in CODE_CHARS }.take(6)

    private fun parse(snap: DocumentSnapshot): Room? {
        if (!snap.exists()) return null
        @Suppress("UNCHECKED_CAST")
        return Room(
            code = snap.id,
            hostUid = snap.getString("hostUid").orEmpty(),
            status = snap.getString("status") ?: Room.STATUS_ENDED,
            seats = (snap.get("seats") as? List<*>).orEmpty().mapNotNull { (it as? Map<*, *>)?.let(Codec::seatFromMap) },
            uids = (snap.get("uids") as? List<String>).orEmpty(),
            state = (snap.get("state") as? Map<*, *>)?.let(Codec::fromMap),
            reason = snap.getString("reason").orEmpty(),
            touchedAt = snap.getLong("touchedAt") ?: 0L,
        )
    }

    suspend fun create(uid: String, name: String, now: Long): String {
        repeat(6) {
            val code = (1..6).map { CODE_CHARS[Deck.rng.nextInt(CODE_CHARS.length)] }.joinToString("")
            val created = db.runTransaction { tx ->
                val existing = parse(tx.get(ref(code)))
                if (existing != null && !existing.ended) return@runTransaction false
                tx.set(ref(code), mapOf(
                    "hostUid" to uid,
                    "status" to Room.STATUS_LOBBY,
                    "seats" to listOf(Codec.seatToMap(Seat(uid, name))),
                    "uids" to listOf(uid),
                    "state" to null,
                    "reason" to "",
                    "touchedAt" to now,
                    "createdAt" to FieldValue.serverTimestamp(),
                ))
                true
            }.await()
            if (created) return code
        }
        throw RoomError("Couldn't create a game. Try again")
    }

    /** Joins a lobby, or rejoins a game this player already belongs to. */
    suspend fun join(code: String, uid: String, name: String, now: Long) {
        db.runTransaction { tx ->
            val room = parse(tx.get(ref(code))) ?: throw RoomError("No game found with code $code")
            if (room.ended) throw RoomError("That game has ended")
            val state = room.state
            if (state != null) {
                val idx = state.seats.indexOfFirst { it.id == uid }
                if (idx < 0) throw RoomError("This game has already started")
                if (state.seats[idx].left) {
                    val seats = state.seats.toMutableList().also { it[idx] = it[idx].copy(left = false, bot = false) }
                    tx.update(ref(code), "state", Codec.toMap(state.copy(seats = seats, version = state.version + 1,
                        event = "$name rejoined", lastHumanAt = now)))
                }
                return@runTransaction
            }
            if (room.seats.any { it.id == uid }) return@runTransaction
            if (room.seats.size >= Rules.MAX_PLAYERS) throw RoomError("This game is full")
            tx.update(ref(code), mapOf(
                "seats" to (room.seats + Seat(uid, name)).map(Codec::seatToMap),
                "uids" to room.uids + uid,
                "touchedAt" to now,
            ))
        }.await()
    }

    suspend fun leave(code: String, uid: String, now: Long) {
        db.runTransaction { tx ->
            val room = parse(tx.get(ref(code))) ?: return@runTransaction
            if (room.ended) return@runTransaction
            val state = room.state
            if (state == null) {
                val seats = room.seats.filter { it.id != uid }
                val humans = seats.filter { !it.bot }
                if (humans.isEmpty()) {
                    tx.update(ref(code), mapOf("status" to Room.STATUS_ENDED, "reason" to "Everyone left"))
                } else {
                    tx.update(ref(code), mapOf(
                        "seats" to seats.map(Codec::seatToMap),
                        "uids" to room.uids - uid,
                        "hostUid" to if (room.hostUid == uid) humans.first().id else room.hostUid,
                        "touchedAt" to now,
                    ))
                }
                return@runTransaction
            }
            val idx = state.seats.indexOfFirst { it.id == uid }
            if (idx < 0) return@runTransaction
            val seats = state.seats.toMutableList().also { it[idx] = it[idx].copy(left = true) }
            if (seats.none { !it.automated }) {
                tx.update(ref(code), mapOf("status" to Room.STATUS_ENDED, "reason" to "Everyone left"))
            } else {
                tx.update(ref(code), "state", Codec.toMap(state.copy(seats = seats, version = state.version + 1,
                    event = "${state.seats[idx].name} left — the computer plays for them")))
            }
        }.await()
    }

    suspend fun addBot(code: String, uid: String, level: Int, now: Long) = lobbyEdit(code, uid, now) { seats ->
        if (seats.size >= Rules.MAX_PLAYERS) throw RoomError("The table is full")
        val n = seats.count { it.bot } + 1
        seats + Seat("bot-${Deck.rng.nextInt(1_000_000)}", "Bot $n", bot = true, level = level)
    }

    suspend fun removeBot(code: String, uid: String, botId: String, now: Long) = lobbyEdit(code, uid, now) { seats ->
        seats.filter { it.id != botId || !it.bot }
    }

    private suspend fun lobbyEdit(code: String, uid: String, now: Long, edit: (List<Seat>) -> List<Seat>) {
        db.runTransaction { tx ->
            val room = parse(tx.get(ref(code))) ?: throw RoomError("Game not found")
            if (room.hostUid != uid) throw RoomError("Only the host can do that")
            if (room.status != Room.STATUS_LOBBY) throw RoomError("The game has already started")
            tx.update(ref(code), mapOf("seats" to edit(room.seats).map(Codec::seatToMap), "touchedAt" to now))
        }.await()
    }

    suspend fun start(code: String, uid: String, now: Long) {
        db.runTransaction { tx ->
            val room = parse(tx.get(ref(code))) ?: throw RoomError("Game not found")
            if (room.hostUid != uid) throw RoomError("Only the host can start")
            if (room.status != Room.STATUS_LOBBY) return@runTransaction
            if (room.seats.size < 2) throw RoomError("Need at least 2 players — invite friends or add a bot")
            val state = GameState(seats = room.seats, phase = Phase.SHUFFLE, dealer = 0, turnAt = now, lastHumanAt = now,
                event = "Waiting for ${room.seats[0].name} to shuffle")
            tx.update(ref(code), mapOf("status" to Room.STATUS_PLAYING, "state" to Codec.toMap(state), "touchedAt" to now))
        }.await()
    }

    /**
     * Applies a move. With [expectVersion] the move is dropped if anything changed first, so two
     * devices automating the same bot turn can never both play it.
     */
    suspend fun act(code: String, expectVersion: Long?, human: Boolean, now: Long, build: (GameState) -> Action?): Boolean =
        db.runTransaction { tx ->
            val room = parse(tx.get(ref(code))) ?: throw RoomError("Game not found")
            if (room.status != Room.STATUS_PLAYING) return@runTransaction false
            val s = room.state ?: return@runTransaction false
            if (expectVersion != null && s.version != expectVersion) return@runTransaction false
            val action = build(s) ?: return@runTransaction false
            val next = Rules.apply(s, action, now).copy(
                version = s.version + 1,
                lastHumanAt = if (human) now else s.lastHumanAt,
            )
            tx.update(ref(code), mapOf("state" to Codec.toMap(next), "updatedAt" to FieldValue.serverTimestamp()))
            true
        }.await()

    suspend fun end(code: String, reason: String) {
        db.runTransaction { tx ->
            val room = parse(tx.get(ref(code))) ?: return@runTransaction
            if (!room.ended) tx.update(ref(code), mapOf("status" to Room.STATUS_ENDED, "reason" to reason))
        }.await()
    }

    suspend fun fetch(code: String): Room? = parse(ref(code).get().await())

    fun observe(code: String): Flow<Room?> = callbackFlow {
        val reg = ref(code).addSnapshotListener { snap, err ->
            if (err != null) return@addSnapshotListener // keep last state; listener retries on reconnect
            if (snap != null) trySend(parse(snap))
        }
        awaitClose { reg.remove() }
    }

    /** uid → last heartbeat in server millis. */
    data class Presence(val seen: Map<String, Long>, val myServerOffset: Long?)

    fun observePresence(code: String, myUid: String): Flow<Presence> = callbackFlow {
        val reg = ref(code).collection("presence").addSnapshotListener { snap, err ->
            if (err != null || snap == null) return@addSnapshotListener
            fun at(d: DocumentSnapshot) =
                d.getTimestamp("at", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)?.toDate()?.time ?: 0L
            // Our own heartbeat just confirmed by the server tells us how far our clock is from server time.
            val mine = snap.documentChanges.firstOrNull { it.document.id == myUid }?.document
            val offset = if (mine != null && !mine.metadata.hasPendingWrites() && !snap.metadata.isFromCache) {
                at(mine) - System.currentTimeMillis()
            } else null
            val seen = snap.documents.associate { it.id to at(it) }
            trySend(Presence(seen, offset))
        }
        awaitClose { reg.remove() }
    }

    suspend fun heartbeat(code: String, uid: String) {
        ref(code).collection("presence").document(uid)
            .set(mapOf("at" to FieldValue.serverTimestamp()), SetOptions.merge()).await()
    }
}
