package com.littledungeon.engine.rpg.learn

import com.littledungeon.engine.model.Speech

enum class Hue(val word: String) {
    RED("red"), BLUE("blue"), GREEN("green"), YELLOW("yellow"), PURPLE("purple"), ORANGE("orange"),
}

/** Things to count. The app draws each one. */
enum class Thing(val one: String, val many: String) {
    STONE("stone", "stones"),
    COIN("coin", "coins"),
    GEM("gem", "gems"),
    POTION("potion", "potions"),
    KEY("key", "keys"),
    MUSHROOM("mushroom", "mushrooms"),
    ;

    fun words(n: Int) = if (n == 1) one else many
}

enum class GemSize(val word: String) { SMALL("small"), BIG("big") }

data class Gem(val hue: Hue, val size: GemSize = GemSize.BIG)

enum class RuneShape(val word: String) { STAR("star"), MOON("moon"), SUN("sun"), HEART("heart"), DROP("drop") }

data class Rune(val shape: RuneShape, val hue: Hue)

/** How a letter is asked for, from easiest to hardest. */
enum class LetterMode { NAME, SOUND, FIRST_SOUND }

/** Paths to trace with a finger, from a straight line up to shapes (brief: pre-writing stages). */
enum class TraceShape { LINE, CURVE, ZIGZAG, LOOP, CIRCLE, TRIANGLE, LETTER, NUMBER }

/** A point on the tracing card, 0..1 across and down. */
data class Point(val x: Float, val y: Float)

enum class Ingredient(val hue: Hue, val one: String, val many: String) {
    LEAF(Hue.GREEN, "green leaf", "green leaves"),
    BERRY(Hue.RED, "red berry", "red berries"),
    CRYSTAL(Hue.BLUE, "blue crystal", "blue crystals"),
    FLOWER(Hue.YELLOW, "yellow flower", "yellow flowers"),
    MUSHROOM(Hue.PURPLE, "purple mushroom", "purple mushrooms"),
    ;

    fun words(n: Int) = if (n == 1) one else many
}

data class RecipeStep(val ingredient: Ingredient, val count: Int)

/** Where a door sits on the map, for "the door on the left". */
enum class Side(val word: String) { LEFT("left"), MIDDLE("middle"), RIGHT("right") }

data class MapDoor(val hue: Hue, val side: Side)

/**
 * One learning moment, already dressed in its story. [prompt] is what the narrator asks (and
 * repeats on the replay button). Most challenges are "pick one picture": [options] pictures,
 * of which [answer] is right. Sequence challenges (memory, recipes) are judged step by step.
 */
sealed interface Challenge {
    val skill: Skill
    val level: Int
    val seed: Long
    val prompt: List<Speech>
    val kind: String get() = this::class.simpleName ?: "challenge"
}

/** A challenge answered by tapping one of several pictures. */
sealed interface PickOne : Challenge {
    val optionCount: Int
    val answer: Int
}

data class CountChallenge(
    override val level: Int,
    override val seed: Long,
    override val prompt: List<Speech>,
    val thing: Thing,
    val count: Int,
    val options: List<Int>,
) : PickOne {
    override val skill get() = Skill.COUNTING
    override val optionCount get() = options.size
    override val answer get() = options.indexOf(count)
}

/**
 * "You have [have] stones and find [more] more", or with [missingAddend] "the bridge needs
 * [total], you have [have], how many more?". The pictures show both groups.
 */
data class AddChallenge(
    override val level: Int,
    override val seed: Long,
    override val prompt: List<Speech>,
    val thing: Thing,
    val have: Int,
    val more: Int,
    val missingAddend: Boolean,
    val options: List<Int>,
) : PickOne {
    override val skill get() = Skill.ADDITION
    val total: Int get() = have + more
    val solution: Int get() = if (missingAddend) more else total
    override val optionCount get() = options.size
    override val answer get() = options.indexOf(solution)
}

/** Find a written number among other numbers (numeral recognition, no dots to count). */
data class NumberChallenge(
    override val level: Int,
    override val seed: Long,
    override val prompt: List<Speech>,
    val number: Int,
    val options: List<Int>,
) : PickOne {
    override val skill get() = Skill.NUMBERS
    override val optionCount get() = options.size
    override val answer get() = options.indexOf(number)
}

data class ColorChallenge(
    override val level: Int,
    override val seed: Long,
    override val prompt: List<Speech>,
    val target: Gem,
    val options: List<Gem>,
) : PickOne {
    override val skill get() = Skill.COLORS
    override val optionCount get() = options.size
    override val answer get() = options.indexOf(target)
}

data class PatternChallenge(
    override val level: Int,
    override val seed: Long,
    override val prompt: List<Speech>,
    val shown: List<Rune>,
    val next: Rune,
    val options: List<Rune>,
) : PickOne {
    override val skill get() = Skill.PATTERNS
    override val optionCount get() = options.size
    override val answer get() = options.indexOf(next)
}

