package com.littledungeon.engine.rpg.hero

import com.littledungeon.engine.rpg.content.Content
import com.littledungeon.engine.rpg.items.Item
import com.littledungeon.engine.rpg.items.Slot
import com.littledungeon.engine.rpg.learn.Skill
import kotlin.math.floor
import kotlin.math.sqrt

/** The five child-friendly attributes from the brief. Learning grows them. */
enum class Attribute(val word: String) {
    COURAGE("courage"),
    CLEVERNESS("cleverness"),
    WISDOM("wisdom"),
    KINDNESS("kindness"),
    MAGIC("magic"),
}

/**
 * Original adventurer classes. Each has one special power per adventure that changes how a
 * moment plays out, never one that skips the learning.
 */
enum class HeroClass(val title: String, val main: Attribute, val unlockLevel: Int, val power: Power) {
    KNIGHT("Knight", Attribute.COURAGE, 1, Power.BRAVE_REROLL),
    WIZARD("Wizard", Attribute.MAGIC, 1, Power.SPARKLE_HINT),
    RANGER("Ranger", Attribute.WISDOM, 1, Power.KEEN_EYES),
    GUARDIAN("Guardian", Attribute.KINDNESS, 3, Power.FRIEND_MAGNET),
    SPELLKEEPER("Spellkeeper", Attribute.MAGIC, 6, Power.SPARKLE_HINT),
}

enum class Power {
    /** Knight: roll the die again once, keeping the better roll. */
    BRAVE_REROLL,

    /** Wizard and Spellkeeper: hints come one step sooner, ending in a glow on the right answer. */
    SPARKLE_HINT,

    /** Ranger: peeks behind the doors on the map. */
    KEEN_EYES,

    /** Guardian: everyone the hero meets wants to be friends. */
    FRIEND_MAGNET,
}

/** Something new to wear, use or play as, earned by levelling up. */
data class Unlock(val level: Int, val id: String, val announcement: String)

/**
 * The adventurer as saved on the phone. XP is kept per attribute; the level comes from the total.
 * Coins, the bag and what is worn are kept between adventures too, so shopping and loot last.
 */
