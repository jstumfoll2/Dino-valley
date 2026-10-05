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
