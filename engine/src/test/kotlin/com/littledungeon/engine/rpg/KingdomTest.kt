package com.littledungeon.engine.rpg

import com.littledungeon.engine.rpg.content.Content
import com.littledungeon.engine.rpg.world.LocationKind
import com.littledungeon.engine.rpg.world.Terrain
import kotlin.math.hypot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class KingdomTest {
    private val k = Content.kingdom

    @Test
    fun `the map is a whole, tidy world`() {
        assertEquals(k.locations.size, k.locations.map { it.id }.toSet().size)
        assertEquals(k.locations.size, k.locations.map { it.name }.toSet().size)
        assertEquals(1, k.locations.count { it.kind == LocationKind.CAMP })
        assertTrue(k.lairs.size >= 2, "different stories can have different lairs")
        for (l in k.locations) assertTrue(l.x in 0.04f..0.96f && l.y in 0.06f..0.94f, "${l.id} off the map")
        for (a in k.locations) for (b in k.locations) if (a.id < b.id) {
            assertTrue(hypot(a.x - b.x, a.y - b.y) > 0.07f, "${a.id} and ${b.id} are too close to label")
        }
        for (r in k.roads) {
            assertNotNull(k.locationOrNull(r.a), r.a); assertNotNull(k.locationOrNull(r.b), r.b)
            assertTrue(r.a != r.b)
        }
        assertEquals(k.roads.size, k.roads.map { it.id }.toSet().size, "no road listed twice")
        for (l in k.locations) assertTrue(k.roadsFrom(l.id).isNotEmpty(), "${l.id} has no road")
    }

    @Test
    fun `every lair and dungeon can be reached, with more than one way to each lair`() {
        val lairIds = k.lairs.map { it.id }.toSet()
        for (l in k.locations) assertNotNull(k.hops("camp", l.id), "${l.id} unreachable")
        for (d in k.locations.filter { it.kind == LocationKind.DUNGEON }) {
            assertNotNull(k.hops("camp", d.id, avoid = lairIds), "${d.id} needs to be reachable without crossing a lair")
            assertNotNull(d.guardian, "${d.id} needs a guardian")
            assertNotNull(Content.monster(d.guardian!!), "${d.id} guardian ${d.guardian} unknown")
        }
        for (lair in k.lairs) {
            assertTrue(k.separateRoutes("camp", lair.id) >= 2, "${lair.id} needs two roads that share nothing")
            assertTrue(k.hops("camp", lair.id)!! >= 5, "the lair is a journey away")
        }
    }

    @Test
    fun `roads differ so the choice matters`() {
        assertTrue(Terrain.entries.all { t -> k.roads.any { it.terrain == t } }, "every terrain is used")
        val towns = k.locations.filter { it.kind == LocationKind.TOWN }
        assertTrue(towns.size >= 4 && towns.all { it.shops.isNotEmpty() && it.residents.isNotEmpty() })
        // Closing any single road never cuts a lair off.
        for (lair in k.lairs) for (r in k.roads) {
            assertNotNull(k.hops("camp", lair.id, blocked = setOf(r.id)), "closing ${r.id} cuts off ${lair.id}")
        }
    }
}
