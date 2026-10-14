package com.satya.uno.game

import kotlin.random.Random

/**
 * Computer player. Levels: 0 Easy (casual, forgets UNO), 1 Normal, 2 Hard (tracks opponents,
 * saves wilds, attacks whoever is close to going out, bluffs Wild Draw Four now and then).
 */
object Bot {
    val LEVELS = listOf("Easy", "Normal", "Hard")

    fun move(s: GameState, seat: Int, level: Int, rnd: Random = Deck.rng): Action? {
        return when (s.phase) {
            Phase.SHUFFLE, Phase.ROUND_OVER -> if (seat == s.dealer) Action.Deal(seat) else null
            Phase.CHOOSE_COLOR -> if (seat == s.turn) Action.ChooseColor(seat, bestColor(s.hands[seat], rnd)) else null
            Phase.CHALLENGE -> if (seat == s.turn) challengeOrAccept(s, seat, level, rnd) else null
            Phase.PLAY -> if (seat == s.turn) playMove(s, seat, level, rnd) else null
        }
    }

    /** Whether this bot notices an opponent who forgot to call UNO. */
    fun catchChance(level: Int) = when (level) { 0 -> 0.25; 1 -> 0.55; else -> 0.9 }

    /** Chance this bot forgets to call UNO. */
    private fun forgetChance(level: Int) = when (level) { 0 -> 0.3; 1 -> 0.1; else -> 0.0 }

    private fun challengeOrAccept(s: GameState, seat: Int, level: Int, rnd: Random): Action {
        val offenderCards = s.hands.getOrNull(s.wd4By)?.size ?: 0
        // The more cards the offender held, the likelier they had the color in play.
        val p = when (level) {
            0 -> 0.1
            1 -> 0.25
            else -> (0.08 * offenderCards).coerceIn(0.1, 0.7)
        }
        return if (rnd.nextDouble() < p) Action.Challenge(seat) else Action.Accept(seat)
    }

    private fun playMove(s: GameState, seat: Int, level: Int, rnd: Random): Action {
        val hand = s.hands[seat]
        val options = s.playableIndices(seat)
        if (options.isEmpty()) return if (s.drewPlayable) Action.Pass(seat) else Action.Draw(seat)

        val idx = when (level) {
            0 -> options.random(rnd)
            else -> options.maxBy { score(s, seat, hand[it], level, rnd) }
        }
        val card = hand[idx]
        // Hard bots sometimes hold a drawn wild for later instead of using it right away.
        if (s.drewPlayable && card.isWild && level == 2 && hand.size > 3 && rnd.nextDouble() < 0.5) return Action.Pass(seat)

        val remaining = hand.toMutableList().apply { removeAt(idx) }
        val color = if (card.isWild) bestColor(remaining, rnd) else null
        val uno = remaining.size == 1 && rnd.nextDouble() >= forgetChance(level)
        return Action.Play(seat, idx, card.code, color, uno)
    }

    private fun score(s: GameState, seat: Int, card: Card, level: Int, rnd: Random): Double {
        val hand = s.hands[seat]
        val next = s.next(seat)
        val nextCards = s.hands[next].size
        val threat = s.seats.indices.filter { it != seat && it !in s.finished }.minOfOrNull { s.hands[it].size } ?: 7
        val colorCount = hand.count { it.color == card.color }
        var v = 0.0

        // Get rid of high-value cards; keep color flow on the color we hold most of.
        v += card.face.points * 0.05
        v += colorCount * 1.2

        when (card.face) {
            Face.DRAW2, Face.SKIP -> v += if (nextCards <= 2) 9.0 else 1.5
            Face.REVERSE -> v += if (nextCards <= 2) 7.0 else 1.0
            Face.WILD -> v -= if (hand.size <= 2 || threat <= 1) -4.0 else 6.0
            Face.WILD4 -> {
                val bluff = hand.any { it.color == s.color && !it.isWild }
                v -= if (nextCards <= 2) -6.0 else 7.0
                if (bluff) v -= if (level == 2 && nextCards <= 2 && rnd.nextDouble() < 0.3) 0.0 else 20.0
            }
            else -> {}
        }
        // Prefer changing to a color we're strong in rather than staying on a weak one.
        if (!card.isWild && card.color != s.color && colorCount >= 3) v += 1.5
        // A little unpredictability so bots don't feel mechanical.
        v += rnd.nextDouble() * if (level == 2) 0.4 else 1.5
        return v
    }

    fun bestColor(hand: List<Card>, rnd: Random = Deck.rng): CardColor {
        val counts = CardColor.PLAYABLE.associateWith { c -> hand.filter { it.color == c }.sumOf { 1 + it.face.points / 20 } }
        val max = counts.values.maxOrNull() ?: 0
        return counts.filterValues { it == max }.keys.toList().random(rnd)
    }
}
