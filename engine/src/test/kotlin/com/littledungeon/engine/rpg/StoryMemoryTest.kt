package com.littledungeon.engine.rpg

import com.littledungeon.engine.model.Voice
import com.littledungeon.engine.rpg.content.Content
import com.littledungeon.engine.rpg.hero.Attribute
import com.littledungeon.engine.rpg.hero.Hero
import com.littledungeon.engine.rpg.hero.HeroClass
import com.littledungeon.engine.rpg.learn.SkillBook
import com.littledungeon.engine.rpg.run.Beat
import com.littledungeon.engine.rpg.run.Journey
import com.littledungeon.engine.rpg.run.Reply
import com.littledungeon.engine.rpg.run.lairArrival
import com.littledungeon.engine.rpg.run.priceAt
import com.littledungeon.engine.rpg.run.roadMonster
import com.littledungeon.engine.rpg.story.Cond
import com.littledungeon.engine.rpg.story.Effect
import com.littledungeon.engine.rpg.world.Terrain
import com.littledungeon.engine.rpg.world.WorldMemory
import com.littledungeon.engine.util.Clock
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** What the world remembers has to change what happens: friends, rivals, clues, prices, and the second telling of a story. */
class StoryMemoryTest {
    private val baron = "baron_grumblewick"

    /** A hero strong enough that fights do not decide the story. */
    private val strong = Hero(HeroClass.KNIGHT, xp = mapOf(Attribute.COURAGE to 4000, Attribute.CLEVERNESS to 400), coins = 200)

    private fun said(b: Beat) = (b as? Beat.Tell)?.let { Voice.caption(it.lines) }.orEmpty()

    /** After the first adventure the story is picked by chance; these tests are about the Baron's, so the others have been told many times. */
    private fun onlyThe(first: WorldMemory): WorldMemory = if (first.adventures == 0) first else first.copy(
        arcsDone = Content.arcs.associate { it.id to if (it.id == "missing_pages") first.arcsDone["missing_pages"] ?: 0 else 9 },
        lastArc = Content.arcs.first { it.id != "missing_pages" }.id,
    )

    /** Plays a whole journey; at the lair, makes peace or fights as asked. */
    private fun play(world: WorldMemory, peace: Boolean, seed: Long = 5): Pair<Journey, List<Beat>> {
        val j = Journey(seed, strong, SkillBook(), onlyThe(world), Clock { 0L })
        val r = Random(seed)
        val seen = mutableListOf<Beat>()
        var guard = 0
        while (!j.finished && guard++ < 4000) {
            val b = j.beat
            seen += b
            j.reply(
                when (b) {
                    is Beat.Travel -> {
                        val open = b.routes.withIndex().filter { !it.value.blocked }.ifEmpty { b.routes.withIndex().toList() }
                        Reply.Picked((open.firstOrNull { it.value.marked } ?: open.random(r)).index)
                    }
                    is Beat.Choose -> {
                        val peaceAt = b.options.indexOfFirst { it.icon == "hub_peace" }
                        val enterAt = b.options.indexOfFirst { it.icon == "hub_enter" }
                        when {
                            peaceAt >= 0 -> Reply.Picked(if (peace) peaceAt else b.options.indexOfFirst { it.icon == "hub_fight" })
                            enterAt >= 0 -> Reply.Picked(enterAt) // the key is in a dungeon
                            else -> Reply.Picked(b.options.indexOfFirst { it.icon == "talk_leave" || it.icon == "hub_leave" }.takeIf { it >= 0 } ?: 0)
                        }
                    }
                    is Beat.Ask -> Reply.Solved(1, 0, 1000)
                    else -> Reply.Next
                },
            )
        }
        assertTrue(j.finished, "the journey ended")
        return j to seen
    }

    private val firstTime = WorldMemory()

    @Test
    fun `a boss made a friend asks for help next time, and the story is told shorter`() {
        val world = WorldMemory(adventures = 1, pages = 1, arcsDone = mapOf("missing_pages" to 1), lastArc = "missing_pages", flags = setOf("friend:$baron"))
        val (j, beats) = play(world, peace = true)
        assertTrue(said(beats.first()).startsWith("Chapter"), "the chapter comes first")
        assertTrue(beats.any { "Another page of the Great Storybook has gone missing" in said(it) }, "a second telling opens with what changed")
        assertTrue(beats.none { "The Great Storybook of Whisperwood is losing its pages" in said(it) }, "and not with the first telling")
        assertTrue(beats.any { "You came back. My ink got away from me again" in said(it) }, "the Baron greets a friend")
        assertTrue(beats.none { it is Beat.Choose && it.options.any { o -> o.icon == "hub_fight" } }, "there is no fight to pick with a friend")
        assertTrue(beats.any { "writes you into the very first line" in said(it) }, "and the ending is a friend's")
        assertTrue("friend:$baron" in j.world.flags && "rival:$baron" !in j.world.flags)
        assertEquals(2, j.world.pages)
    }

