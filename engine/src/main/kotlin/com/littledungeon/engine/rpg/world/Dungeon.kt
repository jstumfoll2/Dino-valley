package com.littledungeon.engine.rpg.world

import com.littledungeon.engine.rpg.learn.Skill

/**
 * The kinds of puzzle room a dungeon can hold. Each pairs a story obstacle with one skill, so the
 * rooms a dungeon deals are the skills the child has practiced least (see `Journey.plan`).
 */
enum class RoomKind(val skill: Skill?) {
    RUNE_DOOR(Skill.PATTERNS),
    BRIDGE(Skill.COUNTING),
    CRYSTAL_CAVE(Skill.COLORS),
    LIBRARY(Skill.LETTERS),
    TUNNEL(Skill.TRACING),
    MIRROR_HALL(Skill.MEMORY),
    VAULT(Skill.ADDITION),
    STOREROOM(Skill.SORTING),
    POND(Skill.SKIP_COUNTING),
    MOSAIC_HALL(Skill.PUZZLES),

    /** A potion workshop: read a recipe, then brew it (see `ChallengeFactory.recipe`). */
    WORKSHOP(Skill.RECIPES),

    /** A bell tower: listen to a song on three bells, then play it back (see `ChallengeFactory.bells`). */
    BELFRY(Skill.LISTENING),

    /** A market stall: which coins pay for the pie (see `PictureFactory.money`). */
    MARKET_STALL(Skill.MONEY),

    /** A rope bridge whose planks are pictures: find the one that rhymes (see `PictureFactory.rhyme`). */
    RHYME_BRIDGE(Skill.RHYMES),

    /** A cave of sleeping bats and a heap of berries: share them out fairly (see `PictureFactory.share`). */
    BAT_CAVE(Skill.SHARING),

    /** Hoot's map table: follow the route to the treasure (see `PictureFactory.map`). */
    MAP_ROOM(Skill.MAPS),
    ;

    companion object {
        /** The rooms a dungeon deals from. */
        val learningRooms = listOf(RUNE_DOOR, BRIDGE, CRYSTAL_CAVE, LIBRARY, TUNNEL, MIRROR_HALL, VAULT, STOREROOM, POND, MOSAIC_HALL, WORKSHOP, BELFRY, MARKET_STALL, RHYME_BRIDGE, BAT_CAVE, MAP_ROOM)
    }
}
