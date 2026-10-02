package com.dinovalley.engine.rpg

import com.dinovalley.engine.rpg.hero.Hero
import com.dinovalley.engine.rpg.hero.HeroClass
import com.dinovalley.engine.rpg.hero.Progression
import com.dinovalley.engine.rpg.learn.Challenge
import com.dinovalley.engine.rpg.learn.ChallengeFactory
import com.dinovalley.engine.rpg.learn.Coach
import com.dinovalley.engine.rpg.learn.MemoryChallenge
import com.dinovalley.engine.rpg.learn.PickOne
import com.dinovalley.engine.rpg.learn.PotionKind
import com.dinovalley.engine.rpg.learn.RecipeChallenge
import com.dinovalley.engine.rpg.learn.Skill
import com.dinovalley.engine.rpg.learn.SkillBook
import com.dinovalley.engine.rpg.learn.Thing
import com.dinovalley.engine.rpg.learn.TraceChallenge
import com.dinovalley.engine.rpg.run.Adventure
import com.dinovalley.engine.rpg.run.Beat
import com.dinovalley.engine.rpg.run.Place
import com.dinovalley.engine.rpg.run.Reply
import com.dinovalley.engine.rpg.world.WorldMemory
import com.dinovalley.engine.util.Clock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AdventureTest {

    private fun check(c: Challenge) {
        when (c) {
            is PickOne -> assertTrue(c.answer in 0 until c.optionCount, "answer missing in $c")
            is MemoryChallenge -> assertTrue(c.sequence.all { it in c.doors.indices } && c.sequence.toSet().size == c.sequence.size)
            is RecipeChallenge -> assertTrue(c.steps.all { it.ingredient in c.shelf && it.count >= 1 })
            is TraceChallenge -> assertTrue(c.path.all { it.x in 0f..1f && it.y in 0f..1f })
        }
    }

    /** Plays a whole adventure: answers right after [misses] wrong tries, picks option [choice]. */
    private fun play(a: Adventure, misses: Int = 0, choice: Int = 0): List<Beat> {
        val seen = mutableListOf<Beat>()
        var guard = 0
        while (!a.finished) {
            val b = a.beat
            seen += b
            val reply = when (b) {
                is Beat.Ask -> {
                    check(b.challenge)
                    Reply.Solved(tries = 1 + misses, hints = misses, millis = 1000)
                }
                is Beat.Roll -> Reply.Rolled(usedReroll = b.reroll != null, sumTries = if (b.askSum) 1 + misses else 0)
                is Beat.Choose -> Reply.Picked(choice.coerceAtMost(b.options.lastIndex))
                is Beat.Doors -> {
                    b.clue?.let { check(it) }
                    Reply.Picked(b.clue?.answer ?: 0, tries = 1 + misses)
                }
                else -> Reply.Next
            }
            a.reply(reply)
            assertTrue(++guard < 200, "adventure never ended")
        }
        seen += a.beat
        return seen
    }

    @Test
    fun `every adventure reaches the lair, lights three stars and ends with stars earned`() {
        for (seed in 1L..60L) {
            for (heroClass in listOf(HeroClass.KNIGHT, HeroClass.WIZARD, HeroClass.RANGER, HeroClass.GUARDIAN)) {
                val a = Adventure(seed, Hero(heroClass), SkillBook(), WorldMemory(), Clock { 0L })
                val beats = play(a, misses = (seed % 3).toInt(), choice = (seed % 3).toInt())
                val places = beats.map { it.scene.place }.toSet()
                assertTrue(listOf(Place.CAMP, Place.GATE, Place.GOBLIN_DEN, Place.WORKSHOP, Place.LAIR).all { it in places }, "missed a landmark: $places")
                assertEquals(3, beats.filter { it.scene.place == Place.LAIR }.maxOf { it.scene.bossStars ?: 0 })
                val finale = beats.last() as Beat.Finale
                assertTrue(finale.summary.starsEarned.values.sum() > 50)
                assertTrue(a.records.size >= 4)
                assertEquals(1, a.world.adventures)
                assertTrue(a.world.endings.isNotEmpty())
            }
        }
    }

    @Test
    fun `the same seed tells the same adventure`() {
        val one = play(Adventure(42, Hero(), SkillBook(), WorldMemory(), Clock { 0L }))
        val two = play(Adventure(42, Hero(), SkillBook(), WorldMemory(), Clock { 0L }))
        assertEquals(one, two)
    }

    @Test
    fun `the world remembers between adventures and the story changes`() {
        var world = WorldMemory()
        var hero = Hero()
        var skills = SkillBook()
        val quests = mutableSetOf<Any>()
        for (seed in 1L..6L) {
            val a = Adventure(seed, hero, skills, world, Clock { seed * 1000 })
            quests += a.quest.kind
            play(a)
            world = a.world
            hero = a.hero
            skills = a.skills
        }
        assertEquals(6, world.adventures)
        assertTrue(quests.size >= 2, "storylines should rotate")
        assertTrue(world.friends.isNotEmpty())
        assertNotNull(world.dragonFriend)
        assertTrue(hero.level >= 3)
        assertTrue(Skill.entries.any { skills.level(it) > 1 }, "first-try wins should raise skills")
    }

    @Test
    fun `every challenge is valid at every level`() {
        for (level in 1..5) for (seed in 1L..50L) {
            check(ChallengeFactory.count(level, seed, Thing.STONE, "How many?"))
            check(ChallengeFactory.add(level, seed, Thing.COIN) { h, m, _ -> "$h and $m" })
            check(ChallengeFactory.color(level, seed, "The wizard"))
            check(ChallengeFactory.pattern(level, seed))
            check(ChallengeFactory.letter(level, seed, "The spell needs a rune."))
            check(ChallengeFactory.trace(level, seed, "to the crystal."))
            check(ChallengeFactory.memory(level, seed))
            check(ChallengeFactory.recipe(level, seed, PotionKind.GLOW))
            val p = ChallengeFactory.pattern(level, seed)
            assertEquals(p.optionCount, p.options.toSet().size, "pattern options must differ")
            val c = ChallengeFactory.color(level, seed, "The wizard")
            assertEquals(c.options.size, c.options.toSet().size, "color options must differ")
        }
    }

    @Test
    fun `hints grow with each miss`() {
        assertEquals(Coach.Verdict(false, null, null), Coach.judge(3, 0, 1, 1))
        assertEquals(listOf(0, 2), Coach.judge(3, 0, 1, 2).keep)
        assertNull(Coach.judge(3, 0, 1, 2).glow)
        assertEquals(0, Coach.judge(3, 0, 1, 3).glow)
        assertTrue(Coach.judge(3, 0, 0, 5).correct)
    }

    @Test
    fun `levels need a little more each time`() {
        assertEquals(1, Progression.levelFor(0))
        assertEquals(2, Progression.levelFor(60))
        assertEquals(3, Progression.levelFor(140))
        assertEquals(2, Progression.levelFor(139))
        assertEquals(listOf("feather_hat", "class_guardian"), Progression.unlocksBetween(1, 3).map { it.id })
    }
}