    @Test
    fun `a boss who was beaten is remembered as a rival until peace is made`() {
        val (afterFight, _) = play(firstTime, peace = false)
        assertTrue("rival:$baron" in afterFight.world.flags && "friend:$baron" !in afterFight.world.flags)

        val (again, beats) = play(afterFight.world, peace = false, seed = 9)
        assertTrue(beats.any { "You again! The one who beat me" in said(it) }, "the Baron remembers")
        assertTrue(beats.any { it is Beat.Choose && it.options.any { o -> o.icon == "hub_fight" } }, "a rival can still be fought")
        assertTrue("rival:$baron" in again.world.flags)

        val (peaceMade, _) = play(again.world, peace = true, seed = 11)
        assertTrue("friend:$baron" in peaceMade.world.flags && "rival:$baron" !in peaceMade.world.flags, "peace makes a friend of a rival")
    }

    private fun lairAsks(flags: Set<String>): Pair<Int, Boolean> {
        val j = Journey(1, strong.give("silver_quill"), SkillBook(), WorldMemory(flags = flags), Clock { 0L })
        j.load(j.lairArrival(Content.kingdom.location(j.arc.lairId)))
        var asks = 0
        var skipped = false
        var guard = 0
        while (!j.finished && guard++ < 200) {
            val b = j.beat
            if (b is Beat.Ask) asks++
            if ("skip the first page" in said(b)) skipped = true
            j.reply(
                when (b) {
                    is Beat.Choose -> Reply.Picked(b.options.indexOfFirst { it.icon == "hub_peace" })
                    is Beat.Ask -> Reply.Solved(1, 0, 0)
                    else -> Reply.Next
                },
            )
        }
        return asks to skipped
    }

    @Test
    fun `learning why the Baron is lonely spares a puzzle`() {
        val (withoutClue, skippedWithout) = lairAsks(emptySet())
        val (withClue, skipped) = lairAsks(setOf("knows_baron_lonely"))
        assertEquals(3, withoutClue)
        assertEquals(2, withClue, "he already knows you understand")
        assertTrue(skipped && !skippedWithout)
    }

    @Test
    fun `being good to people is kindness, and a shopkeeper who likes the hero charges less`() {
        val j = Journey(1, Hero(), SkillBook(), WorldMemory(), Clock { 0L })
        val bun = Content.npc("baker_bun")!!
        val cake = Content.item("honey_cake")!!
        val full = j.priceAt(bun, cake)
        assertEquals(0, j.hero.xp[Attribute.KINDNESS] ?: 0)
        j.befriend(bun.id, 3)
        assertEquals(15, j.hero.xp[Attribute.KINDNESS], "five stars of kindness for every point of friendship")
        assertTrue(j.priceAt(bun, cake) < full, "a friend pays less: $full then ${j.priceAt(bun, cake)}")
        j.befriend(bun.id, 30)
        assertEquals(j.hero.priceOf(cake) * 80 / 100, j.priceAt(bun, cake), "a friend's discount is never more than a fifth, on top of kindness")
        assertEquals(j.hero.priceOf(cake), j.priceAt(Content.npc("merchant_zig"), cake), "other shopkeepers do not know the hero")
        // Unkind or neutral changes earn nothing.
        val k = j.hero.xp[Attribute.KINDNESS]
        j.befriend(bun.id, -1)
        assertEquals(k, j.hero.xp[Attribute.KINDNESS])
    }

    @Test
    fun `a kind of monster that was made a friend does not ambush the hero again`() {
        val j = Journey(2, Hero(), SkillBook(), WorldMemory(), Clock { 0L })
        assertTrue((1..400).any { j.roadMonster(Terrain.FOREST, 1).id == "wolf_pup" }, "wolf pups ambush a stranger")
        j.setFlag("friend:wolf_pup")
        repeat(1000) { assertFalse(j.roadMonster(Terrain.FOREST, 1).id == "wolf_pup") }
    }

    @Test
    fun `a flag that is set is read somewhere, and a flag that is read is set somewhere`() {
        val set = mutableSetOf<String>()
        val read = mutableSetOf<String>()
        fun cond(c: Cond) {
            when (c) {
                is Cond.Flag -> read += c.name
                is Cond.NoFlag -> read += c.name
                is Cond.Not -> cond(c.cond)
                else -> Unit
            }
        }
        fun effect(e: Effect) {
            when (e) {
                is Effect.SetFlag -> set += e.name
                is Effect.ClearFlag -> read += e.name
                else -> Unit
            }
        }
        for (npc in Content.npcs) {
            npc.starts.forEach { it.needs.forEach(::cond) }
            for (n in npc.nodes) {
                n.effects.forEach(::effect)
                for (o in n.options) {
                    o.needs.forEach(::cond)
                    o.effects.forEach(::effect)
                }
            }
            read += npc.passFlags
        }
        for (arc in Content.arcs) {
            for (m in arc.moments) {
                m.needs.forEach(::cond)
                m.effects.forEach(::effect)
            }
            (arc.peaceSteps + arc.friendSteps).forEach { s -> s.skippedBy?.let { read += it } }
        }
        read += Content.flagRoads.keys
        // The engine sets and reads these itself: who has been met, who is a friend, who is a rival.
        fun byEngine(f: String) = listOf("met:", "friend:", "rival:").any { f.startsWith(it) }
        for (f in set) assertTrue(f in read || byEngine(f), "$f is set but nothing ever reads it")
        for (f in read) assertTrue(f in set || byEngine(f), "$f is read but nothing ever sets it")
    }

    @Test
    fun `a second telling of every story is shorter than the first`() {
        for (arc in Content.arcs) if (arc.returnSetup.isNotEmpty()) assertTrue(arc.returnSetup.size < arc.setup.size, arc.id)
    }
}
