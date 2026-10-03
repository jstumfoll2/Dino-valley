package com.dinovalley.engine.rpg.content

import com.dinovalley.engine.rpg.battle.CoreMonsters
import com.dinovalley.engine.rpg.battle.Monster
import com.dinovalley.engine.rpg.items.CoreItems
import com.dinovalley.engine.rpg.items.Item
import com.dinovalley.engine.rpg.world.CoreKingdom
import com.dinovalley.engine.rpg.world.Location
import com.dinovalley.engine.rpg.world.Road

/**
 * A bundle of content: items, monsters, and (in later slices) people, places, storylets and story
 * arcs. The game is built to grow by adding packs, not by changing the engine: a new pack lists
 * what it adds, ships the art it names, and is added to [Content.packs].
 */
interface ContentPack {
    val id: String
    val items: List<Item> get() = emptyList()
    val monsters: List<Monster> get() = emptyList()
    val locations: List<Location> get() = emptyList()
    val roads: List<Road> get() = emptyList()
}

/** The first pack: the starter items and monsters. */
object CorePack : ContentPack {
    override val id = "core"
    override val items = CoreItems.all
    override val monsters = CoreMonsters.all
    override val locations = CoreKingdom.locations
    override val roads = CoreKingdom.roads
}

/** Everything the game knows about, from every pack. */
object Content {
    var packs: List<ContentPack> = listOf(CorePack)

    val items: List<Item> get() = packs.flatMap { it.items }
    val monsters: List<Monster> get() = packs.flatMap { it.monsters }

    val locations: List<Location> get() = packs.flatMap { it.locations }
    val roads: List<Road> get() = packs.flatMap { it.roads }

    /** The map, put together from every pack's places and roads. */
    val kingdom: com.dinovalley.engine.rpg.world.Kingdom get() = com.dinovalley.engine.rpg.world.Kingdom("Whisperwood", locations, roads)

    fun item(id: String): Item? = items.firstOrNull { it.id == id }
    fun monster(id: String): Monster? = monsters.firstOrNull { it.id == id }
}
