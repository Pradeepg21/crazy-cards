package com.satya.uno.game

import kotlin.random.Random

/** An automatic move to make after [delayMs] if the game is still at the same version. */
data class Plan(val key: String, val delayMs: Long, val action: Action)

/**
 * Decides what the computer should do next: bot turns, bots catching a missed UNO,
 * playing for disconnected players, and turn timeouts. Shared by offline and online games.
 */
object Director {
    const val TURN_MS = 30_000L
    const val DEAL_MS = 45_000L

    fun plan(s: GameState, now: Long, isAway: (Int) -> Boolean, timed: Boolean): Plan? {
        // Seeded by version so re-planning the same state gives the same decision.
        val rnd = Random(s.version * 7919 + s.turn)
        val v = s.version

        if (s.vulnerable >= 0 && s.phase != Phase.ROUND_OVER) {
            val catcher = s.seats.indices
                .filter { it != s.vulnerable && (s.seats[it].automated) }
                .maxByOrNull { s.seats[it].level }
            if (catcher != null && rnd.nextDouble() < Bot.catchChance(s.seats[catcher].level)) {
                return Plan("catch:$v", 700L + rnd.nextLong(900), Action.Catch(catcher))
            }
        }

        val seat = s.actor
        if (seat !in s.seats.indices) return null
        val st = s.seats[seat]
        val dealing = s.phase == Phase.SHUFFLE || s.phase == Phase.ROUND_OVER

        if (st.automated) {
            val delay = if (dealing) (if (s.phase == Phase.ROUND_OVER) 4500L else 1500L) else 900L + rnd.nextLong(900)
            val a = Bot.move(s, seat, st.level, rnd) ?: return null
            return Plan("bot:$v", delay, a)
        }
        if (isAway(seat)) {
            val a = Bot.move(s, seat, 1, rnd) ?: return null
            return Plan("away:$v", 2500L, a)
        }
        if (!timed) return null
        val limit = if (dealing) DEAL_MS else TURN_MS
        val delay = (s.turnAt + limit - now).coerceAtLeast(0)
        val a = when (s.phase) {
            Phase.SHUFFLE, Phase.ROUND_OVER -> Action.Deal(seat)
            Phase.CHOOSE_COLOR -> Action.ChooseColor(seat, Bot.bestColor(s.hands[seat], rnd))
            Phase.CHALLENGE -> Action.Accept(seat)
            Phase.PLAY -> if (s.drewPlayable) Action.Pass(seat) else Action.Draw(seat)
        }
        return Plan("timeout:$v", delay, a)
    }
}