data class Hero(
    val heroClass: HeroClass = HeroClass.KNIGHT,
    val xp: Map<Attribute, Int> = emptyMap(),
    val coins: Int = 0,
    /** Item id to how many. */
    val bag: Map<String, Int> = emptyMap(),
    /** What is worn in each place (an item id), drawn on the hero. */
    val worn: Map<Slot, String> = emptyMap(),
) {
    val totalXp: Int get() = xp.values.sum()
    val level: Int get() = Progression.levelFor(totalXp)

    /** Added to dice rolls: +1 to start, growing slowly with level, never more than +3. */
    val diceBonus: Int get() = (1 + (level - 1) / 3).coerceAtMost(3)

    val unlocks: List<Unlock> get() = Progression.unlocks.filter { it.level <= level }

    fun gain(attribute: Attribute, amount: Int): Hero =
        copy(xp = xp + (attribute to (xp[attribute] ?: 0) + amount))

    /** The attribute with the most stars, for the story to notice what the child is good at. */
    val strongest: Attribute? get() = xp.maxByOrNull { it.value }?.takeIf { it.value > 0 }?.key

    // ------------------------------------------------------------- things carried

    fun count(itemId: String): Int = bag[itemId] ?: 0

    fun has(itemId: String): Boolean = count(itemId) > 0

    fun give(itemId: String, n: Int = 1): Hero = copy(bag = bag + (itemId to count(itemId) + n))

    fun take(itemId: String, n: Int = 1): Hero {
        val left = count(itemId) - n
        return copy(bag = if (left > 0) bag + (itemId to left) else bag - itemId)
    }

    fun earn(n: Int): Hero = copy(coins = (coins + n).coerceAtLeast(0))

    /** Puts a piece of gear on (it must be in the bag; the one that was worn goes back in). */
    fun wear(item: Item): Hero {
        val slot = item.slot ?: return this
        if (!has(item.id)) return this
        val old = worn[slot]
        var h = take(item.id).copy(worn = worn + (slot to item.id))
        if (old != null) h = h.give(old)
        return h
    }

    /** Takes a piece of gear off into the bag. */
    fun unwear(slot: Slot): Hero {
        val id = worn[slot] ?: return this
        return copy(worn = worn - slot).give(id)
    }

    val gear: List<Item> get() = worn.values.mapNotNull { Content.item(it) }

    // ------------------------------------------------------------- what the stars do

    /** How strong an attribute is, from its own stars: 1 at the start, then 2, 3, 4 and on. */
    fun statLevel(a: Attribute): Int = 1 + floor(sqrt((xp[a] ?: 0) / 40.0)).toInt()

    /** Courage is health: more courage, more to lose. */
    val maxHp: Int get() = 20 + 5 * statLevel(Attribute.COURAGE) + gear.sumOf { it.hp }

    /** Each attribute powers its own kind of puzzle attack (numbers: cleverness; colors and patterns: magic; letters: wisdom). */
    fun attackWith(skill: Skill): Int = 5 + statLevel(skill.attribute) + gear.sumOf { it.attack }

    /** Takes this much off every hit. */
    val defense: Int get() = gear.sumOf { it.defense } + (statLevel(Attribute.COURAGE) - 1) / 2

    /** Percent off shop prices: kindness makes shopkeepers like you. */
    val discountPercent: Int get() = (5 * (statLevel(Attribute.KINDNESS) - 1)).coerceAtMost(40)

    /** Percent more coins from loot: wisdom spots the good stuff. */
    val lootBonusPercent: Int get() = (10 * (statLevel(Attribute.WISDOM) - 1)).coerceAtMost(50)

    /** Extra health from every healing item. */
    val healBonus: Int get() = statLevel(Attribute.KINDNESS) - 1

    /** Wisdom shows the danger on a road before you take it. */
    val seesDangers: Boolean get() = statLevel(Attribute.WISDOM) >= 2

    /** Puzzles get harder as the hero grows: one level up for every three hero levels. */
    val puzzleBoost: Int get() = (level - 1) / 3

    fun priceOf(item: Item): Int = (item.price * (100 - discountPercent) / 100).coerceAtLeast(1)
}

object Progression {
    /**
     * Stars needed to reach [level]: 180, 420, 720, 1080, … Each level asks for 60 more than
     * the one before. An adventure earns about 250 stars, so the first ones level up every time
     * and later ones every few adventures.
     */
    fun xpFor(level: Int): Int = (1 until level).sumOf { 120 + 60 * it }

    fun levelFor(xp: Int): Int {
        var level = 1
        while (xp >= xpFor(level + 1)) level++
        return level
    }

    /** How far through the current level, 0..1, for the star bar. */
    fun progress(xp: Int): Float {
        val level = levelFor(xp)
        val from = xpFor(level)
        val to = xpFor(level + 1)
        return (xp - from).toFloat() / (to - from)
    }

    val unlocks = listOf(
        Unlock(2, "feather_hat", "You earned a feather hat!"),
        Unlock(3, "class_guardian", "A new adventurer can join: the Guardian!"),
        Unlock(4, "star_cape", "You earned a cape covered in stars!"),
        Unlock(5, "dragon_sparkles", "Your dragon learned a new trick: sparkle breath!"),
        Unlock(6, "class_spellkeeper", "A new adventurer can join: the Spellkeeper!"),
        Unlock(7, "golden_crown", "You earned a golden crown!"),
        Unlock(8, "dragon_wings", "Your dragon's wings grew big and strong!"),
        Unlock(9, "magic_boots", "You earned magic boots that sparkle when you walk!"),
        Unlock(10, "dragon_armor", "Your dragon got shiny silver armor!"),
        Unlock(12, "rainbow_dice", "You earned a rainbow die!"),
    )

    fun unlocksBetween(fromLevel: Int, toLevel: Int): List<Unlock> = unlocks.filter { it.level in (fromLevel + 1)..toLevel }
}
