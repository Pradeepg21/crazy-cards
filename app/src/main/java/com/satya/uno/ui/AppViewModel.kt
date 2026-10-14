package com.satya.uno.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.satya.uno.data.Auth
import com.satya.uno.data.Cloud
import com.satya.uno.data.Online
import com.satya.uno.data.Prefs
import com.satya.uno.data.Room
import com.satya.uno.data.friendly
import com.satya.uno.game.Codec
import com.satya.uno.game.GameState
import com.satya.uno.game.Phase
import com.satya.uno.game.Seat
import com.satya.uno.play.GameController
import com.satya.uno.play.LocalGame
import com.satya.uno.play.OnlineGame
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

enum class Screen { HOME, AUTH, OFFLINE, JOIN, LOBBY, GAME, RULES }

class AppViewModel(app: Application) : AndroidViewModel(app) {
    val prefs = Prefs(app)
    var screen by mutableStateOf(Screen.HOME)
        private set
    private var beforeRules = Screen.HOME
    var game by mutableStateOf<GameController?>(null)
        private set
    var busy by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
    var rejoin by mutableStateOf<Room?>(null)
        private set
    var hasSavedGame by mutableStateOf(prefs.offlineGame != null)
        private set
    private var afterAuth: (() -> Unit)? = null
    private var roomWatch: Job? = null

    val online get() = game as? OnlineGame

    init {
        viewModelScope.launch { Auth.user.collect { refreshRejoin() } }
    }

    fun go(s: Screen) {
        error = null
        screen = s
    }

    fun openRules() {
        if (screen != Screen.RULES) beforeRules = screen
        screen = Screen.RULES
    }

    fun closeRules() {
        screen = beforeRules
    }

    fun playerName(): String =
        prefs.name.ifBlank { Auth.user.value?.let(Auth::displayName) ?: "You" }

    // ---- Offline ----

    fun startOffline(name: String, bots: Int, level: Int) {
        prefs.name = name.trim().take(16)
        prefs.botCount = bots
        prefs.botLevel = level
        val names = BOT_NAMES.shuffled().take(bots)
        val seats = listOf(Seat("me", prefs.name.ifBlank { "You" })) +
            names.mapIndexed { i, n -> Seat("bot$i", n, bot = true, level = level) }
        val now = System.currentTimeMillis()
        val state = GameState(seats = seats, phase = Phase.SHUFFLE, dealer = 0, lastHumanAt = now, event = "You're the dealer — shuffle to start")
        openLocal(state)
    }

    fun resumeOffline() {
        val json = prefs.offlineGame ?: return
        val state = runCatching { Codec.fromJson(json) }.getOrNull()
        if (state == null || state.seats.size < 2) {
            prefs.offlineGame = null
            hasSavedGame = false
            error = "The saved game couldn't be loaded"
            return
        }
        openLocal(state)
    }

    private fun openLocal(state: GameState) {
        game?.close()
        game = LocalGame(viewModelScope, prefs, state)
        go(Screen.GAME)
    }

    // ---- Online ----

    private fun requireUser(then: () -> Unit) {
        if (!Cloud.enabled) {
            error = "Online play isn't configured in this build"
            return
        }
        if (Auth.user.value != null) then() else {
            afterAuth = then
            go(Screen.AUTH)
        }
    }

    fun onSignedIn() {
        val next = afterAuth
        afterAuth = null
        go(Screen.HOME)
        next?.invoke()
    }

    fun signOut() {
        Auth.signOut()
        rejoin = null
    }

    fun createRoom() = requireUser {
        val user = Auth.user.value ?: return@requireUser
        launchBusy {
            val code = Online.create(user.uid, Auth.displayName(user), System.currentTimeMillis())
            openOnline(code, user.uid)
        }
    }

    fun joinRoom(raw: String) = requireUser {
        val user = Auth.user.value ?: return@requireUser
        val code = Online.normalize(raw)
        if (code.length != 6) {
            error = "Game codes have 6 characters"
            return@requireUser
        }
        launchBusy {
            Online.join(code, user.uid, Auth.displayName(user), System.currentTimeMillis())
            openOnline(code, user.uid)
        }
    }

    fun rejoinRoom() {
        val r = rejoin ?: return
        joinRoom(r.code)
    }

    private fun openOnline(code: String, uid: String) {
        game?.close()
        val g = OnlineGame(viewModelScope, prefs, code, uid)
        game = g
        go(Screen.LOBBY)
        roomWatch?.cancel()
        roomWatch = viewModelScope.launch {
            g.room.collect { r ->
                if (r?.status == Room.STATUS_PLAYING && screen == Screen.LOBBY) go(Screen.GAME)
            }
        }
    }

    /** quit = give up the seat for good (online) or discard the saved game (offline). */
    fun exitGame(quit: Boolean) {
        roomWatch?.cancel()
        game?.let { if (quit) it.leave() else it.close() }
        game = null
        hasSavedGame = prefs.offlineGame != null
        go(Screen.HOME)
        refreshRejoin()
    }

    fun refreshRejoin() {
        val code = prefs.room
        val user = Auth.user.value
        if (code == null || user == null || !Cloud.enabled) {
            rejoin = null
            return
        }
        viewModelScope.launch {
            val room = runCatching { Online.fetch(code) }.getOrNull()
            val now = System.currentTimeMillis()
            val stale = room?.state?.let { now - it.lastHumanAt > Online.GAME_IDLE_MS + 60_000 } ?: false
            val mine = room?.table?.any { it.id == user.uid } == true
            rejoin = if (room != null && !room.ended && mine && !stale) room else null
            if (room != null && (room.ended || !mine || stale)) prefs.room = null
        }
    }

    private fun launchBusy(block: suspend () -> Unit) {
        if (busy) return
        busy = true
        error = null
        viewModelScope.launch {
            try { block() } catch (e: Exception) { error = e.friendly() } finally { busy = false }
        }
    }

    override fun onCleared() {
        game?.close()
    }

    companion object {
        val BOT_NAMES = listOf("Ava", "Leo", "Mia", "Kai", "Zoe", "Max", "Ivy", "Sam", "Ruby", "Finn", "Nora", "Arjun", "Priya", "Omar")
    }
}
