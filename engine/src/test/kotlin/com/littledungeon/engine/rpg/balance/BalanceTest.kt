package com.littledungeon.engine.rpg.balance

import kotlin.test.Test
import kotlin.test.assertEquals

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
}
