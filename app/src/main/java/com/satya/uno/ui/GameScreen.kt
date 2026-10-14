package com.satya.uno.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.satya.uno.game.Action
import com.satya.uno.game.CardColor
import com.satya.uno.game.GameState
import com.satya.uno.game.Phase
import com.satya.uno.game.Rules
import com.satya.uno.play.GameController
import com.satya.uno.play.GameView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun GameScreen(controller: GameController, onRules: () -> Unit, onExit: (quit: Boolean) -> Unit) {
    val view by controller.view.collectAsStateWithLifecycle()
    val ended by controller.ended.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var confirmLeave by remember { mutableStateOf(false) }

    LaunchedEffect(controller) {
        controller.messages.collect { snackbar.currentSnackbarData?.dismiss(); snackbar.showSnackbar(it) }
    }
    // Keep the screen awake while a game is on.
    val androidView = LocalView.current
    DisposableEffect(Unit) {
        androidView.keepScreenOn = true
        onDispose { androidView.keepScreenOn = false }
    }
    BackHandler { confirmLeave = true }

    Box(Modifier.fillMaxSize().background(TableBrush)) {
        val v = view
        if (v == null) {
            Text("Loading game…", Modifier.align(Alignment.Center), color = Color.White)
        } else {
            Table(v, controller, onRules = onRules, onLeave = { confirmLeave = true })
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(bottom = 140.dp))
    }

    if (confirmLeave) {
        val online = view?.code != null
        AlertDialog(
            onDismissRequest = { confirmLeave = false },
            title = { Text(if (online) "Leave this game?" else "Exit game?") },
            text = {
                Text(
                    if (online) "Step out and the computer plays your turns — you can rejoin from the home screen. Quit gives up your seat for good."
                    else "Your game is saved — continue any time from the home screen.",
                )
            },
            confirmButton = {
                TextButton(onClick = { confirmLeave = false; onExit(false) }) { Text(if (online) "Step out" else "Save & exit") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { confirmLeave = false; onExit(true) }) { Text(if (online) "Quit game" else "End game", color = UnoColors.Red) }
                    TextButton(onClick = { confirmLeave = false }) { Text("Stay") }
                }
            },
        )
    }

    ended?.let { reason ->
        AlertDialog(
            onDismissRequest = {},
            properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
            title = { Text("Game closed") },
            text = { Text(reason) },
            confirmButton = { TextButton(onClick = { onExit(false) }) { Text("OK") } },
        )
    }
}

