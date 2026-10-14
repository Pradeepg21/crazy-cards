package com.satya.uno.data

import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import com.google.firebase.FirebaseApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Small key-value store: player name, saved offline game, the online room to rejoin, and the rules language. */
class Prefs(context: Context) {
    private val p: SharedPreferences = context.getSharedPreferences("uno", Context.MODE_PRIVATE)

    var name: String
        get() = p.getString("name", null).orEmpty()
        set(v) = p.edit().putString("name", v).apply()

    var offlineGame: String?
        get() = p.getString("offline", null)
        set(v) = p.edit().putString("offline", v).apply()

    var room: String?
        get() = p.getString("room", null)
        set(v) = p.edit().putString("room", v).apply()

    var botCount: Int
        get() = p.getInt("bots", 3)
        set(v) = p.edit().putInt("bots", v).apply()

    var botLevel: Int
        get() = p.getInt("level", 1)
        set(v) = p.edit().putInt("level", v).apply()

    var rulesLang: String?
        get() = p.getString("rulesLang", null)
        set(v) = p.edit().putString("rulesLang", v).apply()
}

/** Tracks whether the device currently has a validated internet connection. */
object Net {
    private val _online = MutableStateFlow(true)
    val online: StateFlow<Boolean> = _online

    fun init(context: Context) {
        val cm = context.getSystemService(ConnectivityManager::class.java) ?: return
        fun check(n: Network?) = cm.getNetworkCapabilities(n)?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        _online.value = check(cm.activeNetwork)
        cm.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { _online.value = true }
            override fun onLost(network: Network) { _online.value = check(cm.activeNetwork) }
            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
                _online.value = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            }
        })
    }
}

/** Firebase is optional: without google-services.json the app runs offline-only. */
object Cloud {
    var enabled = false
        private set

    fun init(context: Context) {
        enabled = FirebaseApp.getApps(context).isNotEmpty() || FirebaseApp.initializeApp(context) != null
    }
}

/** Turns Firebase/transaction exceptions into a short message a player can understand. */
fun Throwable.friendly(): String {
    var t: Throwable? = this
    while (t != null) {
        if (t is com.satya.uno.game.IllegalMove) return t.message ?: "Not allowed"
        if (t is RoomError) return t.message ?: "Something went wrong"
        t = t.cause
    }
    val m = message.orEmpty()
    return when {
        "offline" in m.lowercase() || "unavailable" in m.lowercase() || "network" in m.lowercase() ->
            "No internet connection. Reconnecting…"
        "PERMISSION_DENIED" in m -> "You don't have access to this game"
        else -> m.ifBlank { "Something went wrong" }
    }
}

class RoomError(msg: String) : Exception(msg)
