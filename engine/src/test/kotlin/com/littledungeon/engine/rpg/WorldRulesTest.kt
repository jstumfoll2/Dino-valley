package com.littledungeon.engine.rpg

import com.littledungeon.engine.rpg.content.Content
import com.littledungeon.engine.rpg.hero.Hero
import com.littledungeon.engine.rpg.learn.SkillBook
import com.littledungeon.engine.rpg.run.Journey
import com.littledungeon.engine.rpg.run.roadMonster
import com.littledungeon.engine.rpg.run.tollCheck
import com.littledungeon.engine.rpg.story.Effect
import com.littledungeon.engine.rpg.world.Terrain
import com.littledungeon.engine.rpg.world.WorldMemory
import com.littledungeon.engine.util.Clock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** The rules of the world that the review found broken: forced fights, ambushes by friends, tolls that were only words. */
class WorldRulesTest {
    private fun journey(seed: Long = 1) = Journey(seed, Hero(), SkillBook(), WorldMemory(), Clock { 0L })

    @Test
    fun `every conversation has a way out that is not a fight, whatever the hero has`() {
        // Bess used to offer only "Fight her" to a hero with fewer than five coins and no kindness.
        for (npc in Content.npcs) for (node in npc.nodes.filter { it.options.isNotEmpty() }) {
            val open = node.options.filter { it.needs.isEmpty() && it.effects.none { e -> e is Effect.Fight } }
            assertTrue(open.isNotEmpty(), "${npc.id}/${node.id}: a hero with nothing to offer is forced to fight")
        }
    }

    @Test
    fun `people with a story of their own are never met as random monsters`() {
        val j = journey()
        val storyFoes = Content.monsters.filter { !it.roams }.map { it.id }.toSet()
        assertTrue(storyFoes.containsAll(listOf("bandit_bess", "sneaky_fox")), "$storyFoes")
        repeat(3000) {
            for (terrain in Terrain.entries) {
                val m = j.roadMonster(terrain, it % 3)
                assertTrue(m.id !in storyFoes, "${m.id} turned up on a ${terrain.word}")
            }
        }
    }

    @Test
    fun `someone who holds the way sends the hero back until paid, befriended or beaten`() {
        val bridge = Content.kingdom.location("old_bridge")
        val grumble = Content.npc("grumble")!!
        assertTrue(grumble.passFlags.isNotEmpty() && Content.npc("bandit_bess")!!.passFlags.isNotEmpty())
        val road = assertNotNull(Content.kingdom.roadBetween("mossbrook", "old_bridge"))

        val j = journey()
        j.cameFrom = "mossbrook"
        j.here = bridge.id
        val sent = j.tollCheck(bridge, grumble)
        assertEquals(1, sent.size, "told that the way is held")
        assertEquals("mossbrook", j.here, "back where they came from")
        assertTrue(j.isClosed(road), "and that road is closed for a few moves")

        for (flag in grumble.passFlags) {
            val other = journey()
            other.cameFrom = "mossbrook"
            other.here = bridge.id
            other.setFlag(flag)
            assertTrue(other.tollCheck(bridge, grumble).isEmpty(), "$flag lets the hero cross")
            assertEquals(bridge.id, other.here)
        }
        // Someone who holds nothing never sends anyone back.
        assertTrue(j.tollCheck(bridge, Content.npc("henrietta_hen")!!).isEmpty())
    }

    @Test
    fun `every pass flag can be set by the person who asks for it`() {
        for (npc in Content.npcs.filter { it.passFlags.isNotEmpty() }) {
            val set = npc.nodes.flatMap { it.effects + it.options.flatMap { o -> o.effects } }.filterIsInstance<Effect.SetFlag>().map { it.name }.toSet()
            for (flag in npc.passFlags) assertTrue(flag in set, "${npc.id}: nothing in their conversation sets $flag")
        }
    }
}