@Composable
private fun Table(v: GameView, controller: GameController, onRules: () -> Unit, onLeave: () -> Unit) {
    val s = v.state
    val me = v.mySeat
    val haptic = LocalHapticFeedback.current
    val myTurn = s.actor == me
    var unoArmed by remember(s.round, s.turn, s.hands[me].size) { mutableStateOf(false) }
    var pendingWild by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(myTurn, s.version) {
        if (myTurn && s.phase != Phase.ROUND_OVER) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    Column(Modifier.fillMaxSize().systemBarsPadding()) {
        // Top bar
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onLeave) { Text("✕ Exit", color = Color.White) }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(if (v.code != null) "Room ${v.code}" else "vs Computer", color = Color.White, fontWeight = FontWeight.Bold)
                if (s.round > 0) Text("Round ${s.round}", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
            }
            TextButton(onClick = onRules) { Text("Rules", color = UnoColors.Gold) }
        }
        if (!v.connected) {
            Text(
                "Connection lost — reconnecting… If you're away more than 25 seconds the computer plays your turn.",
                Modifier.fillMaxWidth().background(UnoColors.Red).padding(8.dp),
                color = Color.White, fontSize = 12.sp, textAlign = TextAlign.Center,
            )
        }

        // Opponents, in turn order starting after me.
        val n = s.seats.size
        val order = (1 until n).map { (me + it) % n }
        LazyRow(
            Modifier.fillMaxWidth().padding(vertical = 6.dp),
            contentPadding = PaddingValues(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        ) {
            itemsIndexed(order, key = { _, i -> s.seats[i].id }) { _, i -> PlayerChip(s, i, v) }
        }

        // Centre of the table
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            if (s.phase == Phase.SHUFFLE || s.phase == Phase.ROUND_OVER) {
                RoundPanel(s, me) { controller.act(Action.Deal(me)) }
            } else {
                Pile(s, me, v.busy) { controller.act(Action.Draw(me)) }
            }
        }

        // My status
        val now by produceState(System.currentTimeMillis(), v.deadline) {
            while (true) { value = System.currentTimeMillis(); delay(500) }
        }
        val deadline = v.deadline
        if (deadline != null && s.phase != Phase.ROUND_OVER && s.phase != Phase.SHUFFLE) {
            val left = ((deadline - now) / 1000).coerceAtLeast(0)
            val frac = (left / 30f).coerceIn(0f, 1f)
            LinearProgressIndicator(
                progress = { frac },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(4.dp).clip(RoundedCornerShape(2.dp)),
                color = if (left <= 8) UnoColors.Red else UnoColors.Gold,
                trackColor = Color.White.copy(alpha = 0.15f),
            )
        }
        val status = when {
            s.phase == Phase.ROUND_OVER || s.phase == Phase.SHUFFLE -> ""
            me in s.finished -> "You finished ${Rules.ordinal(s.finished.indexOf(me))}! Watching the rest…"
            myTurn && s.phase == Phase.CHOOSE_COLOR -> "Choose the starting color"
            myTurn && s.drewPlayable -> "You drew a playable card — play it or keep it"
            myTurn && s.playableIndices(me).isEmpty() -> "No match — tap the deck to draw"
            myTurn -> "Your turn"
            else -> "Waiting for ${s.seats[s.actor].name}…"
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                status, Modifier.weight(1f),
                color = if (myTurn) UnoColors.Gold else Color.White.copy(alpha = 0.8f),
                fontWeight = if (myTurn) FontWeight.Bold else FontWeight.Normal, maxLines = 2,
            )
            Text("Score ${s.scores.getOrElse(me) { 0 }}", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
        }

        // Action buttons
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp).heightIn(min = 48.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val hand = s.hands[me]
            val canArm = myTurn && s.phase == Phase.PLAY && hand.size == 2 && s.playableIndices(me).isNotEmpty()
            val late = s.vulnerable == me
            if (canArm || late || unoArmed) {
                UnoButton(armed = unoArmed, urgent = late) {
                    if (late) controller.act(Action.CallUno(me)) else unoArmed = !unoArmed
                }
            }
            if (myTurn && s.drewPlayable) {
                OutlinedButton(onClick = { controller.act(Action.Pass(me)) }) { Text("Keep & pass", color = Color.White) }
            }
            val vuln = s.vulnerable
            if (vuln >= 0 && vuln != me && s.phase != Phase.ROUND_OVER) {
                Button(
                    onClick = { controller.act(Action.Catch(me)) },
                    colors = ButtonDefaults.buttonColors(containerColor = UnoColors.Red, contentColor = Color.White),
                ) { Text("Catch ${s.seats[vuln].name}! +2", fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis) }
            }
        }

        Hand(s, me, unoArmed) { idx ->
            val card = s.hands[me][idx]
            when {
                !myTurn || s.phase != Phase.PLAY -> {}
                idx !in s.playableIndices(me) -> haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                card.isWild -> pendingWild = idx
                else -> controller.act(Action.Play(me, idx, card.code, null, unoArmed))
            }
        }
    }

    pendingWild?.let { idx ->
        ColorPicker(title = "Choose a color", onDismiss = { pendingWild = null }) { color ->
            pendingWild = null
            val card = s.hands[me].getOrNull(idx) ?: return@ColorPicker
            controller.act(Action.Play(me, idx, card.code, color, unoArmed))
        }
    }
    if (myTurn && s.phase == Phase.CHOOSE_COLOR) {
        ColorPicker(title = "First card is Wild — you pick the color", onDismiss = null) { controller.act(Action.ChooseColor(me, it)) }
    }
    if (myTurn && s.phase == Phase.CHALLENGE) {
        val by = s.seats.getOrNull(s.wd4By)?.name ?: "A player"
        AlertDialog(
            onDismissRequest = {},
            properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
            title = { Text("$by played Wild Draw Four") },
            text = {
                Text("Challenge it? If $by had a card matching the previous color, they draw 4 and you play on. If the play was legal, you draw 6 and lose your turn.")
            },
            confirmButton = { TextButton(onClick = { controller.act(Action.Challenge(me)) }) { Text("Challenge") } },
            dismissButton = { TextButton(onClick = { controller.act(Action.Accept(me)) }) { Text("Accept (+4)") } },
        )
    }
}

