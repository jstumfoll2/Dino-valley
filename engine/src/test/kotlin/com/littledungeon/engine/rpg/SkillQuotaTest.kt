package com.littledungeon.engine.rpg

import com.littledungeon.engine.rpg.hero.Hero
import com.littledungeon.engine.rpg.learn.Skill
import com.littledungeon.engine.rpg.learn.SkillBook
import com.littledungeon.engine.rpg.learn.Thing
import com.littledungeon.engine.rpg.run.Journey
import com.littledungeon.engine.rpg.run.PICK_ONE_SKILLS
import com.littledungeon.engine.rpg.run.puzzleFor
import com.littledungeon.engine.rpg.world.WorldMemory
import com.littledungeon.engine.util.Clock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Puzzles are costumes: any pick-one skill can be set up in any words, and the skill is the child's to need. */
class SkillQuotaTest {
    @Test
    fun `every pick-one skill can be set up in any words at every level`() {
        val j = Journey(1, Hero(), SkillBook(), WorldMemory(), Clock { 0L })
        for (skill in PICK_ONE_SKILLS) for (thing in listOf(Thing.STONE, Thing.COIN, Thing.GEM, Thing.MUSHROOM)) repeat(10) {
            val c = j.puzzleFor(skill, if (skill == Skill.COLORS) "The frog" else "Something happens.", thing)
            assertEquals(skill, c.skill)
            assertTrue(c.answer in 0 until c.optionCount)
        }
    }
}
