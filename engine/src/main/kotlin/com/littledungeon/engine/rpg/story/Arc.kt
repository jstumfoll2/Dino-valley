package com.littledungeon.engine.rpg.story

/** One way a story can turn out to be about: its own meeting with the boss and its own endings. */
data class Variant(
    val id: String,
    /** Said at the lair, after the gate opens and before the choice. Tags choose speakers. */
    val meeting: String,
    /** The ending if the hero fought and won. */
    val fightEnd: String,
    /** The ending if the hero made peace. */
    val peaceEnd: String,
)

/** One step on the peaceful way: a puzzle the boss sets, and what is said before and after. */
data class PeaceStep(val kind: String, val intro: String, val yay: String)

/**
 * Something the story shows when the hero first reaches a place: a clue, a sign, a letter. It can
 * need things to be true and can change things (a remembered fact, an item). This is how a story
 * leaves its mark on the world the hero is already walking through.
 */
data class Moment(val at: String, val says: String, val needs: List<Cond> = emptyList(), val effects: List<Effect> = emptyList())

/**
 * A story to play through in one adventure: where it ends, what you need to get there, who you
 * face, and what is said along the way. A story is data: add an Arc to a content pack to add a
 * story. People, places and items it names must exist in packs too. [variants] let one story be
 * told several ways. [minChapter] keeps a story for later in the campaign.
 */
data class Arc(
    val id: String,
    val title: String,
    val lairId: String,
    /** The dungeon that must be explored: its guardian holds the key item. */
    val keyDungeonId: String,
    val keyItemId: String,
    val bossId: String,
    /** Told at camp before the adventure: what has happened, and how it ties into the Storybook. */
    val setup: List<String>,
    /** Said when the hero reaches the lair without the key. */
    val sealed: String,
    /** Said when the guardian gives up the key. */
    val keyFound: String,
    /** Said when the hero opens the way into the lair. */
    val gateOpens: String,
    /** The question at the lair: how will you help? */
    val ask: String,
    val fightLabel: String,
    val peaceLabel: String,
    val peaceSteps: List<PeaceStep>,
    val variants: List<Variant>,
    /** Clues and scenes along the way, shown the first time the hero reaches each place. */
    val moments: List<Moment> = emptyList(),
    val minChapter: Int = 1,
)
