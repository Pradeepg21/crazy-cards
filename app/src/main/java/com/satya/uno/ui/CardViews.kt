package com.satya.uno.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.satya.uno.game.Card
import com.satya.uno.game.CardColor
import com.satya.uno.game.Face

object UnoColors {
    val Red = Color(0xFFE53935)
    val Yellow = Color(0xFFFDD835)
    val Green = Color(0xFF43A047)
    val Blue = Color(0xFF1E88E5)
    val Black = Color(0xFF16161A)
    val TableTop = Color(0xFF0F2A4A)
    val TableBottom = Color(0xFF071526)
    val Gold = Color(0xFFFFC107)

    fun of(c: CardColor) = when (c) {
        CardColor.RED -> Red
        CardColor.YELLOW -> Yellow
        CardColor.GREEN -> Green
        CardColor.BLUE -> Blue
        CardColor.WILD -> Black
    }
}

val TableBrush = Brush.verticalGradient(listOf(UnoColors.TableTop, UnoColors.TableBottom))

@Composable
fun UnoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = UnoColors.Gold,
            onPrimary = Color.Black,
            secondary = UnoColors.Red,
            background = UnoColors.TableBottom,
            surface = Color(0xFF13233A),
            surfaceVariant = Color(0xFF1C3150),
            surfaceContainerHigh = Color(0xFF1A2D4A),
        ),
        content = content,
    )
}

private fun symbol(f: Face) = when (f) {
    Face.SKIP -> "⊘"
    Face.REVERSE -> "⇄"
    Face.DRAW2 -> "+2"
    Face.WILD -> "W"
    Face.WILD4 -> "+4"
    else -> f.label
}

/** A card drawn entirely with shapes and text — no bitmaps, so it's crisp at any size and costs no memory. */
@Composable
fun CardFace(card: Card?, width: Dp, modifier: Modifier = Modifier, faceDown: Boolean = false, dim: Boolean = false) {
    val shape = RoundedCornerShape(width * 0.12f)
    val bg = if (faceDown || card == null) UnoColors.Black else UnoColors.of(card.color)
    Box(
        modifier
            .width(width)
            .aspectRatio(0.66f)
            .shadow(4.dp, shape)
            .clip(shape)
            // Darken unplayable cards with an opaque scrim so overlapping cards never show through.
            .drawWithContent {
                drawContent()
                if (dim) drawRect(Color.Black.copy(alpha = 0.5f))
            }
            .background(Color.White)
            .padding(width * 0.05f)
            .clip(RoundedCornerShape(width * 0.09f))
            .background(bg),
        contentAlignment = Alignment.Center,
    ) {
        if (faceDown || card == null) {
            CardBack(width)
            return@Box
        }
        val wild = card.isWild
        Canvas(Modifier.fillMaxSize()) {
            rotate(28f) {
                val w = size.width * 0.66f
                val h = size.height * 0.92f
                val tl = Offset((size.width - w) / 2, (size.height - h) / 2)
                if (wild) {
                    // Four-color oval for wild cards.
                    val colors = listOf(UnoColors.Red, UnoColors.Blue, UnoColors.Yellow, UnoColors.Green)
                    colors.forEachIndexed { i, c ->
                        drawArc(c, startAngle = 180f + 90f * i, sweepAngle = 90f, useCenter = true, topLeft = tl, size = Size(w, h))
                    }
                } else {
                    drawOval(Color.White, topLeft = tl, size = Size(w, h))
                }
            }
        }
        val fg = if (wild) Color.White else bg
        Text(
            symbol(card.face),
            style = TextStyle(
                color = fg, fontSize = (width.value * 0.42f).sp, fontWeight = FontWeight.Black,
                fontStyle = FontStyle.Italic, textAlign = TextAlign.Center,
                shadow = androidx.compose.ui.graphics.Shadow(Color.Black.copy(alpha = 0.6f), Offset(2f, 2f), 2f),
            ),
        )
        val corner = TextStyle(color = Color.White, fontSize = (width.value * 0.16f).sp, fontWeight = FontWeight.Black, fontStyle = FontStyle.Italic)
        Text(symbol(card.face), style = corner, modifier = Modifier.align(Alignment.TopStart).padding(start = width * 0.04f))
        Text(symbol(card.face), style = corner, modifier = Modifier.align(Alignment.BottomEnd).padding(end = width * 0.04f).rotate(180f))
    }
}

@Composable
private fun CardBack(width: Dp) {
    Canvas(Modifier.fillMaxSize()) {
        rotate(28f) {
            val w = size.width * 0.66f
            val h = size.height * 0.92f
            drawOval(UnoColors.Red, topLeft = Offset((size.width - w) / 2, (size.height - h) / 2), size = Size(w, h))
        }
    }
    Text(
        "UNO",
        modifier = Modifier.rotate(-28f),
        style = TextStyle(
            color = UnoColors.Yellow, fontSize = (width.value * 0.27f).sp, fontWeight = FontWeight.Black,
            fontStyle = FontStyle.Italic,
            shadow = androidx.compose.ui.graphics.Shadow(Color.Black, Offset(2f, 2f), 1f),
        ),
    )
}

/** Big tilted logo card used on the home screen. */
@Composable
fun Logo(modifier: Modifier = Modifier, width: Dp = 150.dp) {
    Box(modifier, contentAlignment = Alignment.Center) {
        CardFace(Card(CardColor.BLUE, Face.SEVEN), width * 0.8f, Modifier.rotate(-18f).padding(end = width * 0.5f), faceDown = false)
        CardFace(Card(CardColor.WILD, Face.WILD4), width * 0.8f, Modifier.rotate(16f).padding(start = width * 0.5f))
        CardFace(null, width, Modifier.border(0.dp, Color.Transparent), faceDown = true)
    }
}

@Composable
fun ColorDot(color: CardColor, size: Dp, modifier: Modifier = Modifier) {
    Box(
        modifier
            .width(size)
            .aspectRatio(1f)
            .clip(RoundedCornerShape(50))
            .background(UnoColors.of(color))
            .border(2.dp, Color.White, RoundedCornerShape(50)),
    )
}
