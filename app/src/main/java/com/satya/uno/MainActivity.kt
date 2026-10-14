package com.satya.uno

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.satya.uno.play.OnlineGame
import com.satya.uno.ui.AppViewModel
import com.satya.uno.ui.AuthScreen
import com.satya.uno.ui.GameScreen
import com.satya.uno.ui.HomeScreen
import com.satya.uno.ui.JoinScreen
import com.satya.uno.ui.LobbyScreen
import com.satya.uno.ui.OfflineSetupScreen
import com.satya.uno.ui.RulesScreen
import com.satya.uno.ui.Screen
import com.satya.uno.ui.TableBrush
import com.satya.uno.ui.UnoTheme

class MainActivity : ComponentActivity() {
    private val vm: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        // The table is always dark, so keep system bar icons light regardless of the phone theme.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        setContent {
            UnoTheme {
                Box(Modifier.fillMaxSize().background(TableBrush)) {
                    AnimatedContent(vm.screen, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "screen") { screen ->
                        val game = vm.game
                        when (screen) {
                            Screen.HOME -> HomeScreen(vm)
                            Screen.AUTH -> AuthScreen(vm)
                            Screen.OFFLINE -> OfflineSetupScreen(vm)
                            Screen.JOIN -> JoinScreen(vm)
                            Screen.RULES -> RulesScreen(vm.prefs, onBack = vm::closeRules)
                            Screen.LOBBY -> if (game is OnlineGame) LobbyScreen(vm, game) else HomeScreen(vm)
                            Screen.GAME -> if (game != null) {
                                GameScreen(game, onRules = vm::openRules, onExit = vm::exitGame)
                            } else HomeScreen(vm)
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (vm.screen == Screen.HOME) vm.refreshRejoin()
    }
}
