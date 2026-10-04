package com.littledungeon.engine.rpg.story

import com.littledungeon.engine.model.Who
import com.littledungeon.engine.rpg.hero.Attribute

/**
 * Something that must be true for an option, a start, or an event to apply. Conditions read the
 * hero, the bag and what the world remembers, so the same person can say different things to
 * different heroes on different days.
 */
sealed interface Cond {
    data class HasItem(val itemId: String, val n: Int = 1) : Cond
    data class Coins(val atLeast: Int) : Cond
    data class Stat(val attribute: Attribute, val atLeast: Int) : Cond
    data class Flag(val name: String) : Cond
    data class NoFlag(val name: String) : Cond
    data class Friend(val npcId: String, val atLeast: Int = 1) : Cond
    data class Chapter(val atLeast: Int) : Cond
    data class ArcIs(val arcId: String) : Cond
    data class Not(val cond: Cond) : Cond
}

/**
 * What a choice does. Flags are what the world remembers: a name starting with "run:" lasts for this
 * adventure only; any other name is remembered between adventures. [Relation] is how well a person
 * likes the hero (it persists). [OpenRoad] makes a hard road easy for the rest of the adventure.
 */
sealed interface Effect {
    data class Give(val itemId: String, val n: Int = 1) : Effect
    data class Take(val itemId: String, val n: Int = 1) : Effect
    data class Pay(val coins: Int) : Effect
    data class Earn(val coins: Int) : Effect
    data class SetFlag(val name: String) : Effect
    data class ClearFlag(val name: String) : Effect
    data class Relation(val npcId: String, val delta: Int) : Effect
    data class Heal(val hp: Int) : Effect
    data class Stars(val attribute: Attribute, val n: Int) : Effect
    data class OpenRoad(val roadId: String) : Effect
    data class CloseRoad(val roadId: String) : Effect

    /**
     * A fight; the conversation goes on at [win] if the hero wins (or ends if null). A hero who is knocked
     * out is carried to the nearest safe place instead, as in any other fight.
     */
    data class Fight(val monsterId: String, val win: String?) : Effect

    /** A one-try puzzle; to [win] or [lose]. [kind] is an Obstacle name (CLIMB, CROSS, DARK, LOCK, RIDDLE). */
    data class Puzzle(val kind: String, val win: String?, val lose: String?) : Effect

    /** Opens a shop. */
    data class Shop(val shopId: String) : Effect

    /** A night's rest at an inn: heals fully for [cost] coins. */
    data class Rest(val cost: Int) : Effect

    /** The baby dragon's page: the story advances. */
    data object Page : Effect
}

/** One thing the hero can say or do. It shows a picture ([icon], a painted icon name) and is read aloud. */
data class Option(
    val said: String,
    val icon: String,
    val next: String? = null,
    val effects: List<Effect> = emptyList(),
    val needs: List<Cond> = emptyList(),
)

/**
 * One moment of a conversation. [says] is spoken by the person (tags like <narrator> or <pet> change
 * the speaker for a bit); [options] are the replies (at most three, so each has a big picture).
 * A node with no options simply ends the conversation after it is said.
 */
data class DialogNode(
    val id: String,
    val says: String,
    val options: List<Option> = emptyList(),
    /** Effects that happen as soon as this node is reached (rewards for finishing a thread). */
    val effects: List<Effect> = emptyList(),
)

/** Where a conversation begins for the first matching start. */
data class Start(val node: String, val needs: List<Cond> = emptyList()) {
    constructor(node: String, vararg needs: Cond) : this(node, needs.toList())
}

/**
 * A person with a life of their own. Everything about them is data: who they are, what they want,
 * how they talk, and what the hero's choices do for them and for the world. Add a person by adding
 * a Npc to a content pack and painted art named `art_npc_<id>_*` (same layers as every character).
 */
data class Npc(
    val id: String,
    val name: String,
    val title: String,
    val who: Who,
    /** Their story in a few sentences: what they have been through and what they want now. */
    val backstory: String,
    /** Said the first time you meet. */
    val intro: String,
    val starts: List<Start>,
    val nodes: List<DialogNode>,
    /** True for shopkeepers and the like, who run a shop of this id. */
    val shop: String? = null,
) {
    val art: String get() = "npc_$id"
    fun node(id: String): DialogNode = nodes.first { it.id == id }
    fun nodeOrNull(id: String): DialogNode? = nodes.firstOrNull { it.id == id }
}

/** A shop: a keeper and a stock of item ids. */
data class ShopDef(val id: String, val name: String, val keeper: String, val stock: List<String>)
