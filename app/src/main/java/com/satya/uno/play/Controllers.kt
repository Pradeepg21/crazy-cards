package com.satya.uno.play

import com.satya.uno.data.Prefs
import com.satya.uno.game.Action
import com.satya.uno.game.Codec
import com.satya.uno.game.Director
import com.satya.uno.game.GameState
import com.satya.uno.game.IllegalMove
import com.satya.uno.game.Rules
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** Everything the game table needs to draw itself. */
data class GameView(
    val state: GameState,
    val mySeat: Int,
    val code: String? = null,
    val away: Set<Int> = emptySet(),
    val connected: Boolean = true,
    /** Local-clock time when the current turn auto-plays, or null if untimed. */
    val deadline: Long? = null,
    val busy: Boolean = false,
)

interface GameController {
    val view: StateFlow<GameView?>
    val messages: SharedFlow<String>
    /** Non-null once the game is over for this device (e.g. closed for inactivity). */
    val ended: StateFlow<String?>
    fun act(action: Action)
    fun leave()
    fun close()
}

/** Single-device game against computer players. Saved after every move so it can be resumed. */
class LocalGame(
    private val scope: CoroutineScope,
    private val prefs: Prefs,
    initial: GameState,
) : GameController {
    private val state = MutableStateFlow(initial)
    override val view = MutableStateFlow<GameView?>(GameView(initial, mySeat = 0))
    override val messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    override val ended = MutableStateFlow<String?>(null)
    private var autoJob: Job? = null
    private var autoKey: String? = null

    init {
        schedule()
    }

    override fun act(action: Action) = apply(action, human = true)

    private fun apply(action: Action, human: Boolean) {
        val s = state.value
        try {
            val now = System.currentTimeMillis()
            val next = Rules.apply(s, action, now).copy(version = s.version + 1, lastHumanAt = if (human) now else s.lastHumanAt)
            state.value = next
            view.value = GameView(next, mySeat = 0)
            prefs.offlineGame = Codec.toJson(next)
            schedule()
        } catch (e: IllegalMove) {
            if (human) messages.tryEmit(e.message ?: "Not allowed")
        }
    }

    private fun schedule() {
        val s = state.value
        val plan = Director.plan(s, System.currentTimeMillis(), isAway = { false }, timed = false)
        if (plan?.key == autoKey && autoJob?.isActive == true) return
        autoJob?.cancel()
        autoKey = plan?.key
        if (plan == null) return
        autoJob = scope.launch {
            delay(plan.delayMs)
            if (state.value.version == s.version) apply(plan.action, human = false)
        }
    }

    override fun leave() {
        prefs.offlineGame = null
        close()
    }

    override fun close() {
        autoJob?.cancel()
    }
}
