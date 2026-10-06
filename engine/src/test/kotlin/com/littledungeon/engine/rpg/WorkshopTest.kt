package com.littledungeon.engine.rpg

import com.littledungeon.engine.rpg.hero.Hero
import com.littledungeon.engine.rpg.learn.RecipeChallenge
import com.littledungeon.engine.rpg.learn.SkillBook
import com.littledungeon.engine.rpg.run.Beat
import com.littledungeon.engine.rpg.run.Journey
import com.littledungeon.engine.rpg.run.roomPuzzle
import com.littledungeon.engine.rpg.run.roomScene
import com.littledungeon.engine.rpg.world.RoomKind
import com.littledungeon.engine.rpg.world.WorldMemory
import com.littledungeon.engine.util.Clock
import kotlin.test.Test
import kotlin.test.assertTrue

class WorkshopTest {
    @Test
    fun `dungeons can deal a potion workshop, and it asks for a recipe`() {
        assertTrue(RoomKind.WORKSHOP in RoomKind.learningRooms)
        val j = Journey(3, Hero(), SkillBook(), WorldMemory(), Clock { 0L })
        val steps = j.roomPuzzle(RoomKind.WORKSHOP, j.roomScene(RoomKind.WORKSHOP), solved = { emptyList() }, failed = { emptyList() })
        val ask = steps.map { it.beat }.filterIsInstance<Beat.Ask>().single()
        assertTrue(ask.challenge is RecipeChallenge)
        assertTrue(ask.oneTry)
    }
}

class TunnelTest {
    @Test
    fun `writing starts with lines and shapes, then letters from straight to twisty`() {
        val shapes = (1L..60L).map { com.littledungeon.engine.rpg.run.tunnelTrace(1, it, "x").shape }.toSet() + (1L..60L).map { com.littledungeon.engine.rpg.run.tunnelTrace(2, it, "x").shape }.toSet()
        assertTrue(shapes.all { it != com.littledungeon.engine.rpg.learn.TraceShape.LETTER }, "levels 1 and 2 are shapes: $shapes")
        assertTrue(com.littledungeon.engine.rpg.learn.TraceShape.LINE in shapes && com.littledungeon.engine.rpg.learn.TraceShape.ZIGZAG in shapes)
        assertTrue((1L..30L).all { com.littledungeon.engine.rpg.run.tunnelTrace(3, it, "x").shape == com.littledungeon.engine.rpg.learn.TraceShape.LETTER })
        assertTrue((1L..30L).all { com.littledungeon.engine.rpg.run.tunnelTrace(3, it, "x").glyph.let { g -> g != null && g in "LTIHEF147" } })
    }
}

class LampWireTest {
    @Test
    fun `a wire joins lamp to lamp, longer with the level, and the tunnel deals it only at the first two levels`() {
        val strokesByLevel = listOf(1, 2, 2, 3, 3)
        for (level in 1..5) for (seed in 1L..100L) {
            val c = com.littledungeon.engine.rpg.learn.ChallengeFactory.wire(level, seed)
            assertTrue(c.shape == com.littledungeon.engine.rpg.learn.TraceShape.WIRE)
            assertTrue(c.strokes.size == strokesByLevel[level - 1], "level $level has ${c.strokes.size} wires")
            assertTrue(c.lamps.size == c.strokes.size + 1, "a lamp at every end")
            c.strokes.zipWithNext().forEach { (a, b) -> assertTrue(a.last() == b.first(), "each wire starts where the last one ended") }
            assertTrue(c.strokes.flatten().all { it.x in 0f..1f && it.y in 0f..1f })
            assertTrue(c.lamps.zipWithNext().all { (a, b) -> kotlin.math.hypot(a.x - b.x, a.y - b.y) > 0.2f }, "the lamps are well apart")
            assertTrue(c.glyph == null)
        }
        val tunnel = { level: Int -> (1L..60L).map { com.littledungeon.engine.rpg.run.tunnelTrace(level, it, "x").shape } }
        assertTrue(com.littledungeon.engine.rpg.learn.TraceShape.WIRE in tunnel(1) && com.littledungeon.engine.rpg.learn.TraceShape.WIRE in tunnel(2))
        assertTrue(listOf(3, 4, 5).all { com.littledungeon.engine.rpg.learn.TraceShape.WIRE !in tunnel(it) })
    }
}

