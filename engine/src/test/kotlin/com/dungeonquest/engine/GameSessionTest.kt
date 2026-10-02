package com.dungeonquest.engine

import com.dungeonquest.engine.activity.count.CountObjectsGenerator
import com.dungeonquest.engine.activity.count.PrototypeCountingLevels
import com.dungeonquest.engine.model.ChildResponse.NumberChosen
import com.dungeonquest.engine.model.SpriteId
import com.dungeonquest.engine.session.GameSession
import com.dungeonquest.engine.session.SessionEvent
import com.dungeonquest.engine.session.SessionState
import com.dungeonquest.engine.util.Clock
import com.dungeonquest.engine.util.GameRandom
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class GameSessionTest {
    private var now = 0L
    private fun session() = GameSession(
        generator = CountObjectsGenerator(listOf(SpriteId("egg")), emptyList()),
        level = PrototypeCountingLevels.level(2),
        random = GameRandom(7),
        clock = Clock { now },
        itemsInRound = 5,
    )

    @Test
    fun `a round of five right answers ends with five first-try outcomes`() {
        val s = session()
        assertIs<SessionState.Introducing>(s.state).also { assertTrue(it.demonstrate) }
        repeat(5) {
            val intro = assertIs<SessionState.Introducing>(s.state)
            s.onEvent(SessionEvent.IntroFinished)
            now += 2_000
            s.onEvent(SessionEvent.Answered(NumberChosen(intro.item.answer)))
            assertIs<SessionState.Celebrating>(s.state)
            s.onEvent(SessionEvent.CelebrationFinished)
        }
        val done = assertIs<SessionState.RoundComplete>(s.state)
        assertEquals(5, done.outcomes.size)
        assertTrue(done.outcomes.all { it.firstTryCorrect && it.tries == 1 && it.responseTimeMs == 2_000L })
    }

    @Test
    fun `wrong answers get help and the item still ends in success`() {
        val s = session()
        val item = assertIs<SessionState.Introducing>(s.state).item
        val wrong = item.choices.first { it != item.answer }
        s.onEvent(SessionEvent.IntroFinished)
        repeat(3) {
            assertIs<SessionState.Helping>(s.onEvent(SessionEvent.Answered(NumberChosen(wrong))).let { st ->
                if (st is SessionState.AwaitingAnswer) error("expected help") else st
            })
            s.onEvent(SessionEvent.HintShown)
        }
        val awaiting = assertIs<SessionState.AwaitingAnswer>(s.state)
        assertEquals(2, awaiting.choices.size)
        assertTrue(item.answer in awaiting.choices)
        val celebrating = assertIs<SessionState.Celebrating>(s.onEvent(SessionEvent.Answered(NumberChosen(item.answer))))
        assertEquals(false, celebrating.outcome.firstTryCorrect)
        assertEquals(4, celebrating.outcome.tries)
        assertEquals(3, celebrating.outcome.hintsUsed)
    }

    @Test
    fun `events that do not fit the current state are ignored`() {
        val s = session()
        val before = s.state
        s.onEvent(SessionEvent.CelebrationFinished)
        s.onEvent(SessionEvent.Answered(NumberChosen(1)))
        assertEquals(before, s.state)
    }
}
