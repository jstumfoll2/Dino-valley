package com.littledungeon.engine.rpg

import com.littledungeon.engine.rpg.content.Content
import com.littledungeon.engine.rpg.content.CorePack
import com.littledungeon.engine.rpg.content.Registry
import com.littledungeon.engine.rpg.hero.Hero
import com.littledungeon.engine.rpg.learn.SkillBook
import com.littledungeon.engine.rpg.run.Journey
import com.littledungeon.engine.rpg.run.dealtRooms
import com.littledungeon.engine.rpg.world.WorldMemory
import com.littledungeon.engine.util.Clock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ContentRegistryTest {
    @Test
    fun `things are found by id, and the same list is served every time`() {
        assertSame(Content.items, Content.items, "lists are built once, not on every call")
        assertSame(Content.kingdom, Content.kingdom, "the map is built once")
        for (item in Content.items) assertSame(item, Content.item(item.id))
        for (m in Content.monsters) assertSame(m, Content.monster(m.id))
        for (n in Content.npcs) assertSame(n, Content.npc(n.id))
        for (a in Content.arcs) assertSame(a, Content.arc(a.id))
        assertNull(Content.item("no_such_item"))
    }

    @Test
    fun `an id used twice is a mistake that fails when the content is put together`() {
        val e = assertFailsWith<IllegalArgumentException> { Registry(listOf(CorePack, CorePack)) }
        assertTrue("share the id" in e.message.orEmpty(), e.message)
    }

    @Test
    fun `dungeon plans belong to their journey, not to the whole app`() {
        fun plan(seed: Long): List<String> {
            val j = Journey(seed, Hero(), SkillBook(), WorldMemory(), Clock { 0L })
            // Walk to the key dungeon by asking for its plan directly.
            val dungeon = j.kingdom.locations.first { it.id == j.arc.keyDungeonId }
            return dealtRooms(j, dungeon).map { it.name }
        }
        assertEquals(plan(5), plan(5), "same seed, same rooms, however many journeys came before")
        assertTrue((1L..30L).map { plan(it) }.toSet().size > 1, "different seeds deal different rooms")
    }
}
