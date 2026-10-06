package com.littledungeon.engine.rpg

import com.littledungeon.engine.rpg.content.Content
import com.littledungeon.engine.rpg.hero.Attribute
import com.littledungeon.engine.rpg.hero.Hero
import com.littledungeon.engine.rpg.items.ItemKind
import com.littledungeon.engine.rpg.items.Slot
import com.littledungeon.engine.rpg.learn.Skill
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ItemsAndHeroTest {
    @Test
    fun `every item and monster is well formed and the ids are unique`() {
        val items = Content.items
        assertEquals(items.size, items.map { it.id }.toSet().size, "duplicate item ids")
        for (i in items) {
            assertTrue(i.name.isNotBlank() && i.blurb.isNotBlank(), i.id)
            if (i.kind == ItemKind.EQUIPMENT) assertNotNull(i.slot, "${i.id} needs a slot")
            if (i.kind == ItemKind.CONSUMABLE) assertNotNull(i.use, "${i.id} needs a use")
            assertTrue(i.id.matches(Regex("[a-z_]+")), i.id)
        }
        assertTrue(Slot.entries.all { s -> items.count { it.slot == s } >= 4 }, "each slot has choices")
        assertTrue(items.count { it.funny } >= 8 && items.count { it.isGear && !it.funny } >= 8, "serious and funny gear")
        val monsters = Content.monsters
        assertEquals(monsters.size, monsters.map { it.id }.toSet().size)
        for (m in monsters) {
            assertTrue(m.skills.isNotEmpty() && m.hp > 0 && m.attack > 0, m.id)
            m.drops.forEach { assertNotNull(Content.item(it.itemId), "${m.id} drops unknown ${it.itemId}") }
            m.weakness?.let { assertNotNull(Content.item(it), "${m.id} weak to unknown $it") }
        }
        // Tougher monsters take more rounds.
        val tiers = monsters.groupBy { it.tier }.mapValues { (_, v) -> v.map { it.hp }.average() }
        val order = com.littledungeon.engine.rpg.battle.Tier.entries.map { tiers.getValue(it) }
        assertEquals(order.sorted(), order, "hp grows with tier")
    }

    @Test
    fun `coins, items and gear are kept, worn and given back`() {
        var h = Hero().earn(30).give("wooden_sword").give("berry", 2)
        assertEquals(2, h.count("berry"))
        h = h.wear(Content.item("wooden_sword")!!)
        assertEquals("wooden_sword", h.worn[Slot.HAND])
        assertTrue(!h.has("wooden_sword"))
        h = h.give("knight_sword").wear(Content.item("knight_sword")!!)
        assertEquals("knight_sword", h.worn[Slot.HAND])
        assertTrue(h.has("wooden_sword"), "the old sword goes back in the bag")
        h = h.unwear(Slot.HAND)
        assertTrue(h.worn.isEmpty() && h.has("knight_sword"))
        assertEquals(0, h.earn(-100).coins, "coins never go below zero")
        assertEquals(1, h.take("berry").count("berry"))
        assertTrue(!h.take("berry", 2).has("berry"))
    }

    @Test
    fun `the stars do something`() {
        val fresh = Hero()
        val brave = Hero(xp = mapOf(Attribute.COURAGE to 640, Attribute.CLEVERNESS to 640, Attribute.KINDNESS to 640, Attribute.WISDOM to 640))
        assertTrue(brave.maxHp > fresh.maxHp, "courage is health")
        assertTrue(brave.attackWith(Skill.NUMBERS) > fresh.attackWith(Skill.NUMBERS), "cleverness powers number attacks")
        assertEquals(fresh.attackWith(Skill.COLORS), brave.attackWith(Skill.COLORS), "magic was not raised")
        assertTrue(brave.discountPercent > 0 && fresh.discountPercent == 0, "kindness is a discount")
        assertTrue(brave.seesDangers && !fresh.seesDangers, "wisdom shows dangers")
        assertTrue(brave.lootBonusPercent > 0 && brave.healBonus > 0)
        val berry = Content.item("big_potion")!!
        assertTrue(brave.priceOf(berry) < berry.price && fresh.priceOf(berry) == berry.price)
        val geared = fresh.give("iron_helm").give("knight_sword").wear(Content.item("iron_helm")!!).wear(Content.item("knight_sword")!!)
        assertEquals(fresh.defense + 2, geared.defense)
        assertEquals(fresh.attackWith(Skill.NUMBERS) + 4, geared.attackWith(Skill.NUMBERS))
    }

    @Test
    fun `no spoken line spells a sound out`() {
        // A speech model reads "Grrr" as letters. Growls are sound effects; keep them out of lines.
        val bad = Regex("\\b[A-Za-z]*([a-zA-Z])\\1{2,}[A-Za-z]*\\b")
        val lines = Content.monsters.flatMap { listOf(it.taunt, it.beaten, it.wins) }
        for (l in lines) assertTrue(!Regex("\\b[Gg]r+\\b").containsMatchIn(l), "spelled sound in: $l")
        assertTrue(lines.isNotEmpty() && bad.pattern.isNotEmpty())
    }
}
