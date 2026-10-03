package com.dinovalley.engine.rpg.world

import com.dinovalley.engine.rpg.items.Obstacle

enum class LocationKind { CAMP, TOWN, DUNGEON, WILD, LAIR }

/**
 * What a road is like. Mountains and rivers are obstacles (one try, a puzzle); forests and swamps
 * have monsters; a plain road is safe. [obstacle] is what can block the way and so which tools help.
 */
enum class Terrain(val word: String, val obstacle: Obstacle?, val monsters: Boolean) {
    ROAD("road", null, false),
    FOREST("forest path", null, true),
    MOUNTAIN("mountain pass", Obstacle.CLIMB, true),
    RIVER("river crossing", Obstacle.CROSS, false),
    SWAMP("swamp trail", Obstacle.DARK, true),
}

/**
 * A place in the kingdom. [x] and [y] are where it sits on the map (0 to 1), which the map
 * painting is made around. Places are data: [residents] are people who live there (ids from the
 * people library), [shops] what they run, [guardian] the mini boss of a dungeon, [theme] picks the
 * look of its rooms and backdrop (art named `art_scene_<theme>`).
 */
data class Location(
    val id: String,
    val name: String,
    val kind: LocationKind,
    val x: Float,
    val y: Float,
    /** One friendly sentence about the place, said when you get there. */
    val blurb: String,
    val theme: String = id,
    val residents: List<String> = emptyList(),
    val shops: List<String> = emptyList(),
    val guardian: String? = null,
    /** In a dungeon: how many puzzle rooms before the guardian. */
    val rooms: Int = 3,
)

/** A road between two places. Roads go both ways. */
data class Road(val a: String, val b: String, val terrain: Terrain, val danger: Int) {
    val id: String get() = if (a < b) "$a~$b" else "$b~$a"
    fun other(from: String): String = if (from == a) b else a
    fun touches(id: String): Boolean = a == id || b == id
}

/** The whole map: where everything is and how the roads join it up. Built once and kept. */
class Kingdom(val name: String, val locations: List<Location>, val roads: List<Road>) {
    private val byId = locations.associateBy { it.id }

    fun location(id: String): Location = byId.getValue(id)

    fun locationOrNull(id: String): Location? = byId[id]

    val camp: Location get() = locations.first { it.kind == LocationKind.CAMP }

    val lairs: List<Location> get() = locations.filter { it.kind == LocationKind.LAIR }

    fun roadsFrom(id: String): List<Road> = roads.filter { it.touches(id) }

    fun roadBetween(a: String, b: String): Road? = roads.firstOrNull { it.touches(a) && it.touches(b) }

    fun neighbors(id: String): List<Location> = roadsFrom(id).map { location(it.other(id)) }

    /** The fewest roads from [from] to [to], not passing through [avoid]; null if there is no way. */
    fun hops(from: String, to: String, avoid: Set<String> = emptySet(), blocked: Set<String> = emptySet()): Int? {
        val seen = mutableMapOf(from to 0)
        val queue = ArrayDeque(listOf(from))
        while (queue.isNotEmpty()) {
            val here = queue.removeFirst()
            if (here == to) return seen.getValue(here)
            for (r in roadsFrom(here)) {
                val next = r.other(here)
                if (r.id in blocked || next in avoid && next != to || next in seen) continue
                seen[next] = seen.getValue(here) + 1
                queue += next
            }
        }
        return null
    }

    /** How many routes from [from] to [to] share no road at all (so closing one road never cuts them off). */
    fun separateRoutes(from: String, to: String): Int {
        val used = mutableSetOf<String>()
        var count = 0
        while (true) {
            // Find a route not using any road already taken by an earlier one.
            val prev = mutableMapOf<String, Road?>(from to null)
            val queue = ArrayDeque(listOf(from))
            while (queue.isNotEmpty() && to !in prev) {
                val here = queue.removeFirst()
                for (r in roadsFrom(here)) {
                    val next = r.other(here)
                    if (r.id in used || next in prev) continue
                    prev[next] = r
                    queue += next
                }
            }
            if (to !in prev) return count
            var at = to
            while (at != from) {
                val r = prev.getValue(at)!!
                used += r.id
                at = r.other(at)
            }
            count++
        }
    }
}
