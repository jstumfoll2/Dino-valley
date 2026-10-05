package com.littledungeon.engine.rpg

import com.littledungeon.engine.model.Speech
import com.littledungeon.engine.model.Voice
import com.littledungeon.engine.rpg.content.Content
import com.littledungeon.engine.rpg.hero.Attribute
import com.littledungeon.engine.rpg.hero.Hero
import com.littledungeon.engine.rpg.hero.HeroClass
import com.littledungeon.engine.rpg.learn.BellChallenge
import com.littledungeon.engine.rpg.learn.PictureChallenge
import com.littledungeon.engine.rpg.learn.SkillBook
import com.littledungeon.engine.rpg.learn.TraceChallenge
import com.littledungeon.engine.rpg.run.Beat
import com.littledungeon.engine.rpg.run.Journey
import com.littledungeon.engine.rpg.run.Reply
import com.littledungeon.engine.rpg.run.lairArrival
import com.littledungeon.engine.rpg.run.peaceChallenge
import com.littledungeon.engine.rpg.story.Cond
import com.littledungeon.engine.rpg.world.LocationKind
import com.littledungeon.engine.rpg.world.WorldMemory
import com.littledungeon.engine.util.Clock
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** Book One has several stories, each with its own lair, key, boss and people; every one has to play through, both ways. */
class ArcsTest {
    private val strong = Hero(HeroClass.KNIGHT, xp = mapOf(Attribute.COURAGE to 4000, Attribute.CLEVERNESS to 400), coins = 200)

    private fun said(b: Beat) = (b as? Beat.Tell)?.let { Voice.caption(it.lines) }.orEmpty()

    /** A journey that plays [arcId]: the story is picked by chance after the first adventure, so find a seed that picks it. */
    private fun journeyFor(arcId: String, flags: Set<String> = emptySet(), hero: Hero = strong): Journey {
        val world = if (arcId == Content.arcs.first().id) {
            WorldMemory(flags = flags)
        } else {
            WorldMemory(adventures = 1, pages = 1, arcsDone = mapOf(Content.arcs.first().id to 1), lastArc = Content.arcs.first().id, flags = flags)
        }
        for (seed in 1L..500L) {
            val j = Journey(seed, hero, SkillBook(), world, Clock { 0L })
            if (j.arc.id == arcId) return j
        }
        error("no seed picks $arcId")
    }

