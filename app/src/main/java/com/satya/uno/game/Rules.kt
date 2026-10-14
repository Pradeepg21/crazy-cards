package com.satya.uno.game

import kotlin.random.Random

/** Pure official-rules engine. Every client (offline or online) applies moves through here. */
object Rules {
    const val HAND_SIZE = 7
    const val MAX_PLAYERS = 10

    fun apply(s: GameState, a: Action, now: Long, rnd: Random = Deck.rng): GameState {
        if (a.seat !in s.seats.indices) throw IllegalMove("Unknown player")
        val name = s.seats[a.seat].name
        return when (a) {
            is Action.Deal -> {
                if (s.phase != Phase.SHUFFLE && s.phase != Phase.ROUND_OVER) throw IllegalMove("Round in progress")
                if (a.seat != s.dealer) throw IllegalMove("Only the dealer can shuffle")
                deal(s, now, rnd)
            }
            is Action.Catch -> {
                val v = s.vulnerable
                if (v < 0 || v == a.seat || s.phase == Phase.ROUND_OVER) throw IllegalMove("Too late to catch")
                give(s, v, 2, rnd).copy(vulnerable = -1, event = "$name caught ${s.seats[v].name} without Last Card! +2")
            }
            is Action.CallUno -> when {
                s.vulnerable == a.seat -> s.copy(vulnerable = -1, event = "$name: Last Card!")
                a.seat == s.turn && s.phase == Phase.PLAY && s.hands[a.seat].size == 2 ->
                    s.copy(unoSaid = true, event = "$name: Last Card!")
                else -> throw IllegalMove("Call Last Card when you have two cards on your turn")
            }
            else -> {
                if (a.seat != s.turn) throw IllegalMove("Not your turn")
                // The next player beginning their turn closes the catch window.
                turnAction(s.copy(vulnerable = -1), a, name, now, rnd)
            }
        }
    }

    private fun turnAction(s: GameState, a: Action, name: String, now: Long, rnd: Random): GameState = when (a) {
        is Action.ChooseColor -> {
            if (s.phase != Phase.CHOOSE_COLOR || a.color == CardColor.WILD) throw IllegalMove("Pick a color")
            s.copy(color = a.color, phase = Phase.PLAY, event = "$name chose ${a.color.label}")
        }
        is Action.Accept -> {
            if (s.phase != Phase.CHALLENGE) throw IllegalMove("Nothing to accept")
            give(s, a.seat, 4, rnd).copy(
                phase = Phase.PLAY, turn = s.next(a.seat), wd4By = -1, turnAt = now, event = "$name drew 4",
            )
        }
        is Action.Challenge -> {
            if (s.phase != Phase.CHALLENGE) throw IllegalMove("Nothing to challenge")
            val by = s.wd4By
            if (s.wd4Illegal) {
                give(s, by, 4, rnd).copy(
                    phase = Phase.PLAY, wd4By = -1, turnAt = now,
                    event = "Challenge won! ${s.seats[by].name} bluffed and draws 4",
                )
            } else {
                give(s, a.seat, 6, rnd).copy(
                    phase = Phase.PLAY, turn = s.next(a.seat), wd4By = -1, turnAt = now,
                    event = "Challenge lost! $name draws 6",
                )
            }
        }
        is Action.Draw -> {
            if (s.phase != Phase.PLAY || s.drewPlayable) throw IllegalMove("You can't draw now")
            val before = s.hands[a.seat].size
            val d = give(s, a.seat, 1, rnd)
            val hand = d.hands[a.seat]
            when {
                hand.size == before ->
                    d.copy(turn = s.next(a.seat), unoSaid = false, turnAt = now, event = "$name passed (no cards left to draw)")
                d.isPlayable(hand.last()) -> d.copy(drewPlayable = true, event = "$name drew a card")
                else -> d.copy(turn = s.next(a.seat), unoSaid = false, turnAt = now, event = "$name drew a card")
            }
        }
        is Action.Pass -> {
            if (!s.drewPlayable) throw IllegalMove("Draw a card first")
            s.copy(drewPlayable = false, unoSaid = false, turn = s.next(a.seat), turnAt = now, event = "$name kept the card")
        }
        is Action.Play -> play(s, a, name, now, rnd)
        else -> s
    }

