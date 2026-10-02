package com.dinovalley.engine.rpg.world

import com.dinovalley.engine.rpg.learn.Hue
import com.dinovalley.engine.rpg.learn.Side
import com.dinovalley.engine.rpg.learn.Skill
import com.dinovalley.engine.rpg.learn.SkillBook
import kotlin.random.Random

/** Every kind of room. Learning rooms pair a story obstacle with one skill. */
enum class RoomKind(val skill: Skill?, val title: String) {
    GATE(null, "the dungeon gate"),
    RUNE_DOOR(Skill.PATTERNS, "the Rune Door"),
    BRIDGE(Skill.COUNTING, "the Broken Bridge"),
    CRYSTAL_CAVE(Skill.COLORS, "the Crystal Cave"),
    LIBRARY(Skill.LETTERS, "the Spell Library"),
    TUNNEL(Skill.TRACING, "the Dark Tunnel"),
    MIRROR_HALL(Skill.MEMORY, "the Mirror Hall"),
    VAULT(Skill.ADDITION, "the Treasure Vault"),
    GOBLIN_DEN(null, "the Goblin Den"),
    WORKSHOP(Skill.RECIPES, "the Alchemist's Workshop"),
    LAIR(null, "the lair"),
    ;

    companion object {
        val learningRooms = listOf(RUNE_DOOR, BRIDGE, CRYSTAL_CAVE, LIBRARY, TUNNEL, MIRROR_HALL, VAULT)
    }
}

data class Room(val id: Int, val kind: RoomKind, val hue: Hue, val side: Side)

/** One step along the route: a room you always reach, or a fork where you pick a door. */
sealed interface Stop {
    data class Landmark(val room: Room) : Stop
    data class Fork(val doors: List<Room>, val treasureDoor: Int?) : Stop
}

/** A small dungeon, new every adventure: gate, fork, goblin den, fork, workshop, lair. */
data class DungeonMap(val name: String, val stops: List<Stop>)

object DungeonGenerator {
    private val adjectives = listOf("Mossy", "Whispering", "Glittering", "Sleepy", "Rumbling", "Misty", "Crooked", "Twinkling", "Echoing", "Bubbling")
    private val places = listOf("Caverns", "Vaults", "Tunnels", "Halls", "Dungeon", "Grottoes", "Burrows")

    /**
     * Rooms are dealt like cards: skills practiced least recently (and lowest) are more likely,
     * with enough shuffle that two adventures never look alike. One fork hides treasure behind
     * a door; finding it from a clue is the map challenge.
     */
    fun generate(seed: Long, skills: SkillBook): DungeonMap {
        val r = Random(seed)
        val name = "the ${adjectives.random(r)} ${places.random(r)}"
        val dealt = RoomKind.learningRooms
            .sortedBy { kind ->
                val skill = kind.skill!!
                val last = skills.lastPracticed[skill] ?: 0L
                // Older practice and lower level sort first; the random part keeps it lively.
                (last / 60_000L).toDouble() + skills.level(skill) * 30.0 + r.nextDouble() * 120.0
            }
        var nextId = 0
        fun room(kind: RoomKind, hue: Hue, side: Side) = Room(nextId++, kind, hue, side)
        val treasureFork = r.nextInt(2)
        val mapsLevel = skills.level(Skill.MAPS)
        fun fork(index: Int, kinds: List<RoomKind>): Stop.Fork {
            val wide = index == treasureFork && mapsLevel >= 2
            val chosen = if (wide) kinds.take(3) else kinds.take(2)
            val sides = if (chosen.size == 3) listOf(Side.LEFT, Side.MIDDLE, Side.RIGHT) else listOf(Side.LEFT, Side.RIGHT)
            val hues = Hue.entries.shuffled(r)
            val doors = chosen.mapIndexed { i, k -> room(k, hues[i], sides[i]) }
            return Stop.Fork(doors, if (index == treasureFork) r.nextInt(doors.size) else null)
        }
        val firstKinds = dealt.take(3)
        val secondKinds = dealt.drop(3).take(3)
        return DungeonMap(
            name,
            listOf(
                Stop.Landmark(room(RoomKind.GATE, Hue.ORANGE, Side.MIDDLE)),
                fork(0, firstKinds),
                Stop.Landmark(room(RoomKind.GOBLIN_DEN, Hue.GREEN, Side.MIDDLE)),
                fork(1, secondKinds),
                Stop.Landmark(room(RoomKind.WORKSHOP, Hue.PURPLE, Side.MIDDLE)),
                Stop.Landmark(room(RoomKind.LAIR, Hue.RED, Side.MIDDLE)),
            ),
        )
    }
}