    /** Plays a whole journey. At the lair, makes peace or fights as asked. */
    private fun play(j: Journey, peace: Boolean): List<Beat> {
        val r = Random(j.seed)
        val seen = mutableListOf<Beat>()
        var guard = 0
        while (!j.finished && guard++ < 5000) {
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
                            enterAt >= 0 -> Reply.Picked(enterAt)
                            else -> Reply.Picked(b.options.indexOfFirst { it.icon == "talk_leave" || it.icon == "hub_leave" }.takeIf { it >= 0 } ?: 0)
                        }
                    }
                    is Beat.Ask -> Reply.Solved(1, 0, 1000)
                    else -> Reply.Next
                },
            )
        }
        assertTrue(j.finished, "${j.arc.id}: the journey ended")
        return seen
    }

    @Test
    fun `there are several stories, and each one is wired to places and things that exist`() {
        assertTrue(Content.arcs.size >= 3)
        assertEquals(Content.arcs.size, Content.arcs.map { it.lairId }.toSet().size, "each story has its own lair")
        assertEquals(Content.arcs.size, Content.arcs.map { it.keyItemId }.toSet().size, "and its own key")
        val kinds = setOf("letters", "pattern", "colors", "numbers", "count", "rhyme", "money", "share", "bats", "map", "bells", "trace")
        for (arc in Content.arcs) {
            assertEquals(LocationKind.LAIR, Content.kingdom.location(arc.lairId).kind, arc.id)
            assertEquals(LocationKind.DUNGEON, Content.kingdom.location(arc.keyDungeonId).kind, arc.id)
            assertNotNull(Content.item(arc.keyItemId), arc.id)
            assertNotNull(Content.monster(arc.bossId), arc.id)
            assertTrue(arc.variants.size >= 3, "${arc.id} can be told three ways")
            for (m in arc.moments) assertNotNull(Content.kingdom.locationOrNull(m.at), "${arc.id}: a clue at ${m.at}")
            for (s in arc.peaceSteps + arc.friendSteps) assertTrue(s.kind in kinds, "${arc.id}: unknown step ${s.kind}")
            // Every line parses: a typo in a speaker's tag is found here and not on a child's screen.
            val lines = arc.setup + arc.returnSetup + arc.moments.map { it.says } + arc.variants.flatMap { listOf(it.meeting, it.fightEnd, it.peaceEnd) } +
                listOfNotNull(arc.sealed, arc.keyFound, arc.gateOpens, arc.friendMeeting, arc.friendEnd, arc.rivalMeeting)
            lines.forEach { Speech.of(it) }
        }
    }

    @Test
    fun `every story can be played to its end, by making peace or by fighting, and it is remembered`() {
        for (arc in Content.arcs) for (peace in listOf(true, false)) {
            val j = journeyFor(arc.id)
            val beats = play(j, peace)
            val boss = arc.bossId
            assertEquals(1 + if (arc === Content.arcs.first()) 0 else 1, j.world.pages, "${arc.id}: a page came home")
            assertTrue(if (peace) "friend:$boss" in j.world.flags else "rival:$boss" in j.world.flags, "${arc.id}: the boss is remembered")
            assertTrue(j.hero.has(arc.keyItemId) || beats.any { it is Beat.Found }, "${arc.id}: the key was found on the way")
            assertTrue(beats.any { arc.title in said(it) }, "${arc.id}: the chapter names the story")
        }
    }

    @Test
    fun `a boss made a friend asks for help the next time, in the story's own words`() {
        for (arc in Content.arcs) {
            val j = journeyFor(arc.id, flags = setOf("friend:${arc.bossId}"))
            val beats = play(j, peace = true)
            assertTrue(beats.any { arc.friendMeeting != null && Voice.caption(Speech.of(arc.friendMeeting)) in said(it) }, "${arc.id}: a friend asks for help")
            assertTrue(beats.none { it is Beat.Choose && it.options.any { o -> o.icon == "hub_fight" } }, "${arc.id}: no fight to pick with a friend")
        }
    }

    private fun lairAsks(arcId: String, flags: Set<String>): Pair<Int, List<String>> {
        val j0 = journeyFor(arcId, flags)
        val j = Journey(j0.seed, strong.give(j0.arc.keyItemId), SkillBook(), WorldMemory(adventures = 1, pages = 1, arcsDone = mapOf(Content.arcs.first().id to 1), lastArc = Content.arcs.first().id, flags = flags), Clock { 0L })
        j.load(j.lairArrival(Content.kingdom.location(j.arc.lairId)))
        var asks = 0
        val heard = mutableListOf<String>()
        var guard = 0
        while (!j.finished && guard++ < 200) {
            val b = j.beat
            if (b is Beat.Ask) asks++
            heard += said(b)
            j.reply(
                when (b) {
                    is Beat.Choose -> Reply.Picked(b.options.indexOfFirst { it.icon == "hub_peace" })
                    is Beat.Ask -> Reply.Solved(1, 0, 0)
                    else -> Reply.Next
                },
            )
        }
        return asks to heard
    }

    @Test
    fun `what the hero learned on the way spares a puzzle, and the boss says so`() {
        val (withoutRhyme, _) = lairAsks("lonely_dragon", emptySet())
        val (withRhyme, heard) = lairAsks("lonely_dragon", setOf("knows_dragon_rhymes"))
        assertEquals(3, withoutRhyme)
        assertEquals(2, withRhyme)
        assertTrue(heard.any { "We can skip the rhymes" in it })
        val (withoutLonely, _) = lairAsks("lonely_dragon", emptySet())
        val (withLonely, _) = lairAsks("lonely_dragon", setOf("knows_dragon_lonely", "knows_dragon_rhymes"))
        assertEquals(3, withoutLonely)
        assertEquals(1, withLonely, "knowing both spares both")
        val (bats, _) = lairAsks("lantern_night", emptySet())
        val (batsKnown, heardBats) = lairAsks("lantern_night", setOf("knows_bats_afraid"))
        assertEquals(3, bats)
        assertEquals(2, batsKnown)
        assertTrue(heardBats.any { "You may skip the bells" in it })
    }

    @Test
    fun `friends made in earlier stories are met again in this one`() {
        // Each new story reads at least two things the world remembered from earlier adventures.
        for (arc in Content.arcs.drop(1)) {
            val remembered = arc.moments.flatMap { it.needs }.filterIsInstance<Cond.Flag>().map { it.name }.toSet()
            assertTrue(remembered.size >= 2, "${arc.id} reads ${remembered}")
        }
        val withBrogan = journeyFor("lonely_dragon", flags = setOf("brogan_braver"))
        assertTrue(play(withBrogan, peace = true).any { "I did not run this time" in said(it) }, "Brogan waits at the gate")
        val without = journeyFor("lonely_dragon")
        assertTrue(play(without, peace = true).none { "I did not run this time" in said(it) })
    }

    @Test
    fun `the new kinds of puzzle are asked on the peaceful way`() {
        val j = journeyFor("lantern_night")
        val boss = Content.monster("bat_king")!!
        val kinds = (j.arc.peaceSteps + j.arc.friendSteps).map { j.peaceChallenge(it, boss) }
        assertTrue(kinds.any { it is BellChallenge } && kinds.any { it is TraceChallenge } && kinds.any { it is PictureChallenge })
        val dragon = journeyFor("lonely_dragon")
        val dragonKinds = dragon.arc.peaceSteps.map { dragon.peaceChallenge(it, Content.monster("big_dragon")!!) }
        assertTrue(dragonKinds.all { it is PictureChallenge })
        assertEquals(listOf("rhyme", "share", "pay"), dragonKinds.map { (it as PictureChallenge).kind })
    }
}
