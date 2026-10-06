package com.littledungeon.engine.rpg.world

/** What the kingdom remembers between adventures, so the story can notice the child. */
data class WorldMemory(
    val adventures: Int = 0,
    /** The endings reached, as "arc_variant_fight" or "arc_variant_peace". */
    val endings: Set<String> = emptySet(),
    /** Things that happened and stay true: "finn_boat", "bess_befriended". See story.Effect. */
    val flags: Set<String> = emptySet(),
    /** How well each person likes the hero (person id to a number; higher is friendlier). */
    val relations: Map<String, Int> = emptyMap(),
    /** Pages of the Great Storybook recovered so far, across adventures. */
    val pages: Int = 0,
    /** Stories finished, by arc id, with how many times. */
    val arcsDone: Map<String, Int> = emptyMap(),
    /** The story played last, so the next one is different. */
    val lastArc: String? = null,
)