class BellSongTest {
    @Test
    fun `dungeons can deal a bell tower, and it asks for a song`() {
        assertTrue(RoomKind.BELFRY in RoomKind.learningRooms)
        val j = Journey(5, Hero(), SkillBook(), WorldMemory(), Clock { 0L })
        val steps = j.roomPuzzle(RoomKind.BELFRY, j.roomScene(RoomKind.BELFRY), solved = { emptyList() }, failed = { emptyList() })
        val ask = steps.map { it.beat }.filterIsInstance<Beat.Ask>().single()
        assertTrue(ask.challenge is com.littledungeon.engine.rpg.learn.BellChallenge)
        assertTrue(ask.oneTry)
    }

    @Test
    fun `songs grow with the level, always use at least two bells, and are judged tap by tap`() {
        val lengths = (1..5).map { level -> level to (1L..200L).map { com.littledungeon.engine.rpg.learn.ChallengeFactory.bells(level, it) } }
        for ((level, songs) in lengths) {
            for (c in songs) {
                assertTrue(c.song.toSet().size >= 2, "level $level: $c")
                assertTrue(c.song.all { it in 0 until c.bells })
                // The song is right as it is played, and a wrong bell is wrong wherever it comes.
                c.song.forEachIndexed { i, bell ->
                    assertTrue(c.expects(i, bell))
                    assertTrue(!c.expects(i, (bell + 1) % c.bells))
                }
                assertTrue(!c.expects(c.song.size, 0), "nothing is expected after the last bell")
                if (level < 3) assertTrue(c.song.zipWithNext().none { (a, b) -> a == b }, "no bell twice in a row before level 3: ${c.song}")
            }
        }
        assertTrue(lengths.map { (_, s) -> s.first().song.size } == listOf(2, 3, 3, 4, 5))
        assertTrue(lengths.map { (_, s) -> s.first().gapMillis }.zipWithNext().all { (a, b) -> b <= a }, "songs never slow down as the level rises")
        // A song is not always the same: every bell is used somewhere in a level.
        assertTrue((0 until 3).all { b -> lengths[0].second.any { b in it.song } })
        // Level 3 and up does repeat a bell sometimes.
        assertTrue(lengths[2].second.any { c -> c.song.zipWithNext().any { (a, b) -> a == b } })
    }
}

class EveryRoomTest {
    @Test
    fun `every kind of room deals a puzzle of its own skill, with one try, on a backdrop that exists`() {
        for (kind in RoomKind.learningRooms) {
            val j = Journey(11, Hero(), SkillBook(), WorldMemory(), Clock { 0L })
            val steps = j.roomPuzzle(kind, j.roomScene(kind), solved = { emptyList() }, failed = { emptyList() })
            val ask = steps.map { it.beat }.filterIsInstance<Beat.Ask>().single()
            assertTrue(ask.challenge.skill == kind.skill, "$kind deals ${ask.challenge.skill}")
            assertTrue(ask.oneTry, "$kind is one try")
        }
        // The rooms the newer skills are dealt in: every skill with a room is reached by dungeons.
        val skillsWithRooms = RoomKind.learningRooms.mapNotNull { it.skill }.toSet()
        for (s in listOf(com.littledungeon.engine.rpg.learn.Skill.MONEY, com.littledungeon.engine.rpg.learn.Skill.RHYMES, com.littledungeon.engine.rpg.learn.Skill.SHARING, com.littledungeon.engine.rpg.learn.Skill.MAPS, com.littledungeon.engine.rpg.learn.Skill.LISTENING, com.littledungeon.engine.rpg.learn.Skill.RECIPES)) {
            assertTrue(s in skillsWithRooms, "$s has a room")
        }
    }
}