@Composable
private fun PlayerChip(s: GameState, i: Int, v: GameView) {
    val seat = s.seats[i]
    val active = s.actor == i && s.phase != Phase.ROUND_OVER
    val pulse = rememberInfiniteTransition(label = "turn")
    val glow by pulse.animateFloat(0.4f, 1f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "glow")
    val cards = s.hands[i].size
    val place = s.finished.indexOf(i)
    Column(
        Modifier
            .width(if (isTablet()) 104.dp else 78.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = if (active) 0.16f else 0.06f))
            .border(2.dp, if (active) UnoColors.Gold.copy(alpha = glow) else Color.Transparent, RoundedCornerShape(12.dp))
            .padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(36.dp).clip(CircleShape).background(avatarColor(i)),
            contentAlignment = Alignment.Center,
        ) {
            Text(if (seat.bot) "🤖" else seat.name.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold)
        }
        Text(seat.name, color = Color.White, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        when {
            place >= 0 -> Text("🏆 ${Rules.ordinal(place)}", color = UnoColors.Gold, fontSize = 11.sp)
            cards == 1 -> Text("UNO!", color = UnoColors.Red, fontWeight = FontWeight.Black, fontSize = 13.sp)
            else -> Text("$cards cards", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp)
        }
        val tag = when {
            seat.left -> "AI playing"
            i in v.away -> "offline"
            else -> null
        }
        if (tag != null) Text(tag, color = Color(0xFFFF8A80), fontSize = 10.sp)
    }
}

private fun avatarColor(i: Int) = listOf(UnoColors.Red, UnoColors.Blue, UnoColors.Green, Color(0xFFF57C00), Color(0xFF8E24AA))[i % 5]

@Composable
private fun Pile(s: GameState, me: Int, busy: Boolean, onDraw: () -> Unit) {
    val canDraw = s.actor == me && s.phase == Phase.PLAY && !s.drewPlayable && !busy
    val mustDraw = canDraw && s.playableIndices(me).isEmpty()
    val pulse = rememberInfiniteTransition(label = "draw")
    val scale by pulse.animateFloat(1f, if (mustDraw) 1.08f else 1f, infiniteRepeatable(tween(600), RepeatMode.Reverse), label = "s")
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(28.dp)) {
            Box(contentAlignment = Alignment.Center) {
                CardFace(null, 84.dp * tableScale(), Modifier.offset(3.dp, 3.dp), faceDown = true)
                CardFace(
                    null, 84.dp * tableScale(),
                    Modifier.scale(scale).clickable(enabled = canDraw, onClick = onDraw),
                    faceDown = true,
                )
                Text(
                    "${s.draw.size}", Modifier.align(Alignment.BottomCenter).offset(y = 20.dp),
                    color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp,
                )
            }
            AnimatedContent(
                targetState = s.discard.size to s.top,
                transitionSpec = { (scaleIn(initialScale = 1.4f) + fadeIn()) togetherWith fadeOut(tween(150)) },
                label = "top",
            ) { (_, top) ->
                CardFace(top, 96.dp * tableScale(), Modifier.rotate(((s.discard.size * 37) % 15 - 7).toFloat()))
            }
        }
        Spacer(Modifier.height(26.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            ColorDot(s.color, 18.dp)
            Spacer(Modifier.width(8.dp))
            Text(
                "${s.color.label}  ${if (s.dir == 1) "↻ clockwise" else "↺ counter-clockwise"}",
                color = Color.White, fontSize = 13.sp,
            )
        }
        Spacer(Modifier.height(10.dp))
        AnimatedContent(targetState = s.event, label = "event") { e ->
            Text(
                e, Modifier.widthIn(max = 340.dp).padding(horizontal = 16.dp),
                color = Color.White, textAlign = TextAlign.Center, fontSize = 14.sp,
            )
        }
    }
}

