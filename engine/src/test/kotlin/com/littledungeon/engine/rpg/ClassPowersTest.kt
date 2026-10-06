package com.littledungeon.engine.rpg

import com.littledungeon.engine.model.Voice
import com.littledungeon.engine.rpg.content.Content
import com.littledungeon.engine.rpg.hero.Attribute
import com.littledungeon.engine.rpg.hero.Hero
import com.littledungeon.engine.rpg.hero.HeroClass
import com.littledungeon.engine.rpg.hero.Progression
import com.littledungeon.engine.rpg.hero.SPARKLE_BREATH_LEVEL
import com.littledungeon.engine.rpg.items.Obstacle
import com.littledungeon.engine.rpg.learn.PickOne
import com.littledungeon.engine.rpg.learn.SkillBook
import com.littledungeon.engine.rpg.run.Beat
import com.littledungeon.engine.rpg.run.Journey
import com.littledungeon.engine.rpg.run.Place
import com.littledungeon.engine.rpg.run.Reply
import com.littledungeon.engine.rpg.run.PICK_ONE_SKILLS
import com.littledungeon.engine.rpg.run.Scene
import com.littledungeon.engine.rpg.run.battle
import com.littledungeon.engine.rpg.run.obstacle
import com.littledungeon.engine.rpg.run.sparklesLeft
import com.littledungeon.engine.rpg.story.Cond
import com.littledungeon.engine.rpg.world.WorldMemory
import com.littledungeon.engine.util.Clock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Every class has a power that does something in a journey, and every reward for levelling up is real. */
class ClassPowersTest {
    private fun journey(hero: Hero, seed: Long = 1) = Journey(seed, hero, SkillBook(levels = PICK_ONE_SKILLS.associateWith { 4 }), WorldMemory(), Clock { 0L })

    private fun said(b: Beat) = (b as? Beat.Tell)?.let { Voice.caption(it.lines) }.orEmpty()

    @Test
    fun `the Ranger sees dangers, the Guardian is always kind, others earn them`() {
        assertTrue(Hero(HeroClass.RANGER).seesDangers && !Hero(HeroClass.KNIGHT).seesDangers && !Hero(HeroClass.WIZARD).seesDangers)
        val kind = Cond.Stat(Attribute.KINDNESS, 9)
        assertTrue(journey(Hero(HeroClass.GUARDIAN)).holds(kind), "a Guardian is offered the kind choices from the start")
        assertFalse(journey(Hero(HeroClass.KNIGHT)).holds(kind))
        // And every friendship a Guardian makes is worth one more point.
        val g = journey(Hero(HeroClass.GUARDIAN))
        g.befriend("baker_bun", 2)
        assertEquals(3, g.relation("baker_bun"))
        val k = journey(Hero(HeroClass.KNIGHT))
        k.befriend("baker_bun", 2)
        assertEquals(2, k.relation("baker_bun"))
    }

    @Test
    fun `sparkle magic is one charge for a Wizard, two for a Spellkeeper, and the dragon adds one at level five`() {
        assertEquals(0, Hero(HeroClass.KNIGHT).sparkleCharges)
        assertEquals(1, Hero(HeroClass.WIZARD).sparkleCharges)
        assertEquals(2, Hero(HeroClass.SPELLKEEPER).sparkleCharges)
        val fifth = Progression.xpFor(SPARKLE_BREATH_LEVEL)
        val veteran = Hero(HeroClass.KNIGHT, xp = mapOf(Attribute.COURAGE to fifth))
        assertEquals(SPARKLE_BREATH_LEVEL, veteran.level)
        assertEquals(1, veteran.sparkleCharges, "sparkle breath")
        assertEquals(3, Hero(HeroClass.SPELLKEEPER, xp = mapOf(Attribute.COURAGE to fifth)).sparkleCharges)
    }

    @Test
    fun `a missed puzzle offers sparkle magic once to a Wizard, then turns back`() {
        val j = journey(Hero(HeroClass.WIZARD))
        var through = 0
        var back = 0
        j.load(j.obstacle(Obstacle.CLIMB, Scene(Place.CAMP, j.cast), next = { through++; emptyList() }, turnBack = { back++; emptyList() }))
        j.reply(Reply.Next) // the introduction
        val first = j.beat as Beat.Ask
        val wrong = (first.challenge as PickOne).let { listOf((it.answer + 1) % it.optionCount) }
        j.reply(Reply.Solved(2, 0, 0, failed = true, wrong = wrong))
        val offer = j.beat as Beat.Choose
        assertTrue(offer.options.any { it.icon == "item_magic_wand" }, "sparkle magic is offered, shown as the wand")
        j.reply(Reply.Picked(0))
        assertTrue("Sparkle magic" in said(j.beat))
        j.reply(Reply.Next)
        val second = j.beat as Beat.Ask
        assertTrue(second.tried.size > wrong.size, "a wrong answer faded away")
        j.reply(Reply.Solved(2, 0, 0, failed = true, wrong = second.tried))
        // One charge, one use: now it is a turn back.
        assertTrue(j.beat is Beat.Tell, "no second offer: ${j.beat::class.simpleName}")
        assertEquals(0, j.sparklesLeft())
    }

    @Test
    fun `a knight who is knocked out stands back up once a journey, and the second time faints`() {
        val j = journey(Hero(HeroClass.KNIGHT))
        val foe = Content.monster("cave_troll")!!
        var rose = 0
        var wins = 0
        j.load(j.battle(foe, Place.CAMP, onWin = { wins++; emptyList() }))
        var guard = 0
        while (j.faints == 0 && guard++ < 200 && j.beat !is Beat.Finale) {
            val b = j.beat
            if ("stand right back up" in said(b) || "brave heart lifts you" in said(b)) rose++
            j.reply(
                when (b) {
                    is Beat.Choose -> Reply.Picked(0) // attack
                    is Beat.Ask -> Reply.Solved(2, 0, 0, failed = true, wrong = listOf((((b.challenge as? PickOne)?.answer ?: 0) + 1) % ((b.challenge as? PickOne)?.optionCount ?: 2)))
                    else -> Reply.Next
                },
            )
        }
        assertEquals(1, rose, "the brave heart saves the knight once")
        assertEquals(1, j.faints, "and then they are carried off as anyone would be")
        assertEquals(0, wins)
    }

    @Test
    fun `every reward for levelling up is something the game really gives`() {
        val ids = Progression.unlocks.map { it.id }
        assertEquals(listOf("feather_hat", "class_guardian", "star_cape", "dragon_sparkles", "class_spellkeeper"), ids)
        // Heroes to play are unlocked at the level the announcement says.
        for (c in HeroClass.entries.filter { it.unlockLevel > 1 }) {
            assertEquals(c.unlockLevel, Progression.unlocks.first { it.id == "class_${c.name.lowercase()}" }.level)
        }
        assertEquals(SPARKLE_BREATH_LEVEL, Progression.unlocks.first { it.id == "dragon_sparkles" }.level)
    }
}
