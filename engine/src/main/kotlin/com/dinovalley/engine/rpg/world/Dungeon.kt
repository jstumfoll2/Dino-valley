package com.dinovalley.engine.rpg.world

import com.dinovalley.engine.rpg.learn.Hue
import com.dinovalley.engine.rpg.learn.Side
import com.dinovalley.engine.rpg.learn.Skill
import com.dinovalley.engine.rpg.learn.SkillBook
import kotlin.random.Random

/**
 * Every kind of room. Learning rooms pair a story obstacle with one skill; [activity] is what
 * the narrator calls it at a fork, so the child can pick the kind of puzzle they want.
 */
enum class RoomKind(val skill: Skill?, val title: String, val activity: String) {
    GATE(null, "the dungeon gate", ""),
    RUNE_DOOR(Skill.PATTERNS, "the Rune Door", "a pattern puzzle"),
    BRIDGE(Skill.COUNTING, "the Broken Bridge", "a counting game"),
    CRYSTAL_CAVE(Skill.COLORS, "the Crystal Cave", "a color game"),
    LIBRARY(Skill.LETTERS, "the Spell Library", "a letter game"),
    TUNNEL(Skill.TRACING, "the Dark Tunnel", "a writing game"),
    MIRROR_HALL(Skill.MEMORY, "the Mirror Hall", "a memory game"),
    VAULT(Skill.ADDITION, "the Treasure Vault", "an adding game"),
    STOREROOM(Skill.SORTING, "the Goblins' Storeroom", "a sorting game"),
    POND(Skill.SKIP_COUNTING, "the Frog Pond", "a frog counting game"),
    MOSAIC_HALL(Skill.PUZZLES, "the Mosaic Hall", "a jigsaw puzzle"),
    GOBLIN_DEN(null, "the Goblin Den", ""),
    WORKSHOP(Skill.RECIPES, "the Alchemist's Workshop", ""),
    LAIR(null, "the lair", ""),
    ;

    companion object {
        val learningRooms = listOf(RUNE_DOOR, BRIDGE, CRYSTAL_CAVE, LIBRARY, TUNNEL, MIRROR_HALL, VAULT, STOREROOM, POND, MOSAIC_HALL)
    }
}

data class Room(val id: Int, val kind: RoomKind, val hue: Hue, val side: Side)

/** One step along the route: a room you always reach, or a fork where you pick a door. */
sealed interface Stop {
    data class Landmark(val room: Room) : Stop
    data class Fork(val doors: List<Room>, val treasureDoor: Int?) : Stop
}

/** A small dungeon, new every adventure: gate, fork, fork, goblin den, fork, workshop, lair. */
data class DungeonMap(val name: String, val stops: List<Stop>)

object DungeonGenerator {
    /** A dozen names, so every one can be recorded by the narrator ahead of time. */
    private val names = listOf(
        "the Mossy Caverns", "the Whispering Vaults", "the Glittering Tunnels", "the Sleepy Halls", "the Rumbling Dungeon",
        "the Misty Grottoes", "the Crooked Burrows", "the Twinkling Caverns", "the Echoing Halls", "the Bubbling Grottoes",
        "the Mossy Burrows", "the Glittering Halls",
    )

    /**
     * Rooms are dealt like cards: skills practiced least recently (and lowest) are more likely,
     * with enough shuffle that two adventures never look alike. Every fork hides treasure behind
     * one door; finding it from a clue is the map challenge.
     */
    fun generate(seed: Long, skills: SkillBook): DungeonMap {
        val r = Random(seed)
        val name = names.random(r)
        val dealt = RoomKind.learningRooms
            .sortedBy { kind ->
                val skill = kind.skill!!
                val last = skills.lastPracticed[skill] ?: 0L
                // Older practice and lower level sort first; the random part keeps it lively.
                (last / 60_000L).toDouble() + skills.level(skill) * 30.0 + r.nextDouble() * 120.0
            }
        var nextId = 0
        fun room(kind: RoomKind, hue: Hue, side: Side) = Room(nextId++, kind, hue, side)
        val wideFork = r.nextInt(3)
        val mapsLevel = skills.level(Skill.MAPS)
        fun fork(index: Int, kinds: List<RoomKind>): Stop.Fork {
            val wide = index == wideFork && mapsLevel >= 2
            val chosen = if (wide) kinds.take(3) else kinds.take(2)
            val sides = if (chosen.size == 3) listOf(Side.LEFT, Side.MIDDLE, Side.RIGHT) else listOf(Side.LEFT, Side.RIGHT)
            val hues = Hue.entries.shuffled(r)
            val doors = chosen.mapIndexed { i, k -> room(k, hues[i], sides[i]) }
            return Stop.Fork(doors, r.nextInt(doors.size))
        }
        // Three forks, each showing two doors (three at the wide one) from its own three rooms.
        val firstKinds = dealt.subList(0, 3)
        val secondKinds = dealt.subList(3, 6)
        val thirdKinds = dealt.subList(6, 9)
        return DungeonMap(
            name,
            listOf(
                Stop.Landmark(room(RoomKind.GATE, Hue.ORANGE, Side.MIDDLE)),
                fork(0, firstKinds),
                fork(1, secondKinds),
                Stop.Landmark(room(RoomKind.GOBLIN_DEN, Hue.GREEN, Side.MIDDLE)),
                fork(2, thirdKinds),
                Stop.Landmark(room(RoomKind.WORKSHOP, Hue.PURPLE, Side.MIDDLE)),
                Stop.Landmark(room(RoomKind.LAIR, Hue.RED, Side.MIDDLE)),
            ),
        )
    }
}