data class LetterChallenge(
    override val level: Int,
    override val seed: Long,
    override val prompt: List<Speech>,
    val letter: Char,
    val word: String,
    val mode: LetterMode,
    val options: List<Char>,
) : PickOne {
    override val skill get() = Skill.LETTERS
    override val optionCount get() = options.size
    override val answer get() = options.indexOf(letter)
}

/** Pick a door on the map from a clue. */
data class MapChallenge(
    override val level: Int,
    override val seed: Long,
    override val prompt: List<Speech>,
    val doors: List<MapDoor>,
    override val answer: Int,
) : PickOne {
    override val skill get() = Skill.MAPS
    override val optionCount get() = doors.size
}

/**
 * Trace with a finger, one stroke after another, each from its start in its direction: a
 * path, a shape, or a letter or number written the way it is taught ([glyph]). A stroke counts
 * once most of its points are touched within [tolerance] (fraction of the card's height).
 */
data class TraceChallenge(
    override val level: Int,
    override val seed: Long,
    override val prompt: List<Speech>,
    val shape: TraceShape,
    val strokes: List<List<Point>>,
    val tolerance: Float,
    val glyph: Char? = null,
) : Challenge {
    override val skill get() = Skill.TRACING
    val path: List<Point> get() = strokes.flatten()
}

/** Look at the doors, then they hide; tap the doors in [sequence] order (indices into [doors]). */
data class MemoryChallenge(
    override val level: Int,
    override val seed: Long,
    override val prompt: List<Speech>,
    val doors: List<Hue>,
    val sequence: List<Int>,
    val showMillis: Long,
    /** Said while the doors are visible, e.g. "Remember the GREEN door." */
    val remember: List<Speech>,
) : Challenge {
    override val skill get() = Skill.MEMORY
}

enum class PotionKind(val title: String) {
    GIANT_STRENGTH("Potion of Giant Strength"),
    GLOW("Potion of Glowing"),
    BUBBLE("Potion of Bubbles"),
    FRIENDSHIP("Potion of Friendship"),
}

/**
 * Brew a potion: put ingredients from the [shelf] into the cauldron, then stir [stirs] times.
 * With [ordered], steps must go in order. With [hideRecipe], the recipe is shown and then
 * hidden (memory). With [riddle], the recipe is described instead of shown.
 */
data class RecipeChallenge(
    override val level: Int,
    override val seed: Long,
    override val prompt: List<Speech>,
    val potion: PotionKind,
    val steps: List<RecipeStep>,
    val stirs: Int,
    val ordered: Boolean,
    val hideRecipe: Boolean,
    val riddle: List<Speech>?,
    val shelf: List<Ingredient>,
) : Challenge {
    override val skill get() = Skill.RECIPES
}

/** How things are sorted into baskets, from easiest to hardest. */
enum class SortRule { COLOR, KIND, SIZE }

/** Something to sort: a gem (with its color and size) or another [Thing]. */
data class Sortable(val thing: Thing, val hue: Hue = Hue.RED, val size: GemSize = GemSize.BIG)

/**
 * Drag every item into its basket. [baskets] shows one sample item on each basket; [home] is
 * the right basket for each of [items]. Judged item by item.
 */
data class SortChallenge(
    override val level: Int,
    override val seed: Long,
    override val prompt: List<Speech>,
    val rule: SortRule,
    val baskets: List<Sortable>,
    val items: List<Sortable>,
    val home: List<Int>,
) : Challenge {
    override val skill get() = Skill.SORTING
}

/**
 * Counting by [step]s: [shown] lily pads hold [step] [thing]s each, the first ones labeled with
 * the running count (2, 4, 6) and the last one asking for its number.
 */
data class SkipCountChallenge(
    override val level: Int,
    override val seed: Long,
    override val prompt: List<Speech>,
    val step: Int,
    val shown: Int,
    val thing: Thing,
    val options: List<Int>,
) : PickOne {
    override val skill get() = Skill.SKIP_COUNTING
    val total: Int get() = step * shown
    override val optionCount get() = options.size
    override val answer get() = options.indexOf(total)
}

/**
 * A picture cut into [cols] x [rows] pieces, to put back in place. With [ghost] a faint copy of
 * the picture shows where each piece goes. The app draws the place's picture.
 */
data class PuzzleChallenge(
    override val level: Int,
    override val seed: Long,
    override val prompt: List<Speech>,
    val picture: PuzzlePicture,
    val cols: Int,
    val rows: Int,
    val ghost: Boolean,
) : Challenge {
    override val skill get() = Skill.PUZZLES
    val pieces: Int get() = cols * rows
}

/** Pictures for the mosaic puzzles: the places the child knows from adventures. */
enum class PuzzlePicture(val said: String) {
    CAMP("our camp"),
    BRIDGE("the wobbly bridge"),
    CRYSTAL_CAVE("the crystal cave"),
    LIBRARY("the spell library"),
    LAIR("the dragon's lair"),
    POND("the frog pond"),
}
