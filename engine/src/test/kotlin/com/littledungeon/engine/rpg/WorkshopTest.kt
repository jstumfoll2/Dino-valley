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
