package com.littledungeon.engine.rpg

import com.littledungeon.engine.rpg.learn.ChallengeRecord
import com.littledungeon.engine.rpg.learn.Skill
import com.littledungeon.engine.rpg.learn.SkillBook
import com.littledungeon.engine.rpg.learn.Trend
import com.littledungeon.engine.rpg.learn.progressOf
import kotlin.test.Test
import kotlin.test.assertEquals

class ProgressTest {
    private fun rec(ok: Boolean) = ChallengeRecord(Skill.COUNTING, "count", 1, if (ok) 1 else 2, 0, 1, 0, 0)
    private fun counting(results: List<Boolean>) = progressOf(results.map(::rec), SkillBook()).first { it.skill == Skill.COUNTING }

    @Test
    fun `a skill with little practice is new, and one that has got better says so`() {
        assertEquals(Trend.NEW, counting(listOf(true, false)).trend)
        assertEquals(Trend.GETTING_EASIER, counting(List(8) { false } + List(8) { true }).trend)
        assertEquals(Trend.NEEDS_TIME, counting(List(8) { true } + List(8) { false }).trend)
        assertEquals(Trend.STEADY, counting(List(16) { it % 2 == 0 }).trend)
    }

    @Test
    fun `every skill is listed, even ones never asked`() {
        val all = progressOf(emptyList(), SkillBook())
        assertEquals(Skill.entries.size, all.size)
        assertEquals(0, all.first().answered)
    }
}
