package com.satya.uno.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.satya.uno.data.Prefs

/** The official rules every game in this app follows — offline and online alike. */
@Composable
fun RulesScreen(prefs: Prefs, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    var lang by remember { mutableStateOf(RulesLang.of(prefs.rulesLang)) }
    var menuOpen by remember { mutableStateOf(false) }
    // Only the chosen language is read from assets; the previous one is dropped.
    val text = remember(lang) { lang.load(context) }
    Column(Modifier.fillMaxSize().background(TableBrush).systemBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("← Back", color = Color.White) }
            Text(
                text.title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold,
                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
            )
            Box {
                TextButton(onClick = { menuOpen = true }) { Text("🌐 ${lang.label} ▾", color = UnoColors.Gold) }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    RulesLang.ALL.forEach { l ->
                        DropdownMenuItem(
                            text = { Text(l.label, fontWeight = if (l == lang) FontWeight.Bold else FontWeight.Normal) },
                            onClick = {
                                lang = l
                                prefs.rulesLang = l.code
                                menuOpen = false
                            },
                        )
                    }
                }
            }
        }
        CompositionLocalProvider(LocalLayoutDirection provides if (lang.rtl) LayoutDirection.Rtl else LayoutDirection.Ltr) {
            Column(
                Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                text.sections.forEach { (title, body) ->
                    Column(Modifier.widthIn(max = 600.dp).fillMaxWidth().padding(bottom = 16.dp)) {
                        Text(title, color = UnoColors.Gold, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(Modifier.height(4.dp))
                        Text(body, color = Color.White.copy(alpha = 0.9f), fontSize = 14.sp, lineHeight = 20.sp)
                    }
                }
            }
        }
    }
}
