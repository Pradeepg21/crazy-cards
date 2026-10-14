package com.satya.uno.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.satya.uno.data.Auth
import com.satya.uno.data.Cloud
import com.satya.uno.data.Room
import com.satya.uno.game.Bot
import com.satya.uno.game.Rules
import com.satya.uno.play.OnlineGame
import kotlinx.coroutines.launch

/** Shared page scaffold: table background, optional back button, centred scrolling column. */
@Composable
private fun Page(title: String? = null, onBack: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    if (onBack != null) BackHandler(onBack = onBack)
    Column(Modifier.fillMaxSize().background(TableBrush).systemBarsPadding().imePadding()) {
        if (onBack != null || title != null) {
            Row(Modifier.fillMaxWidth().padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
                if (onBack != null) TextButton(onClick = onBack) { Text("← Back", color = Color.White) }
                if (title != null) Text(title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        }
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(Modifier.widthIn(max = 460.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, content = content)
        }
    }
}

@Composable
private fun BigButton(text: String, color: Color = UnoColors.Red, enabled: Boolean = true, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().height(56.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color, contentColor = if (color == UnoColors.Yellow || color == UnoColors.Gold) Color.Black else Color.White),
    ) { Text(text, fontSize = 17.sp, fontWeight = FontWeight.Bold) }
}

@Composable
private fun ErrorText(msg: String?) {
    if (msg != null) {
        Text(msg, color = Color(0xFFFF8A80), textAlign = TextAlign.Center, modifier = Modifier.padding(vertical = 8.dp))
    }
}

@Composable
fun HomeScreen(vm: AppViewModel) {
    val user by Auth.user.collectAsStateWithLifecycle()
    Page {
        Spacer(Modifier.height(24.dp))
        Logo(width = 130.dp)
        Spacer(Modifier.height(20.dp))
        Text(
            "UNO", color = UnoColors.Yellow, fontSize = 56.sp, fontWeight = FontWeight.Black, fontStyle = FontStyle.Italic,
        )
        Text("Multiplayer", color = Color.White.copy(alpha = 0.8f), fontSize = 16.sp, letterSpacing = 4.sp)
        Spacer(Modifier.height(28.dp))

        vm.rejoin?.let { r ->
            BigButton("↩ Rejoin game ${r.code}", UnoColors.Gold) { vm.rejoinRoom() }
            Spacer(Modifier.height(12.dp))
        }
        if (vm.hasSavedGame) {
            BigButton("▶ Continue vs Computer", UnoColors.Gold) { vm.resumeOffline() }
            Spacer(Modifier.height(12.dp))
        }
        BigButton("Play vs Computer", UnoColors.Red) { vm.go(Screen.OFFLINE) }
        Spacer(Modifier.height(12.dp))
        BigButton("Create online game", UnoColors.Blue, enabled = !vm.busy) { vm.createRoom() }
        Spacer(Modifier.height(12.dp))
        BigButton("Join with code", UnoColors.Green, enabled = !vm.busy) { vm.go(Screen.JOIN) }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = vm::openRules, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(16.dp)) {
            Text("📖 Official rules", color = Color.White)
        }
        if (vm.busy) {
            Spacer(Modifier.height(12.dp))
            CircularProgressIndicator()
        }
        ErrorText(vm.error)
        Spacer(Modifier.height(24.dp))
        HorizontalDivider(color = Color.White.copy(alpha = 0.15f))
        Spacer(Modifier.height(12.dp))
        val u = user
        if (u != null) {
            Text("Signed in as ${Auth.displayName(u)}", color = Color.White.copy(alpha = 0.8f))
            TextButton(onClick = vm::signOut) { Text("Sign out") }
        } else if (Cloud.enabled) {
            TextButton(onClick = { vm.go(Screen.AUTH) }) { Text("Sign in for online play") }
        } else {
            Text("Offline edition — online play not configured", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
        }
    }
}

@Composable
fun AuthScreen(vm: AppViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var signUp by rememberSaveable { mutableStateOf(false) }
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var info by remember { mutableStateOf<String?>(null) }

    fun run(block: suspend () -> Result<Unit>, onOk: () -> Unit = vm::onSignedIn) {
        if (busy) return
        busy = true; error = null; info = null
        scope.launch {
            block().onSuccess { onOk() }.onFailure { error = it.message }
            busy = false
        }
    }

    Page(if (signUp) "Create account" else "Sign in", onBack = { vm.go(Screen.HOME) }) {
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = { run({ Auth.google(context) }) },
            enabled = !busy,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
        ) {
            Text("G", color = UnoColors.Blue, fontWeight = FontWeight.Black, fontSize = 20.sp)
            Spacer(Modifier.width(12.dp))
            Text("Continue with Google", fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(20.dp))
        Text("or use email", color = Color.White.copy(alpha = 0.6f))
        Spacer(Modifier.height(12.dp))
        if (signUp) {
            OutlinedTextField(
                name, { name = it.take(16) }, label = { Text("Display name") }, singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
            )
            Spacer(Modifier.height(8.dp))
        }
        OutlinedTextField(
            email, { email = it.trim() }, label = { Text("Email") }, singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            password, { password = it }, label = { Text("Password") }, singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = { TextButton(onClick = { showPassword = !showPassword }) { Text(if (showPassword) "Hide" else "Show") } },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
        )
        Spacer(Modifier.height(16.dp))
        val valid = email.contains('@') && password.length >= 6 && (!signUp || name.isNotBlank())
        BigButton(if (signUp) "Create account" else "Sign in", UnoColors.Red, enabled = valid && !busy) {
            if (signUp) run({ Auth.signUp(name, email, password) }) else run({ Auth.signIn(email, password) })
        }
        if (busy) { Spacer(Modifier.height(12.dp)); CircularProgressIndicator() }
        ErrorText(error)
        info?.let { Text(it, color = Color(0xFFB9F6CA), textAlign = TextAlign.Center) }
        if (!signUp) {
            TextButton(onClick = {
                if (!email.contains('@')) error = "Enter your email first" else
                    run({ Auth.resetPassword(email) }, onOk = { info = "Password reset email sent to $email" })
            }) { Text("Forgot password?") }
        }
        TextButton(onClick = { signUp = !signUp; error = null }) {
            Text(if (signUp) "Already have an account? Sign in" else "New here? Create an account")
        }
    }
}

@Composable
fun OfflineSetupScreen(vm: AppViewModel) {
    var name by rememberSaveable { mutableStateOf(vm.playerName().takeIf { it != "You" }.orEmpty()) }
    var bots by rememberSaveable { mutableIntStateOf(vm.prefs.botCount.coerceIn(1, Rules.MAX_PLAYERS - 1)) }
    var level by rememberSaveable { mutableIntStateOf(vm.prefs.botLevel.coerceIn(0, 2)) }
    Page("Play vs Computer", onBack = { vm.go(Screen.HOME) }) {
        OutlinedTextField(
            name, { name = it.take(16) }, label = { Text("Your name") }, singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
        )
        Spacer(Modifier.height(24.dp))
        Text("Computer players: $bots", color = Color.White, fontWeight = FontWeight.Bold)
        Slider(
            value = bots.toFloat(), onValueChange = { bots = it.toInt() },
            valueRange = 1f..(Rules.MAX_PLAYERS - 1).toFloat(), steps = Rules.MAX_PLAYERS - 3,
        )
        Spacer(Modifier.height(16.dp))
        Text("Difficulty", color = Color.White, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Bot.LEVELS.forEachIndexed { i, l -> FilterChip(selected = level == i, onClick = { level = i }, label = { Text(l) }) }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            when (level) {
                0 -> "Relaxed opponents who sometimes forget to say UNO."
                1 -> "Solid players who use action cards wisely."
                else -> "Sharp opponents: they save wilds, gang up on the leader, catch every missed UNO and might bluff a Wild Draw Four."
            },
            color = Color.White.copy(alpha = 0.7f), textAlign = TextAlign.Center, fontSize = 13.sp,
        )
        Spacer(Modifier.height(32.dp))
        BigButton("Start game") { vm.startOffline(name.ifBlank { "You" }, bots, level) }
        if (vm.hasSavedGame) {
            Text("Starting a new game replaces your saved one.", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
        }
    }
}

@Composable
fun JoinScreen(vm: AppViewModel) {
    var code by rememberSaveable { mutableStateOf("") }
    Page("Join a game", onBack = { vm.go(Screen.HOME) }) {
        Spacer(Modifier.height(16.dp))
        Text("Enter the 6-character code your friend shared", color = Color.White.copy(alpha = 0.8f), textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            code, { code = it.uppercase().filter(Char::isLetterOrDigit).take(6) },
            label = { Text("Game code") }, singleLine = true,
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 28.sp, letterSpacing = 6.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, keyboardType = KeyboardType.Ascii, imeAction = ImeAction.Go),
        )
        Spacer(Modifier.height(20.dp))
        BigButton(if (vm.busy) "Joining…" else "Join", UnoColors.Green, enabled = code.length == 6 && !vm.busy) { vm.joinRoom(code) }
        ErrorText(vm.error)
    }
}

@Composable
fun LobbyScreen(vm: AppViewModel, g: OnlineGame) {
    val context = LocalContext.current
    val room by g.room.collectAsStateWithLifecycle()
    val loaded by g.loaded.collectAsStateWithLifecycle()
    val ended by g.ended.collectAsStateWithLifecycle()
    val uid = Auth.user.value?.uid
    var botLevel by rememberSaveable { mutableIntStateOf(1) }
    var leaving by remember { mutableStateOf(false) }
    val snackMsg = remember { mutableStateOf<String?>(null) }
    androidx.compose.runtime.LaunchedEffect(g) { g.messages.collect { snackMsg.value = it } }

    Page("Game lobby", onBack = { leaving = true }) {
        val r = room
        if (!loaded || r == null) {
            Spacer(Modifier.height(48.dp)); CircularProgressIndicator()
            return@Page
        }
        val isHost = r.hostUid == uid
        Text("Share this code with your friends", color = Color.White.copy(alpha = 0.8f))
        Spacer(Modifier.height(8.dp))
        Box(
            Modifier.clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha = 0.08f))
                .border(2.dp, UnoColors.Gold, RoundedCornerShape(16.dp)).padding(horizontal = 24.dp, vertical = 12.dp),
        ) {
            Text(r.code, color = UnoColors.Gold, fontSize = 40.sp, fontWeight = FontWeight.Black, letterSpacing = 8.sp)
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = { shareCode(context, r.code) }, colors = ButtonDefaults.buttonColors(containerColor = UnoColors.Blue, contentColor = Color.White)) {
                Text("Share code")
            }
            OutlinedButton(onClick = { copyCode(context, r.code) }) { Text("Copy to clipboard", color = Color.White) }
        }
        Spacer(Modifier.height(24.dp))
        Text("Players (${r.seats.size}/${Rules.MAX_PLAYERS})", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        r.seats.forEachIndexed { i, seat ->
            Row(
                Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.06f)).padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(32.dp).clip(CircleShape).background(listOf(UnoColors.Red, UnoColors.Blue, UnoColors.Green, UnoColors.Gold)[i % 4]), contentAlignment = Alignment.Center) {
                    Text(if (seat.bot) "🤖" else seat.name.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(seat.name + if (seat.id == uid) " (you)" else "", color = Color.White)
                    val tag = when {
                        seat.id == r.hostUid -> "Host · deals first"
                        seat.bot -> "Computer · ${Bot.LEVELS[seat.level.coerceIn(0, 2)]}"
                        else -> null
                    }
                    if (tag != null) Text(tag, color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                }
                if (isHost && seat.bot) TextButton(onClick = { g.removeBot(seat.id) }) { Text("Remove", color = Color(0xFFFF8A80)) }
            }
        }
        Spacer(Modifier.height(16.dp))
        if (isHost) {
            if (r.seats.size < Rules.MAX_PLAYERS) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Bot.LEVELS.forEachIndexed { i, l -> FilterChip(selected = botLevel == i, onClick = { botLevel = i }, label = { Text(l) }) }
                    TextButton(onClick = { g.addBot(botLevel) }) { Text("+ Add bot") }
                }
                Spacer(Modifier.height(12.dp))
            }
            BigButton("Start game", UnoColors.Red, enabled = r.seats.size >= 2 && r.status == Room.STATUS_LOBBY) { g.start() }
            if (r.seats.size < 2) Text("Waiting for at least one more player…", color = Color.White.copy(alpha = 0.6f), modifier = Modifier.padding(top = 8.dp))
        } else {
            CircularProgressIndicator(Modifier.size(28.dp))
            Spacer(Modifier.height(8.dp))
            Text("Waiting for the host to start the game…", color = Color.White.copy(alpha = 0.8f))
        }
        snackMsg.value?.let { ErrorText(it) }
        Spacer(Modifier.height(12.dp))
        TextButton(onClick = vm::openRules) { Text("📖 Rules") }
    }

    if (leaving) {
        AlertDialog(
            onDismissRequest = { leaving = false },
            title = { Text("Leave the lobby?") },
            confirmButton = { TextButton(onClick = { leaving = false; vm.exitGame(quit = true) }) { Text("Leave") } },
            dismissButton = { TextButton(onClick = { leaving = false }) { Text("Stay") } },
        )
    }
    ended?.let { reason ->
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Game closed") },
            text = { Text(reason) },
            confirmButton = { TextButton(onClick = { vm.exitGame(quit = false) }) { Text("OK") } },
        )
    }
}

private fun shareText(code: String) =
    "Let's play UNO! Open the UNO Multiplayer app, tap \"Join with code\" and enter: $code"

private fun shareCode(context: Context, code: String) {
    val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, shareText(code))
    context.startActivity(Intent.createChooser(send, "Share game code"))
}

private fun copyCode(context: Context, code: String) {
    context.getSystemService(ClipboardManager::class.java)?.setPrimaryClip(ClipData.newPlainText("UNO game code", code))
    Toast.makeText(context, "Code $code copied", Toast.LENGTH_SHORT).show()
}
