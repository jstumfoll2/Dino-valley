package com.littledungeon.engine.rpg.balance

import com.littledungeon.engine.rpg.learn.Skill
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * What must hold for every kind of player. The numbers each fix is held to (journey length, answer
 * position, skill mix, ambushes) are added next to the fix that makes them true; see
 * `docs/review/improvement-plan.md`.
 */
class BalanceTest {
    @Test
    fun everySimulatedJourneyReachesTheEnd() {
        for (accuracy in listOf(0.3, 0.75, 1.0)) {
            val batch = BalanceSim.run(accuracy, children = 3, journeysEach = 3)
            assertEquals(batch.journeys, batch.finished, "at accuracy $accuracy only ${batch.finished} of ${batch.journeys} journeys finished")
        }
    }

    /** The skills that come up every journey; the rarer ones need their own minigames first. */
    private val everyday = listOf(Skill.COUNTING, Skill.NUMBERS, Skill.ADDITION, Skill.COLORS, Skill.PATTERNS, Skill.LETTERS)

    private fun BalanceSim.Batch.meanLevel() = everyday.map { avgLevel(it) }.average()

    @Test
    fun levelsFollowWhatTheChildShows() {
        val weak = BalanceSim.run(0.6, children = 6, journeysEach = 8)
        val strong = BalanceSim.run(0.9, children = 6, journeysEach = 8)
        // Before: a child who got 60 percent right was at level 5 in every skill after ten journeys.
        assertTrue(weak.meanLevel() <= 2.0, "a 60% child should not be pushed up: mean level ${weak.meanLevel()}")
        assertTrue(strong.meanLevel() >= weak.meanLevel() + 1.0, "a 90% child should be well ahead of a 60% one: ${strong.meanLevel()} vs ${weak.meanLevel()}")
    }

    @Test
    fun aChildWhoOnlyGuessesNeverClimbs() {
        assertTrue(BalanceSim.guesserLevel() <= 1, "guesser reached level ${BalanceSim.guesserLevel()}")
    }
}
