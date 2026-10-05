package com.littledungeon.engine.rpg

import com.littledungeon.engine.rpg.hero.Hero
import com.littledungeon.engine.rpg.learn.Skill
import com.littledungeon.engine.rpg.learn.SkillBook
import com.littledungeon.engine.rpg.run.Beat
import com.littledungeon.engine.rpg.run.Journey
import com.littledungeon.engine.rpg.run.Place
import com.littledungeon.engine.rpg.run.Reply
import com.littledungeon.engine.rpg.run.Settings
import com.littledungeon.engine.rpg.run.luckyRoll
import com.littledungeon.engine.rpg.world.WorldMemory
import com.littledungeon.engine.util.Clock
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** What a grown-up can set, and the lucky dice on the road. */
class SettingsTest {
    private fun journey(settings: Settings, skills: SkillBook = SkillBook(), seed: Long = 1) =
        Journey(seed, Hero(), skills, WorldMemory(), Clock { 0L }, settings)

    @Test
    fun `the puzzle level stays between the floor and the ceiling`() {
        val strong = SkillBook(levels = Skill.entries.associateWith { 5 })
        val capped = journey(Settings(levelCeiling = 2), strong)
        capped.warmedUp.addAll(Skill.entries)
        assertEquals(2, capped.level(Skill.COUNTING))
        val raised = journey(Settings(levelFloor = 3))
        raised.warmedUp.addAll(Skill.entries)
        assertEquals(3, raised.level(Skill.COUNTING))
    }

    @Test
    fun `settings that make no sense are refused`() {
        assertTrue(runCatching { Settings(levelFloor = 4, levelCeiling = 2) }.isFailure)
        assertTrue(runCatching { Settings(dayMinutes = 1) }.isFailure)
    }

    @Test
    fun `a shorter day camps sooner`() {
        fun nights(minutes: Int): Int {
            var n = 0
            for (seed in 1L..4L) {
                val j = journey(Settings(dayMinutes = minutes), seed = seed)
                val r = Random(seed)
                var guard = 0
                while (!j.finished && guard++ < 4000) {
                    val b = j.beat
                    if (b is Beat.Night) n++
                    j.reply(
                        when (b) {
                            is Beat.Ask -> Reply.Solved(1, 0, 1000)
                            is Beat.Choose -> Reply.Picked(r.nextInt(b.options.size))
                            is Beat.Travel -> Reply.Picked(b.routes.indexOfFirst { it.marked }.coerceAtLeast(0))
                            is Beat.Roll -> Reply.Rolled(false, 1)
                            else -> Reply.Next
                        },
                    )
                }
            }
            return n
        }
        assertTrue(nights(5) > nights(20), "5-minute days make more nights than 20-minute days")
    }

    @Test
    fun `with two tries a miss gets another look with a wrong answer crossed out`() {
        for (twoTries in listOf(false, true)) {
            var gaveUp = 0
            var second = 0
            for (seed in 1L..6L) {
                val j = journey(Settings(twoTries = twoTries), seed = seed)
                val r = Random(seed)
                var guard = 0
                while (!j.finished && guard++ < 4000) {
                    val b = j.beat
                    if (b is Beat.Ask && b.tried.isNotEmpty() && twoTries) second++
                    val reply: Reply = when (b) {
                        // Always wrong on the first try of a one-try puzzle.
                        is Beat.Ask -> if (b.oneTry && b.tried.isEmpty()) Reply.Solved(1, 0, 10, failed = true, wrong = listOf(0)) else Reply.Solved(1, 0, 10)
                        is Beat.Choose -> Reply.Picked(r.nextInt(b.options.size))
                        is Beat.Travel -> Reply.Picked(b.routes.indexOfFirst { it.marked }.coerceAtLeast(0))
                        is Beat.Roll -> Reply.Rolled(false, 1)
                        else -> Reply.Next
                    }
                    if (b is Beat.Choose && b.options.any { it.said == "Give up" }) gaveUp++
                    j.reply(reply)
                }
            }
            if (twoTries) assertTrue(second > 0, "a second look is offered") else assertEquals(0, second)
        }
    }

    @Test
    fun `lucky dice add up to coins, and a low roll is silly rather than bad`() {
        for (seed in 1L..40L) {
            val j = journey(Settings(), seed = seed)
            val coinsBefore = j.hero.coins
            val roll = j.luckyRoll(j.scene(Place.CAMP)).single()
            val b = roll.beat as Beat.Roll
            val after = roll.then(Reply.Rolled(false, 1))
            assertTrue(after.isNotEmpty())
            assertEquals(if (b.total <= 4) 1 else b.total, j.hero.coins - coinsBefore)
        }
    }
}
