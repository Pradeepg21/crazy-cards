package com.satya.uno.play

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.satya.uno.data.Net
import com.satya.uno.data.Online
import com.satya.uno.data.Prefs
import com.satya.uno.data.Room
import com.satya.uno.data.friendly
import com.satya.uno.game.Action
import com.satya.uno.game.Director
import com.satya.uno.game.Phase
import com.satya.uno.game.Plan
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/**
 * One online room as seen from this device.
 *
 * Disconnect handling: every device heartbeats every [HEARTBEAT_MS]; a player silent for [AWAY_MS]
 * is shown as offline and the computer plays their turns until they come back. Bots, timeouts and
 * stand-in moves are run by a single "lead" device (the first connected human at the table). If the
 * lead drops, the next connected human takes over automatically, and version-checked transactions
 * make sure an automated move is never applied twice.
 */
class OnlineGame(
    parent: CoroutineScope,
    private val prefs: Prefs,
    val code: String,
    private val uid: String,
) : GameController {
    private val scope = CoroutineScope(parent.coroutineContext + SupervisorJob())
    val room = MutableStateFlow<Room?>(null)
    val loaded = MutableStateFlow(false)
    override val view = MutableStateFlow<GameView?>(null)
    override val messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    override val ended = MutableStateFlow<String?>(null)

    private var presence = Online.Presence(emptyMap(), null)
    private var presenceLoaded = false
    private var offset = 0L
    private var busy = false
    private var autoJob: Job? = null
    private var autoKey: String? = null

    private fun serverNow() = System.currentTimeMillis() + offset

    init {
        prefs.room = code
        scope.launch {
            Online.observe(code).collect {
                room.value = it
                loaded.value = true
                refresh()
            }
        }
        scope.launch {
            Online.observePresence(code, uid).collect { p ->
                p.myServerOffset?.let { offset = it }
                presence = p
                presenceLoaded = true
                refresh()
            }
        }
        scope.launch { Net.online.collect { refresh() } }
        // Heartbeat only while the app is on screen; backgrounded players show as away.
        scope.launch {
            ProcessLifecycleOwner.get().lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (true) {
                    runCatching { Online.heartbeat(code, uid) }
                    delay(HEARTBEAT_MS)
                }
            }
        }
        scope.launch {
            while (true) {
                delay(2_000)
                refresh()
            }
        }
    }

    private fun refresh() {
        if (!loaded.value || ended.value != null) return
        val r = room.value
        if (r == null) return finish("This game no longer exists")
        if (r.ended) return finish(r.reason.ifBlank { "The game has ended" })
        val connected = Net.online.value
        val s = r.state
        if (s == null) {
            if (connected && presenceLoaded && serverNow() - r.touchedAt > Online.LOBBY_IDLE_MS) {
                endRoom("Closed after 15 minutes of inactivity")
            }
            return
        }
        val me = s.seats.indexOfFirst { it.id == uid }
        if (me < 0) return finish("You're no longer part of this game")

        val now = serverNow()
        val away = if (!presenceLoaded) emptySet() else s.seats.indices.filter { i ->
            val seat = s.seats[i]
            !seat.automated && !(i == me && connected) && now - (presence.seen[seat.id] ?: 0L) > AWAY_MS
        }.toSet()
        val actor = s.actor
        val dealing = s.phase == Phase.SHUFFLE || s.phase == Phase.ROUND_OVER
        val deadline = if (!s.seats[actor].automated && actor !in away) {
            s.turnAt + (if (dealing) Director.DEAL_MS else Director.TURN_MS) - offset
        } else null
        view.value = GameView(s, me, code, away, connected, deadline, busy)

        val lead = s.seats.indices.firstOrNull { !s.seats[it].automated && it !in away }
        if (lead != me || !connected || !presenceLoaded) return cancelAuto()
        if (now - s.lastHumanAt > Online.GAME_IDLE_MS) return endRoom("Closed after 5 minutes without any moves")
        schedule(Director.plan(s, now, isAway = { it in away }, timed = true), s.version)
    }

    private fun schedule(plan: Plan?, version: Long) {
        if (plan?.key == autoKey && autoJob?.isActive == true) return
        autoJob?.cancel()
        autoKey = plan?.key
        if (plan == null) return
        autoJob = scope.launch {
            delay(plan.delayMs)
            runCatching { Online.act(code, version, human = false, now = serverNow()) { plan.action } }
        }
    }

    private fun cancelAuto() {
        autoJob?.cancel()
        autoKey = null
    }

    private fun finish(reason: String) {
        cancelAuto()
        if (prefs.room == code) prefs.room = null
        ended.value = reason
    }

    private fun endRoom(reason: String) {
        scope.launch { runCatching { Online.end(code, reason) } }
    }

    override fun act(action: Action) {
        if (busy) return
        if (!Net.online.value) {
            messages.tryEmit("No internet connection. Reconnecting…")
            return
        }
        busy = true
        refresh()
        scope.launch {
            try {
                val ok = Online.act(code, null, human = true, now = serverNow()) { s ->
                    val my = s.seats.indexOfFirst { it.id == uid }
                    if (my < 0) null else action.withSeat(my)
                }
                if (!ok) messages.tryEmit("The game moved on — try again")
            } catch (e: Exception) {
                messages.tryEmit(e.friendly())
            } finally {
                busy = false
                refresh()
            }
        }
    }

    fun lobby(block: suspend (now: Long) -> Unit) {
        scope.launch {
            try { block(serverNow()) } catch (e: Exception) { messages.tryEmit(e.friendly()) }
        }
    }

    fun start() = lobby { Online.start(code, uid, it) }
    fun addBot(level: Int) = lobby { Online.addBot(code, uid, level, it) }
    fun removeBot(id: String) = lobby { Online.removeBot(code, uid, id, it) }

    override fun leave() {
        if (prefs.room == code) prefs.room = null
        cancelAuto()
        // Run on the parent scope so leaving completes even as this controller closes.
        val now = serverNow()
        CoroutineScope(SupervisorJob()).launch { runCatching { Online.leave(code, uid, now) } }
        close()
    }

    override fun close() {
        scope.cancel()
    }

    companion object {
        const val HEARTBEAT_MS = 10_000L
        const val AWAY_MS = 25_000L
    }
}

fun Action.withSeat(seat: Int): Action = when (this) {
    is Action.Deal -> copy(seat = seat)
    is Action.Play -> copy(seat = seat)
    is Action.Draw -> copy(seat = seat)
    is Action.Pass -> copy(seat = seat)
    is Action.ChooseColor -> copy(seat = seat)
    is Action.Challenge -> copy(seat = seat)
    is Action.Accept -> copy(seat = seat)
    is Action.CallUno -> copy(seat = seat)
    is Action.Catch -> copy(seat = seat)
}
