package com.dinovalley.engine.rpg.items

/** Where a piece of gear is worn. What is worn is drawn on the hero. */
enum class Slot(val word: String) { HEAD("head"), HAND("hand"), BODY("body"), FEET("feet") }

enum class ItemKind { CONSUMABLE, TOOL, KEY, CHARM, EQUIPMENT, QUEST }

/** What a consumable does in a fight. */
enum class BattleUse { HEAL, DAMAGE, STUN, ESCAPE, BEFRIEND, SHIELD }

/** What can block the way, and so which tools can get past it. */
enum class Obstacle(val word: String) {
    CLIMB("a steep climb"),
    CROSS("a rushing river"),
    DARK("a dark place"),
    LOCK("a locked door"),
    RIDDLE("a tricky puzzle"),
}

/**
 * Anything the hero can carry. Items are data: a new one needs an entry here and a picture named
 * `art_item_<id>`; no other code changes. [guess] is for charms: after a wrong answer it lets the
 * hero guess again, with [guess] wrong answers taken away first (never more than the puzzle has).
 */
data class Item(
    val id: String,
    val name: String,
    val kind: ItemKind,
    /** One friendly sentence, said when it is found or looked at. */
    val blurb: String,
    val price: Int = 0,
    val slot: Slot? = null,
    val attack: Int = 0,
    val defense: Int = 0,
    val hp: Int = 0,
    /** Funny gear: just as useful, and the monsters giggle. */
    val funny: Boolean = false,
    val use: BattleUse? = null,
    val power: Int = 0,
    val opens: Set<Obstacle> = emptySet(),
    val guess: Int = 0,
) {
    val isGear: Boolean get() = kind == ItemKind.EQUIPMENT
    val isCharm: Boolean get() = kind == ItemKind.CHARM
    val sellable: Boolean get() = price > 0
}
