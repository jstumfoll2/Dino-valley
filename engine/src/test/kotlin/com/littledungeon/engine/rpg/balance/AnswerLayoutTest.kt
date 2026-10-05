package com.littledungeon.engine.rpg.balance

import com.littledungeon.engine.rpg.learn.ChallengeFactory
import com.littledungeon.engine.rpg.learn.PickOne
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Where the right answer sits must tell a child nothing. Counting at levels 4 and 5 used to put it in the
 * middle card every time (the options were the answer and its neighbours, sorted).
 */
class AnswerLayoutTest {
    private fun fair(name: String, make: (Int, Long) -> PickOne) {
        for (level in 1..5) {
            val shares = BalanceSim.positionShare(level, make, 3000)
            val expected = 1.0 / shares.size
            shares.forEachIndexed { position, share ->
                assertTrue(abs(share - expected) < 0.05, "$name level $level: the right answer is in position $position ${"%.0f".format(share * 100)}% of the time, fair is ${"%.0f".format(expected * 100)}%")
            }
        }
    }

    @Test fun counting() = fair("counting", BalanceSim.countMaker)

    @Test fun adding() = fair("adding", BalanceSim.addMaker)

    @Test fun skipCounting() = fair("skip counting", BalanceSim.skipMaker)

    @Test fun numerals() = fair("numerals") { lv, s -> ChallengeFactory.numeral(lv, s, "") }

    @Test fun colors() = fair("colors") { lv, s -> ChallengeFactory.color(lv, s, "") }

    @Test fun patterns() = fair("patterns") { lv, s -> ChallengeFactory.pattern(lv, s) }

    @Test fun letters() = fair("letters") { lv, s -> ChallengeFactory.letter(lv, s, "") }
}
