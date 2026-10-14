package com.satya.uno.game

import java.security.SecureRandom
import kotlin.random.Random
import kotlin.random.asKotlinRandom

enum class CardColor(val code: Char, val label: String) {
    RED('R', "Red"), YELLOW('Y', "Yellow"), GREEN('G', "Green"), BLUE('B', "Blue"), WILD('W', "Wild");

    companion object {
        val PLAYABLE = listOf(RED, YELLOW, GREEN, BLUE)
        fun of(c: Char) = entries.first { it.code == c }
    }
}

enum class Face(val code: Char, val points: Int, val label: String) {
    ZERO('0', 0, "0"), ONE('1', 1, "1"), TWO('2', 2, "2"), THREE('3', 3, "3"), FOUR('4', 4, "4"),
    FIVE('5', 5, "5"), SIX('6', 6, "6"), SEVEN('7', 7, "7"), EIGHT('8', 8, "8"), NINE('9', 9, "9"),
    SKIP('S', 20, "Skip"), REVERSE('R', 20, "Reverse"), DRAW2('D', 20, "Draw Two"),
    WILD('W', 50, "Wild"), WILD4('F', 50, "Wild Draw Four");

    val isNumber get() = ordinal <= NINE.ordinal
    val isAction get() = this == SKIP || this == REVERSE || this == DRAW2

    companion object {
        fun of(c: Char) = entries.first { it.code == c }
    }
}

/** A card is encoded as two characters (color + face), e.g. "R7", "GS", "WF", keeping synced state tiny. */
data class Card(val color: CardColor, val face: Face) {
    val code: String get() = "${color.code}${face.code}"
    val isWild get() = color == CardColor.WILD
    val label: String get() = if (isWild) face.label else "${color.label} ${face.label}"

    companion object {
        fun parse(s: String) = Card(CardColor.of(s[0]), Face.of(s[1]))
    }
}

fun List<Card>.encode(): String = joinToString("") { it.code }
fun decodeCards(s: String?): List<Card> = s.orEmpty().chunked(2).filter { it.length == 2 }.map(Card::parse)
fun List<Card>.points(): Int = sumOf { it.face.points }

object Deck {
    /** The official 108-card deck. */
    fun fresh(): List<Card> = buildList {
        for (c in CardColor.PLAYABLE) {
            add(Card(c, Face.ZERO))
            for (f in Face.entries) if (f != Face.ZERO && (f.isNumber || f.isAction)) repeat(2) { add(Card(c, f)) }
        }
        repeat(4) { add(Card(CardColor.WILD, Face.WILD)); add(Card(CardColor.WILD, Face.WILD4)) }
    }

    /** Cryptographically seeded RNG so shuffles cannot be predicted. */
    val rng: Random = SecureRandom().asKotlinRandom()

    /** Fisher–Yates shuffle, run several passes like a real riffle-and-cut. */
    fun shuffle(cards: List<Card>, rnd: Random = rng): List<Card> {
        val a = cards.toMutableList()
        repeat(3) {
            for (i in a.lastIndex downTo 1) {
                val j = rnd.nextInt(i + 1)
                val t = a[i]; a[i] = a[j]; a[j] = t
            }
        }
        return a
    }
}
