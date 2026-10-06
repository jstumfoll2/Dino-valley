package com.littledungeon.engine.rpg.battle

import com.littledungeon.engine.model.Who
import com.littledungeon.engine.rpg.learn.Skill

/** How tough and important a monster is. Tougher monsters mean more rounds of fighting. */
enum class Tier(val word: String) {
    MINION("little"),
    ELITE("tough"),
    MINI_BOSS("guardian"),
    BOSS("boss"),
}

/** A chance (in percent) that a defeated monster leaves this item behind. */
data class Drop(val itemId: String, val percent: Int)

/**
 * Anything that can be fought. Monsters are data: a new one needs an entry in a content pack and
 * painted art named `art_monster_<id>_*` (same layers as every character); no other code changes.
 * Its [skills] are the kinds of puzzle it fights with. [weakness] is an item that hurts it badly,
 * which is how loot found earlier matters in a boss fight. A [befriendable] monster can be won
 * over with a Friendship Cookie instead of beaten.
 */
data class Monster(
    val id: String,
    val name: String,
    val tier: Tier,
    val hp: Int,
    val attack: Int,
    val skills: List<Skill>,
    val who: Who,
    /** Said when the fight starts; tags like <critter> choose who speaks. */
    val taunt: String,
    /** Said when the monster is beaten. */
    val beaten: String,
    /** Said when the hero is knocked out. */
    val wins: String,
    val coins: IntRange = 2..5,
    val drops: List<Drop> = emptyList(),
    val befriendable: Boolean = false,
    val weakness: String? = null,
    /** The roads it lurks on (empty: anywhere). */
    val habitat: Set<com.littledungeon.engine.rpg.world.Terrain> = emptySet(),
    /**
     * False for a person with a story of their own (Bess, Rascal): they are only fought where their own conversation
     * puts the hero in front of them, never met as a random monster on a road, least of all after becoming friends.
     */
    val roams: Boolean = true,
) {
    /** The picture name; the app looks up `art_monster_<id>`. */
    val art: String get() = "monster_$id"
}
