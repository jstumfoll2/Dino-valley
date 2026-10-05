package com.littledungeon.engine.rpg.content

import com.littledungeon.engine.rpg.battle.CoreMonsters
import com.littledungeon.engine.rpg.battle.Monster
import com.littledungeon.engine.rpg.items.CoreItems
import com.littledungeon.engine.rpg.items.Item
import com.littledungeon.engine.rpg.story.Arc
import com.littledungeon.engine.rpg.story.CoreArcs
import com.littledungeon.engine.rpg.story.CoreNpcs
import com.littledungeon.engine.rpg.story.CoreShops
import com.littledungeon.engine.rpg.story.Npc
import com.littledungeon.engine.rpg.story.ShopDef
import com.littledungeon.engine.rpg.world.CoreKingdom
import com.littledungeon.engine.rpg.world.Kingdom
import com.littledungeon.engine.rpg.world.Location
import com.littledungeon.engine.rpg.world.Road

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
    val arcs: List<Arc> get() = emptyList()
    val npcs: List<Npc> get() = emptyList()
    val shops: List<ShopDef> get() = emptyList()

    /** A remembered fact that keeps roads open in every later adventure (the troll's boat, the bandit's path). */
    val flagRoads: Map<String, List<String>> get() = emptyMap()
}

/** The first pack: the starter items and monsters. */
object CorePack : ContentPack {
    override val id = "core"
    override val items = CoreItems.all
    override val monsters = CoreMonsters.all
    override val locations = CoreKingdom.locations
    override val roads = CoreKingdom.roads
    override val arcs = CoreArcs.all
    override val npcs = CoreNpcs.all
    override val shops = CoreShops.all
    override val flagRoads = CoreKingdom.flagRoads
}

/**
 * Everything the game knows about, from every pack, put together once: lists for tests and tools to
 * walk, maps by id for the game to look things up (which it does constantly, for every item a hero
 * wears), and the map of the kingdom. A duplicate id in any list is a mistake and fails right here.
 */
class Registry(val packs: List<ContentPack>) {
    val items: List<Item> = packs.flatMap { it.items }
    val monsters: List<Monster> = packs.flatMap { it.monsters }
    val locations: List<Location> = packs.flatMap { it.locations }
    val roads: List<Road> = packs.flatMap { it.roads }
    val arcs: List<Arc> = packs.flatMap { it.arcs }
    val npcs: List<Npc> = packs.flatMap { it.npcs }
    val shops: List<ShopDef> = packs.flatMap { it.shops }
    val flagRoads: Map<String, List<String>> =
        packs.flatMap { it.flagRoads.entries }.groupBy({ it.key }, { it.value }).mapValues { (_, v) -> v.flatten() }

    val kingdom: Kingdom = Kingdom("Whisperwood", locations, roads)

    private val itemById = unique("item", items) { it.id }
    private val monsterById = unique("monster", monsters) { it.id }
    private val npcById = unique("person", npcs) { it.id }
    private val shopById = unique("shop", shops) { it.id }
    private val arcById = unique("story", arcs) { it.id }

    fun item(id: String): Item? = itemById[id]
    fun monster(id: String): Monster? = monsterById[id]
    fun npc(id: String): Npc? = npcById[id]
    fun shop(id: String): ShopDef? = shopById[id]
    fun arc(id: String): Arc? = arcById[id]

    private fun <T> unique(what: String, list: List<T>, id: (T) -> String): Map<String, T> {
        val map = LinkedHashMap<String, T>()
        for (x in list) require(map.put(id(x), x) == null) { "two ${what}s share the id ${id(x)}" }
        return map
    }
}

/** The game's content. Swapping [packs] (tests, tools) rebuilds the registry. */
object Content {
    @Volatile
    private var registry = Registry(listOf(CorePack))

    var packs: List<ContentPack>
        get() = registry.packs
        set(value) {
            registry = Registry(value)
        }

    val items: List<Item> get() = registry.items
    val monsters: List<Monster> get() = registry.monsters
    val locations: List<Location> get() = registry.locations
    val roads: List<Road> get() = registry.roads
    val arcs: List<Arc> get() = registry.arcs
    val npcs: List<Npc> get() = registry.npcs
    val shops: List<ShopDef> get() = registry.shops
    val flagRoads: Map<String, List<String>> get() = registry.flagRoads

    /** The map of Whisperwood, put together from every pack's places and roads. */
    val kingdom: Kingdom get() = registry.kingdom

    fun npc(id: String): Npc? = registry.npc(id)
    fun shop(id: String): ShopDef? = registry.shop(id)
    fun arc(id: String): Arc? = registry.arc(id)
    fun item(id: String): Item? = registry.item(id)
    fun monster(id: String): Monster? = registry.monster(id)
}