    private fun play(s: GameState, a: Action.Play, name: String, now: Long, rnd: Random): GameState {
        if (s.phase != Phase.PLAY) throw IllegalMove("You can't play now")
        val hand = s.hands[a.seat]
        val playIdx = if (s.drewPlayable) {
            if (hand.lastOrNull()?.code != a.code) throw IllegalMove("You may only play the card you drew")
            hand.lastIndex
        } else {
            if (hand.getOrNull(a.index)?.code == a.code) a.index else hand.indexOfFirst { it.code == a.code }
        }
        if (playIdx < 0) throw IllegalMove("Card not in hand")
        val card = hand[playIdx]
        if (!s.isPlayable(card)) throw IllegalMove("That card doesn't match")
        val chosen = a.color
        if (card.isWild && (chosen == null || chosen == CardColor.WILD)) throw IllegalMove("Pick a color")

        val newHand = hand.toMutableList().apply { removeAt(playIdx) }
        val calledUno = s.unoSaid || a.uno
        val outNow = newHand.isEmpty()
        var event = "$name played ${card.label}"
        if (card.isWild) event += " → ${chosen!!.label}"
        if (newHand.size == 1 && calledUno) event += " — Last Card!"
        var st = s.copy(
            hands = s.hands.replace(a.seat, newHand),
            discard = s.discard + card,
            color = if (card.isWild) chosen!! else card.color,
            drewPlayable = false,
            unoSaid = false,
            vulnerable = if (newHand.size == 1 && !calledUno) a.seat else -1,
            finished = if (outNow) s.finished + a.seat else s.finished,
            turnAt = now,
            event = event,
        )
        st = when (card.face) {
            Face.SKIP -> st.copy(turn = st.next(a.seat, 2))
            Face.REVERSE -> {
                val r = st.copy(dir = -st.dir)
                // With two players left, Reverse works like Skip.
                if (r.activeCount <= 2) r.copy(turn = r.next(a.seat, 2)) else r.copy(turn = r.next(a.seat, 1))
            }
            Face.DRAW2 -> {
                val victim = st.next(a.seat)
                give(st, victim, 2, rnd).copy(
                    turn = st.next(a.seat, 2), event = st.event + " · ${s.seats[victim].name} draws 2",
                )
            }
            Face.WILD4 -> {
                val victim = st.next(a.seat)
                if (outNow) {
                    give(st, victim, 4, rnd).copy(
                        turn = st.next(a.seat, 2), event = st.event + " · ${s.seats[victim].name} draws 4",
                    )
                } else {
                    // Legal only if the player held no card of the color in play.
                    val illegal = hand.any { it.color == s.color && !it.isWild }
                    st.copy(phase = Phase.CHALLENGE, turn = victim, wd4By = a.seat, wd4Illegal = illegal)
                }
            }
            else -> st.copy(turn = st.next(a.seat))
        }
        if (outNow) st = finish(st, a.seat)
        return st
    }

    /**
     * Official scoring, extended so play continues until everyone is out: each player who goes out
     * scores the points still held by everyone who hasn't finished.
     */
    private fun finish(s: GameState, seat: Int): GameState {
        val place = s.finished.indexOf(seat)
        val pts = s.seats.indices.filter { it !in s.finished }.sumOf { s.hands[it].points() }
        var st = s.copy(
            scores = s.scores.replace(seat, s.scores[seat] + pts),
            event = s.event + " · ${s.seats[seat].name} finished ${ordinal(place)} (+$pts)",
        )
        if (st.activeCount <= 1) {
            val last = st.seats.indices.filter { it !in st.finished }
            st = st.copy(
                finished = st.finished + last,
                phase = Phase.ROUND_OVER,
                dealer = (st.dealer + 1) % st.seats.size,
                vulnerable = -1,
                drewPlayable = false,
                wd4By = -1,
                event = "Round ${st.round} over! ${st.seats[st.finished.first()].name} won",
            )
        }
        return st
    }

    fun deal(s0: GameState, now: Long, rnd: Random): GameState {
        // Players who quit for good leave the table between rounds.
        val keep = s0.seats.indices.filter { !s0.seats[it].left }
        val dealerSeat = s0.seats[s0.dealer]
        val seats = keep.map { s0.seats[it] }
        if (seats.size < 2) throw IllegalMove("Need at least 2 players")
        val scores = keep.map { s0.scores.getOrElse(it) { 0 } }
        val dealer = seats.indexOf(dealerSeat).takeIf { it >= 0 } ?: 0
        val n = seats.size

        val deck = Deck.shuffle(Deck.fresh(), rnd).toMutableList()
        val hands = List(n) { mutableListOf<Card>() }
        repeat(HAND_SIZE) { for (k in 1..n) hands[(dealer + k) % n].add(deck.removeAt(deck.lastIndex)) }
        var top = deck.removeAt(deck.lastIndex)
        // A Wild Draw Four can't start the game: return it to the deck and flip again.
        while (top.face == Face.WILD4) {
            deck.add(rnd.nextInt(deck.size + 1), top)
            top = deck.removeAt(deck.lastIndex)
        }
        val first = (dealer + 1) % n
        val st = GameState(
            seats = seats, hands = hands, draw = deck, discard = listOf(top), color = top.color,
            turn = first, dir = 1, phase = Phase.PLAY, dealer = dealer, scores = scores,
            round = s0.round + 1, version = s0.version, turnAt = now, lastHumanAt = s0.lastHumanAt,
            event = "${dealerSeat.name} shuffled and dealt · first card ${top.label}",
        )
        return when (top.face) {
            Face.SKIP -> st.copy(turn = (dealer + 2) % n)
            Face.REVERSE -> st.copy(dir = -1, turn = dealer)
            Face.DRAW2 -> give(st, first, 2, rnd).copy(turn = (dealer + 2) % n)
            Face.WILD -> st.copy(phase = Phase.CHOOSE_COLOR)
            else -> st
        }
    }

    /** Moves [count] cards to [seat], reshuffling the discard pile (minus the top card) when the deck runs out. */
    fun give(s: GameState, seat: Int, count: Int, rnd: Random): GameState {
        val draw = s.draw.toMutableList()
        var discard = s.discard
        val hand = s.hands[seat].toMutableList()
        repeat(count) {
            if (draw.isEmpty() && discard.size > 1) {
                draw.addAll(Deck.shuffle(discard.dropLast(1), rnd))
                discard = listOf(discard.last())
            }
            if (draw.isNotEmpty()) hand.add(draw.removeAt(draw.lastIndex))
        }
        return s.copy(draw = draw, discard = discard, hands = s.hands.replace(seat, hand))
    }

    fun ordinal(i: Int) = when (i) {
        0 -> "1st"; 1 -> "2nd"; 2 -> "3rd"; else -> "${i + 1}th"
    }
}

fun <T> List<T>.replace(i: Int, v: T): List<T> = toMutableList().also { it[i] = v }
