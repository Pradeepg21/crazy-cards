package com.satya.uno.game

enum class Phase { SHUFFLE, PLAY, CHOOSE_COLOR, CHALLENGE, ROUND_OVER }

/** level: 0 easy, 1 normal, 2 hard. A human who leaves mid-game becomes [left] and is played by the computer. */
data class Seat(
    val id: String,
    val name: String,
    val bot: Boolean = false,
    val level: Int = 1,
    val left: Boolean = false,
) {
    val automated get() = bot || left
}

data class GameState(
    val seats: List<Seat>,
    val hands: List<List<Card>> = seats.map { emptyList() },
    val draw: List<Card> = emptyList(),
    val discard: List<Card> = emptyList(),
    val color: CardColor = CardColor.WILD,
    val turn: Int = 0,
    val dir: Int = 1,
    val phase: Phase = Phase.SHUFFLE,
    val dealer: Int = 0,
    /** Current player drew a playable card; they may play it (last card of hand) or pass. */
    val drewPlayable: Boolean = false,
    /** Current player pressed UNO before playing their second-to-last card. */
    val unoSaid: Boolean = false,
    /** Seat that is down to one card without calling UNO and can be caught, or -1. */
    val vulnerable: Int = -1,
    val wd4By: Int = -1,
    val wd4Illegal: Boolean = false,
    /** Seats in the order they emptied their hand this round. */
    val finished: List<Int> = emptyList(),
    val scores: List<Int> = seats.map { 0 },
    val round: Int = 0,
    val version: Long = 0,
    val event: String = "",
    val turnAt: Long = 0,
    val lastHumanAt: Long = 0,
) {
    val top: Card? get() = discard.lastOrNull()
    val activeCount get() = seats.indices.count { it !in finished }

    /** Seat expected to act next (dealer while waiting for a shuffle). */
    val actor: Int get() = if (phase == Phase.SHUFFLE || phase == Phase.ROUND_OVER) dealer else turn

    fun isPlayable(card: Card): Boolean {
        val t = top ?: return true
        if (card.isWild) return true
        if (card.color == color) return true
        return !t.isWild && card.face == t.face
    }

    fun playableIndices(seat: Int): List<Int> {
        if (phase != Phase.PLAY || seat != turn) return emptyList()
        val hand = hands[seat]
        if (drewPlayable) return listOf(hand.lastIndex)
        return hand.indices.filter { isPlayable(hand[it]) }
    }

    fun next(from: Int, steps: Int = 1, direction: Int = dir): Int {
        val n = seats.size
        var i = from
        repeat(steps) {
            var guard = 0
            do { i = ((i + direction) % n + n) % n; guard++ } while (i in finished && guard <= n)
        }
        return i
    }
}

sealed interface Action {
    val seat: Int

    data class Deal(override val seat: Int) : Action
    data class Play(override val seat: Int, val index: Int, val code: String, val color: CardColor? = null, val uno: Boolean = false) : Action
    data class Draw(override val seat: Int) : Action
    data class Pass(override val seat: Int) : Action
    data class ChooseColor(override val seat: Int, val color: CardColor) : Action
    data class Challenge(override val seat: Int) : Action
    data class Accept(override val seat: Int) : Action
    data class CallUno(override val seat: Int) : Action
    data class Catch(override val seat: Int) : Action
}

class IllegalMove(msg: String) : Exception(msg)
