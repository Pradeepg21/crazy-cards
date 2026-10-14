package com.satya.uno

import com.satya.uno.game.Action
import com.satya.uno.game.Bot
import com.satya.uno.game.Card
import com.satya.uno.game.CardColor
import com.satya.uno.game.Codec
import com.satya.uno.game.Deck
import com.satya.uno.game.Director
import com.satya.uno.game.Face
import com.satya.uno.game.GameState
import com.satya.uno.game.IllegalMove
import com.satya.uno.game.Phase
import com.satya.uno.game.Rules
import com.satya.uno.game.Seat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class RulesTest {
    private fun table(n: Int, level: Int = 1) =
        GameState(seats = List(n) { Seat("p$it", "P$it", bot = true, level = level % 3) }, phase = Phase.SHUFFLE)

    private fun total(s: GameState) = s.draw.size + s.discard.size + s.hands.sumOf { it.size }

    @Test
    fun deckIsOfficial() {
        val d = Deck.fresh()
        assertEquals(108, d.size)
        assertEquals(4, d.count { it.face == Face.WILD4 })
        assertEquals(4, d.count { it.face == Face.ZERO })
        assertEquals(8, d.count { it.face == Face.SKIP })
        assertEquals(25, d.count { it.color == CardColor.RED })
    }

    @Test
    fun dealGivesSevenAndNeverStartsWithWild4() {
        repeat(500) { i ->
            val s = Rules.apply(table(2 + i % 9), Action.Deal(0), 0, Random(i))
            assertEquals(108, total(s))
            assertTrue(s.top!!.face != Face.WILD4)
            val drew2 = s.top!!.face == Face.DRAW2
            s.hands.forEachIndexed { seat, h -> assertEquals(if (drew2 && seat == 1) 9 else 7, h.size) }
        }
    }

    /** Plays thousands of complete games between bots and checks invariants after every move. */
    @Test
    fun simulatedGamesFinishAndConserveCards() {
        repeat(1500) { g ->
            val rnd = Random(g)
            var s = table(2 + g % 9, g)
            var moves = 0
            while (s.phase != Phase.ROUND_OVER) {
                val plan = Director.plan(s, 0, isAway = { false }, timed = false)!!
                s = Rules.apply(s, plan.action, 0, rnd).copy(version = s.version + 1)
                assertEquals("cards lost in game $g", 108, total(s))
                assertTrue(s.turn in s.seats.indices)
                if (s.phase == Phase.PLAY) assertFalse("finished player has the turn", s.turn in s.finished)
                assertTrue("game $g never ended", ++moves < 5000)
            }
            assertEquals(s.seats.size, s.finished.size)
            assertEquals(s.seats.indices.toSet(), s.finished.toSet())
            // Next round can be dealt by the rotated dealer.
            val next = Rules.apply(s, Action.Deal(s.dealer), 0, rnd)
            assertEquals(Phase.PLAY.takeIf { next.top!!.face != Face.WILD } ?: Phase.CHOOSE_COLOR, next.phase)
            assertEquals(s.round + 1, next.round)
        }
    }

    @Test
    fun codecRoundTrips() {
        var s = Rules.apply(table(4), Action.Deal(0), 123, Random(1))
        repeat(20) { s = Rules.apply(s, Bot.move(s, s.actor, 1, Random(it))!!, 0, Random(it)) }
        assertEquals(s, Codec.fromJson(Codec.toJson(s)))
        assertEquals(s, Codec.fromMap(Codec.toMap(s)))
    }

    private fun c(code: String) = Card.parse(code)

    private fun fixed(hands: List<List<String>>, top: String, color: CardColor? = null) = GameState(
        seats = List(hands.size) { Seat("p$it", "P$it") },
        hands = hands.map { h -> h.map(::c) },
        draw = Deck.fresh().take(40),
        discard = listOf(c(top)),
        color = color ?: c(top).color,
        phase = Phase.PLAY,
    )

    @Test
    fun matchingRules() {
        val s = fixed(listOf(listOf("R5", "B7", "G5", "Y1", "WW"), listOf("B1")), "R7")
        assertEquals(listOf(0, 1, 4), s.playableIndices(0))
        assertThrows(IllegalMove::class.java) { Rules.apply(s, Action.Play(0, 3, "Y1"), 0) }
        assertThrows(IllegalMove::class.java) { Rules.apply(s, Action.Play(1, 0, "B1"), 0) }
        assertThrows(IllegalMove::class.java) { Rules.apply(s, Action.Play(0, 4, "WW"), 0) } // no color chosen
    }

    @Test
    fun forgottenUnoCanBeCaughtUntilNextPlayerActs() {
        val s = fixed(listOf(listOf("R5", "R6"), listOf("B1", "B2"), listOf("G1")), "R7")
        val after = Rules.apply(s, Action.Play(0, 0, "R5"), 0)
        assertEquals(0, after.vulnerable)
        val caught = Rules.apply(after, Action.Catch(2), 0, Random(1))
        assertEquals(3, caught.hands[0].size)
        // Calling UNO before playing keeps you safe.
        val safe = Rules.apply(s, Action.Play(0, 0, "R5", uno = true), 0)
        assertEquals(-1, safe.vulnerable)
        // Next player acting closes the window.
        val moved = Rules.apply(after, Action.Draw(1), 0, Random(1))
        assertThrows(IllegalMove::class.java) { Rules.apply(moved, Action.Catch(2), 0) }
    }

    @Test
    fun wildDrawFourChallenge() {
        // P0 holds a red card, so the Wild Draw Four is illegal.
        val s = fixed(listOf(listOf("WF", "R2", "B3"), listOf("G1", "G2"), listOf("Y1")), "R7")
        val played = Rules.apply(s, Action.Play(0, 0, "WF", CardColor.BLUE), 0)
        assertEquals(Phase.CHALLENGE, played.phase)
        assertEquals(1, played.turn)
        val won = Rules.apply(played, Action.Challenge(1), 0, Random(2))
        assertEquals(6, won.hands[0].size)
        assertEquals(1, won.turn)
        // Legal play: challenger draws 6 and is skipped.
        val legal = fixed(listOf(listOf("WF", "B2", "B3"), listOf("G1", "G2"), listOf("Y1")), "R7")
        val p2 = Rules.apply(legal, Action.Play(0, 0, "WF", CardColor.BLUE), 0)
        val lost = Rules.apply(p2, Action.Challenge(1), 0, Random(2))
        assertEquals(8, lost.hands[1].size)
        assertEquals(2, lost.turn)
    }

    @Test
    fun reverseActsAsSkipWithTwoPlayers() {
        val s = fixed(listOf(listOf("RR", "R1"), listOf("B1", "B2")), "R7")
        val r = Rules.apply(s, Action.Play(0, 0, "RR"), 0)
        assertEquals(0, r.turn)
        val three = fixed(listOf(listOf("RR", "R1"), listOf("B1"), listOf("G1")), "R7")
        val r3 = Rules.apply(three, Action.Play(0, 0, "RR"), 0)
        assertEquals(2, r3.turn)
        assertEquals(-1, r3.dir)
    }

    @Test
    fun drawnPlayableCardMayBePlayedOrKept() {
        val s = fixed(listOf(listOf("B1"), listOf("G1", "G2")), "R7").copy(draw = listOf(c("R3")))
        val d = Rules.apply(s, Action.Draw(0), 0)
        assertTrue(d.drewPlayable)
        assertEquals(0, d.turn)
        assertThrows(IllegalMove::class.java) { Rules.apply(d, Action.Play(0, 0, "B1"), 0) }
        val kept = Rules.apply(d, Action.Pass(0), 0)
        assertEquals(1, kept.turn)
    }

    @Test
    fun goingOutScoresAndPlayContinues() {
        val s = fixed(listOf(listOf("R1"), listOf("G9", "WW"), listOf("Y5", "BS")), "R7")
        val out = Rules.apply(s, Action.Play(0, 0, "R1"), 0)
        assertEquals(listOf(0), out.finished)
        assertEquals(9 + 50 + 5 + 20, out.scores[0])
        assertEquals(Phase.PLAY, out.phase)
        assertEquals(1, out.turn)
    }
}
