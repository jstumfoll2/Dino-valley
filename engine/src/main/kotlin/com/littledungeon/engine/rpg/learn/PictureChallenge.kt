package com.littledungeon.engine.rpg.learn

import com.littledungeon.engine.model.Speech

/** A painted picture (`art_<art>`) shown [count] times in a row. */
data class Shown(val art: String, val count: Int = 1)

/** An answer card: some pictures (a handful of coins, a word's picture) or, with none, [label] drawn big (a number). */
data class Card(val pictures: List<Shown>, val label: String)

/**
 * A challenge answered by tapping one of several picture cards, for the skills that are about pictures and words and not
 * only numbers: rhymes (which picture ends like this one?), money (which coins pay for it?), sharing (how many does each get?).
 * [scene] is what is shown above the cards; [because] is said, with the right card glowing, when the last try is missed.
 */
data class PictureChallenge(
    override val skill: Skill,
    override val level: Int,
    override val seed: Long,
    override val prompt: List<Speech>,
    override val kind: String,
    val scene: List<Shown>,
    val options: List<Card>,
    override val answer: Int,
    val because: List<Speech>,
) : PickOne {
    override val optionCount get() = options.size
}
