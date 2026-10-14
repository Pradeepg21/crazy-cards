package com.satya.uno.game

import org.json.JSONArray
import org.json.JSONObject

/** Compact map form of [GameState] used both in Firestore and for saving offline games. */
object Codec {
    fun seatToMap(s: Seat): Map<String, Any> =
        mapOf("id" to s.id, "name" to s.name, "bot" to s.bot, "level" to s.level, "left" to s.left)

    fun seatFromMap(m: Map<*, *>) = Seat(
        id = m["id"] as? String ?: "",
        name = m["name"] as? String ?: "Player",
        bot = m["bot"] as? Boolean ?: false,
        level = (m["level"] as? Number)?.toInt() ?: 1,
        left = m["left"] as? Boolean ?: false,
    )

    fun toMap(s: GameState): Map<String, Any> = mapOf(
        "seats" to s.seats.map(::seatToMap),
        "hands" to s.hands.map { it.encode() },
        "draw" to s.draw.encode(),
        "discard" to s.discard.encode(),
        "color" to s.color.code.toString(),
        "turn" to s.turn,
        "dir" to s.dir,
        "phase" to s.phase.name,
        "dealer" to s.dealer,
        "drew" to s.drewPlayable,
        "uno" to s.unoSaid,
        "vuln" to s.vulnerable,
        "wd4By" to s.wd4By,
        "wd4Bad" to s.wd4Illegal,
        "fin" to s.finished,
        "scores" to s.scores,
        "round" to s.round,
        "ver" to s.version,
        "event" to s.event,
        "turnAt" to s.turnAt,
        "humanAt" to s.lastHumanAt,
    )

    fun fromMap(m: Map<*, *>): GameState {
        fun int(k: String, d: Int = 0) = (m[k] as? Number)?.toInt() ?: d
        fun long(k: String) = (m[k] as? Number)?.toLong() ?: 0L
        fun ints(k: String) = (m[k] as? List<*>)?.mapNotNull { (it as? Number)?.toInt() }.orEmpty()
        val seats = (m["seats"] as? List<*>).orEmpty().mapNotNull { (it as? Map<*, *>)?.let(::seatFromMap) }
        val hands = (m["hands"] as? List<*>).orEmpty().map { decodeCards(it as? String) }
        return GameState(
            seats = seats,
            hands = List(seats.size) { hands.getOrElse(it) { emptyList() } },
            draw = decodeCards(m["draw"] as? String),
            discard = decodeCards(m["discard"] as? String),
            color = (m["color"] as? String)?.firstOrNull()?.let(CardColor::of) ?: CardColor.WILD,
            turn = int("turn"),
            dir = int("dir", 1),
            phase = runCatching { Phase.valueOf(m["phase"] as String) }.getOrDefault(Phase.SHUFFLE),
            dealer = int("dealer"),
            drewPlayable = m["drew"] as? Boolean ?: false,
            unoSaid = m["uno"] as? Boolean ?: false,
            vulnerable = int("vuln", -1),
            wd4By = int("wd4By", -1),
            wd4Illegal = m["wd4Bad"] as? Boolean ?: false,
            finished = ints("fin"),
            scores = ints("scores").let { sc -> List(seats.size) { sc.getOrElse(it) { 0 } } },
            round = int("round"),
            version = long("ver"),
            event = m["event"] as? String ?: "",
            turnAt = long("turnAt"),
            lastHumanAt = long("humanAt"),
        )
    }

    fun toJson(s: GameState): String = JSONObject(toMap(s)).toString()

    fun fromJson(json: String): GameState = fromMap(jsonToMap(JSONObject(json)))

    private fun jsonToMap(o: JSONObject): Map<String, Any?> =
        o.keys().asSequence().associateWith { unwrap(o.get(it)) }

    private fun unwrap(v: Any?): Any? = when (v) {
        is JSONObject -> jsonToMap(v)
        is JSONArray -> List(v.length()) { unwrap(v.get(it)) }
        JSONObject.NULL -> null
        else -> v
    }
}
