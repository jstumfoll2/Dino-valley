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
        // Ten kinds of pick-one puzzle share the practice now (it was seven), so each of the everyday ones climbs more slowly: the gap is smaller, not gone.
        assertTrue(strong.meanLevel() >= weak.meanLevel() + 0.75, "a 90% child should be well ahead of a 60% one: ${strong.meanLevel()} vs ${weak.meanLevel()}")
    }

    @Test
    fun aChildWhoOnlyGuessesNeverClimbs() {
        assertTrue(BalanceSim.guesserLevel() <= 1, "guesser reached level ${BalanceSim.guesserLevel()}")
    }

    @Test
    fun noSkillTakesOverAJourney() {
        val batch = BalanceSim.run(0.75, children = 5, journeysEach = 6)
        val pickOne = listOf(Skill.COUNTING, Skill.NUMBERS, Skill.ADDITION, Skill.COLORS, Skill.PATTERNS, Skill.LETTERS, Skill.SKIP_COUNTING)
        for (s in pickOne) {
            val share = batch.share(s)
            // Before, patterns were 19% of all puzzles and counting 9%.
            assertTrue(share in 0.08..0.20, "$s is ${"%.0f".format(share * 100)}% of puzzles")
        }
        val rare = listOf(Skill.TRACING, Skill.MEMORY, Skill.SORTING, Skill.PUZZLES).sumOf { batch.share(it) }
        assertTrue(rare >= 0.04, "tracing, memory, sorting and jigsaws were 1% each; now ${"%.1f".format(rare * 100)}% together")
    }

    @Test
    fun aSittingIsShortAndAJourneyIsMoreThanOne() {
        for (accuracy in listOf(0.6, 0.9)) {
            val batch = BalanceSim.run(accuracy, children = 5, journeysEach = 6)
            // Before: one unbroken sitting of about 25 minutes, with no way to put the game down.
            assertTrue(batch.medianSession <= 12.0, "at $accuracy the median sitting is ${"%.1f".format(batch.medianSession)} minutes")
            assertTrue(batch.percentile(batch.sessionMinutes, 0.9) <= 17.0, "at $accuracy a long sitting is ${"%.1f".format(batch.percentile(batch.sessionMinutes, 0.9))} minutes")
            assertTrue(batch.nights >= batch.journeys, "at $accuracy a journey has ${"%.1f".format(batch.nights.toDouble() / batch.journeys)} nights; every one should have at least one")
        }
    }

    @Test
    fun nobodyIsForcedToFightOrAmbushedByAFriend() {
        for (accuracy in listOf(0.6, 0.9)) {
            val batch = BalanceSim.run(accuracy, children = 5, journeysEach = 6)
            assertEquals(0, batch.forcedFights, "at $accuracy a conversation offered only a fight")
            assertEquals(0, batch.namedAmbushes, "at $accuracy a person with a story was met as a random monster")
        }
    }
}