@Composable
private fun RoundPanel(s: GameState, me: Int, onDeal: () -> Unit) {
    val dealer = s.seats[s.dealer]
    var shuffling by rememberSaveable(s.version) { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    Column(
        Modifier
            .padding(16.dp)
            .widthIn(max = 420.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color.Black.copy(alpha = 0.35f))
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (s.phase == Phase.ROUND_OVER) {
            Text("Round ${s.round} results", color = UnoColors.Gold, fontSize = 22.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(8.dp))
            s.finished.forEachIndexed { place, seat ->
                Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                    Text("${Rules.ordinal(place)}  ${s.seats[seat].name}${if (seat == me) " (you)" else ""}", Modifier.weight(1f), color = Color.White)
                    Text("${s.scores[seat]} pts", color = Color.White.copy(alpha = 0.8f))
                }
            }
            Spacer(Modifier.height(12.dp))
        } else {
            Text("Ready to play", color = UnoColors.Gold, fontSize = 22.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(4.dp))
            Text("${s.seats.size} players at the table", color = Color.White.copy(alpha = 0.8f))
            Spacer(Modifier.height(12.dp))
        }
        ShuffleFan(shuffling)
        Spacer(Modifier.height(12.dp))
        if (s.dealer == me) {
            Button(
                onClick = {
                    shuffling = true
                    scope.launch { delay(900); onDeal() }
                },
                enabled = !shuffling,
                colors = ButtonDefaults.buttonColors(containerColor = UnoColors.Red, contentColor = Color.White),
            ) { Text(if (shuffling) "Shuffling…" else "🔀 Shuffle & Deal", fontSize = 18.sp, fontWeight = FontWeight.Bold) }
        } else {
            Text("Waiting for ${dealer.name} to shuffle and deal…", color = Color.White, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun ShuffleFan(active: Boolean) {
    val t = rememberInfiniteTransition(label = "shuffle")
    val a by t.animateFloat(-1f, 1f, infiniteRepeatable(tween(220), RepeatMode.Reverse), label = "a")
    val spread by animateFloatAsState(if (active) 1f else 0.35f, label = "spread")
    Box(Modifier.height(100.dp).width(160.dp), contentAlignment = Alignment.Center) {
        for (k in -2..2) {
            val wobble = if (active) a * 14f * (if (k % 2 == 0) 1 else -1) else 0f
            CardFace(
                null, 56.dp,
                Modifier.offset(x = (k * 22 * spread + wobble).dp).rotate(k * 9f * spread + wobble / 2),
                faceDown = true,
            )
        }
    }
}

@Composable
private fun UnoButton(armed: Boolean, urgent: Boolean, onClick: () -> Unit) {
    val t = rememberInfiniteTransition(label = "uno")
    val s by t.animateFloat(1f, if (urgent || !armed) 1.1f else 1f, infiniteRepeatable(tween(450), RepeatMode.Reverse), label = "s")
    Box(
        Modifier
            .scale(s)
            .size(60.dp)
            .clip(CircleShape)
            .background(if (armed) UnoColors.Gold else UnoColors.Red)
            .border(3.dp, Color.White, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            if (armed) "UNO ✓" else "UNO!", color = if (armed) Color.Black else Color.White,
            fontWeight = FontWeight.Black, fontStyle = FontStyle.Italic, fontSize = 15.sp,
        )
    }
}

@Composable
private fun Hand(s: GameState, me: Int, unoArmed: Boolean, onTap: (Int) -> Unit) {
    val hand = s.hands[me]
    val playable = s.playableIndices(me).toSet()
    val myTurn = s.actor == me && s.phase == Phase.PLAY
    // Show cards grouped by color then value; play actions still use the real index.
    val order = hand.indices.sortedWith(compareBy({ hand[it].color.ordinal }, { hand[it].face.ordinal }))
    val keys = run {
        val seen = HashMap<String, Int>()
        order.map { i -> val c = hand[i].code; val k = seen.merge(c, 1, Int::plus)!!; "$c#$k" }
    }
    BoxWithConstraints(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 12.dp)) {
        val cardW = (maxWidth / 5.2f).coerceIn(58.dp, if (isTablet()) 130.dp else 96.dp)
        val count = hand.size.coerceAtLeast(1)
        val fit = if (count == 1) cardW else (maxWidth - 24.dp - cardW) / (count - 1)
        val step = fit.coerceIn(cardW * 0.34f, cardW + 4.dp)
        LazyRow(
            Modifier.fillMaxWidth().height(cardW * 1.52f + 18.dp),
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(step - cardW, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.Bottom,
        ) {
            itemsIndexed(order, key = { pos, _ -> keys[pos] }) { _, idx ->
                val canPlay = myTurn && idx in playable
                CardFace(
                    hand[idx], cardW,
                    Modifier
                        .animateItem()
                        .offset(y = if (canPlay) (-14).dp else 0.dp)
                        .clickable { onTap(idx) },
                    dim = myTurn && !canPlay,
                )
            }
        }
        if (unoArmed) {
            Text(
                "UNO called — play your card", Modifier.align(Alignment.TopCenter),
                color = UnoColors.Gold, fontSize = 12.sp, fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
fun ColorPicker(title: String, onDismiss: (() -> Unit)?, onPick: (CardColor) -> Unit) {
    Dialog(
        onDismissRequest = { onDismiss?.invoke() },
        properties = DialogProperties(dismissOnBackPress = onDismiss != null, dismissOnClickOutside = onDismiss != null),
    ) {
        Column(
            Modifier.clip(RoundedCornerShape(20.dp)).background(Color(0xFF13233A)).padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(16.dp))
            CardColor.PLAYABLE.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { c ->
                        Box(
                            Modifier
                                .padding(bottom = 12.dp)
                                .size(100.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(UnoColors.of(c))
                                .clickable { onPick(c) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(c.label, color = if (c == CardColor.YELLOW) Color.Black else Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            if (onDismiss != null) TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    }
}

/** Tablets (600dp+ wide) get a larger table so cards don't look lost on big screens. */
@Composable
private fun isTablet() = androidx.compose.ui.platform.LocalConfiguration.current.smallestScreenWidthDp >= 600

@Composable
private fun tableScale() = if (isTablet()) 1.45f else 1f
