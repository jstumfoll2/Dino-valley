package com.dinovalley.engine.rpg.learn

import com.dinovalley.engine.model.Speech
import com.dinovalley.engine.rpg.learn.Words.number
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * The learning engine's generator: turns a skill level and a seed into one concrete challenge.
 * Rooms decide the story words around it; the factory decides the numbers, colors and letters.
 * The same seed always gives the same challenge.
 */
object ChallengeFactory {

    fun count(level: Int, seed: Long, thing: Thing, question: String): CountChallenge {
        val r = Random(seed)
        val (min, max, opts) = when (level) {
            1 -> Triple(1, 3, 2)
            2 -> Triple(1, 5, 3)
            3 -> Triple(2, 7, 3)
            4 -> Triple(3, 10, 4)
            else -> Triple(5, 12, 4)
        }
        val n = r.nextInt(min, max + 1)
        return CountChallenge(level, seed, Speech.of(question), thing, n, numberOptions(n, opts, r))
    }

    /** [story] decides the words; it gets have, more and total and says them. */
    fun add(level: Int, seed: Long, thing: Thing, story: (have: Int, more: Int, missing: Boolean) -> String): AddChallenge {
        val r = Random(seed)
        val (maxTotal, opts) = when (level) {
            1 -> 3 to 2
            2 -> 5 to 3
            3 -> 7 to 3
            4 -> 10 to 4
            else -> 10 to 4
        }
        val total = r.nextInt(2, maxTotal + 1)
        val have = r.nextInt(1, total)
        val more = total - have
        val missing = level >= 5
        val solution = if (missing) more else total
        return AddChallenge(level, seed, Speech.of(story(have, more, missing)), thing, have, more, missing, numberOptions(solution, opts, r))
    }

    fun color(level: Int, seed: Long, who: String, what: String = "crystal"): ColorChallenge {
        val r = Random(seed)
        val hues = Hue.entries.shuffled(r)
        val sized = level >= 4
        val target = Gem(hues[0], if (sized) GemSize.entries.random(r) else GemSize.BIG)
        val options = when (level) {
            1 -> listOf(target, Gem(hues[1]))
            2 -> listOf(target, Gem(hues[1]), Gem(hues[2]))
            3 -> listOf(target, Gem(hues[1]), Gem(hues[2]), Gem(hues[3]))
            4 -> listOf(target, target.copy(size = other(target.size)), Gem(hues[1], target.size), Gem(hues[2], other(target.size)))
            else -> listOf(
                target, target.copy(size = other(target.size)), Gem(hues[1], target.size),
                Gem(hues[1], other(target.size)), Gem(hues[2], target.size),
            )
        }.shuffled(r)
        val name = (if (sized) "${target.size.word} " else "") + target.hue.word.uppercase()
        return ColorChallenge(level, seed, Speech.of("$who needs the $name $what. Can you find it?"), target, options)
    }

    private fun other(size: GemSize) = if (size == GemSize.BIG) GemSize.SMALL else GemSize.BIG

    fun pattern(level: Int, seed: Long): PatternChallenge {
        val r = Random(seed)
        val unit: List<Int> = when (level) {
            1 -> listOf(0, 1)
            2 -> if (r.nextBoolean()) listOf(0, 1) else listOf(0, 0, 1)
            3 -> if (r.nextBoolean()) listOf(0, 1, 2) else listOf(0, 1, 1)
            4 -> listOf(listOf(0, 0, 1, 1), listOf(0, 1, 1, 2), listOf(0, 1, 2)).random(r)
            else -> listOf(listOf(0, 1, 1, 2), listOf(0, 0, 1, 2), listOf(0, 1, 2, 1)).random(r)
        }
        val kinds = unit.distinct().size
        val hues = Hue.entries.shuffled(r)
        val shapes = RuneShape.entries.shuffled(r)
        val runes: List<Rune> = List(maxOf(kinds, 4)) { i ->
            when {
                level <= 2 -> Rune(shapes[0], hues[i]) // colors only
                level == 3 -> Rune(shapes[i], hues[0]) // shapes only
                else -> Rune(shapes[i], hues[i]) // both
            }
        }
        val length = minOf(7, unit.size * 2 + r.nextInt(0, unit.size))
        val shown = List(length) { runes[unit[it % unit.size]] }
        val next = runes[unit[length % unit.size]]
        val optionCount = when (level) {
            1 -> 2
            2, 3 -> 3
            else -> 4
        }
        val options = (listOf(next) + runes.filter { it != next }.take(optionCount - 1)).shuffled(r)
        val prompt = Speech.of("The ancient door opens only when its magic symbols are in order. Which symbol comes next?")
        return PatternChallenge(level, seed, prompt, shown, next, options)
    }

    fun letter(level: Int, seed: Long, purpose: String): LetterChallenge {
        val r = Random(seed)
        val mode = when (level) {
            1, 2 -> LetterMode.NAME
            3 -> LetterMode.SOUND
            else -> LetterMode.FIRST_SOUND
        }
        val pool = when {
            mode != LetterMode.NAME -> Words.LETTER_SOUNDS.keys.toList()
            level == 1 -> "ABCDEMOST".toList()
            else -> ('A'..'Z').toList()
        }
        val letter = pool.random(r)
        val word = Words.LETTER_WORDS.getValue(letter)
        val optionCount = when (level) {
            1 -> 2
            5 -> 4
            else -> 3
        }
        val options = (listOf(letter) + pool.filter { it != letter }.shuffled(r).take(optionCount - 1)).shuffled(r)
        val ask = when (mode) {
            LetterMode.NAME -> "$purpose Find the rune $letter. $letter, as in $word."
            LetterMode.SOUND -> "$purpose It starts with the sound ${Words.LETTER_SOUNDS.getValue(letter)}. Which rune makes that sound?"
            LetterMode.FIRST_SOUND -> "$purpose The magic word is ${word.uppercase()}. Which rune does ${word.uppercase()} start with?"
        }
        return LetterChallenge(level, seed, Speech.of(ask), letter, word, mode, options)
    }

    fun trace(level: Int, seed: Long, goal: String): TraceChallenge {
        val r = Random(seed)
        val shape = when (level) {
            1 -> TraceShape.LINE
            2 -> TraceShape.CURVE
            3 -> TraceShape.ZIGZAG
            4 -> TraceShape.LOOP
            else -> if (r.nextBoolean()) TraceShape.CIRCLE else TraceShape.TRIANGLE
        }
        val tolerance = 0.14f - 0.012f * level
        return TraceChallenge(level, seed, Speech.of("Draw the magic path with your finger, $goal"), shape, path(shape, r), tolerance)
    }

    /** Evenly spaced points along the path, left to right (or around, for shapes). */
    fun path(shape: TraceShape, r: Random): List<Point> {
        val n = 40
        val wobble = 0.85f + r.nextFloat() * 0.3f
        fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t
        return when (shape) {
            TraceShape.LINE -> List(n) { Point(lerp(0.1f, 0.9f, it / (n - 1f)), 0.5f) }
            TraceShape.CURVE -> List(n) {
                val t = it / (n - 1f)
                Point(lerp(0.1f, 0.9f, t), 0.5f + 0.28f * wobble * sin(t * PI).toFloat() * -1f)
            }
            TraceShape.ZIGZAG -> {
                val peaks = listOf(Point(0.1f, 0.7f), Point(0.3f, 0.3f), Point(0.5f, 0.7f), Point(0.7f, 0.3f), Point(0.9f, 0.7f))
                along(peaks, n)
            }
            TraceShape.LOOP -> List(n) {
                val t = it / (n - 1f)
                val a = t * 2 * PI * 2
                Point(lerp(0.12f, 0.88f, t) + 0.07f * sin(a).toFloat(), 0.55f - 0.2f * wobble * ((1 - cos(a)) / 2).toFloat())
            }
            TraceShape.CIRCLE -> List(n) {
                val a = -PI / 2 + it / (n - 1.0) * 2 * PI
                Point(0.5f + 0.3f * cos(a).toFloat(), 0.5f + 0.32f * sin(a).toFloat())
            }
            TraceShape.TRIANGLE -> along(listOf(Point(0.5f, 0.15f), Point(0.85f, 0.85f), Point(0.15f, 0.85f), Point(0.5f, 0.15f)), n)
        }
    }

    private fun along(corners: List<Point>, n: Int): List<Point> {
        val segs = corners.zipWithNext()
        val lengths = segs.map { (a, b) -> kotlin.math.hypot(b.x - a.x, b.y - a.y) }
        val total = lengths.sum()
        return List(n) { i ->
            var d = total * i / (n - 1)
            var k = 0
            while (k < segs.lastIndex && d > lengths[k]) {
                d -= lengths[k]
                k++
            }
            val (a, b) = segs[k]
            val t = (d / lengths[k]).coerceIn(0f, 1f)
            Point(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t)
        }
    }

    fun memory(level: Int, seed: Long): MemoryChallenge {
        val r = Random(seed)
        val (doorCount, steps, show) = when (level) {
            1 -> Triple(3, 1, 2600L)
            2 -> Triple(4, 1, 2200L)
            3 -> Triple(3, 2, 3200L)
            4 -> Triple(4, 2, 3000L)
            else -> Triple(4, 3, 3600L)
        }
        val doors = Hue.entries.shuffled(r).take(doorCount)
        val sequence = doors.indices.shuffled(r).take(steps)
        val names = sequence.map { doors[it].word.uppercase() }
        val remember = if (steps == 1) "Remember the ${names[0]} door." else "Remember the doors in order: ${names.joinToString(", then ")}."
        val ask = if (steps == 1) "The doors are hiding! Which one was it?" else "The doors are hiding! Tap them in order."
        return MemoryChallenge(level, seed, Speech.of(ask), doors, sequence, show, Speech.of(remember))
    }

    fun recipe(level: Int, seed: Long, potion: PotionKind): RecipeChallenge {
        val r = Random(seed)
        val stepCount = if (level <= 2) 2 else 3
        val maxEach = if (level == 1) 1 else 2 + (level / 4)
        val ingredients = Ingredient.entries.shuffled(r)
        val steps = ingredients.take(stepCount).map { RecipeStep(it, r.nextInt(1, maxEach + 1)) }
        val stirs = if (level == 1) 0 else r.nextInt(2, 3 + level)
        val shelf = ingredients.take(minOf(Ingredient.entries.size, stepCount + if (level >= 3) 2 else 1)).shuffled(r)
        val ordered = level >= 3
        val said = steps.mapIndexed { i, s ->
            val lead = if (ordered) listOf("First", "Then", "Next").getOrElse(i) { "Then" } + " add" else "Add"
            "$lead ${number(s.count)} ${s.ingredient.words(s.count)}."
        } + if (stirs > 0) listOf("${if (ordered) "Finally, stir" else "Stir"} ${number(stirs)} times.") else emptyList()
        val riddle = if (level >= 5) Speech.of(riddleFor(steps, stirs)) else null
        val prompt = Speech.of("Let's brew the ${potion.title}! " + (if (riddle != null) riddleFor(steps, stirs) else said.joinToString(" ")))
        return RecipeChallenge(level, seed, prompt, potion, steps, stirs, ordered, hideRecipe = level == 4, riddle = riddle, shelf = shelf)
    }

    private fun riddleFor(steps: List<RecipeStep>, stirs: Int): String {
        val clue = mapOf(
            Ingredient.LEAF to "green things that grow on trees",
            Ingredient.BERRY to "red things that grow on bushes",
            Ingredient.CRYSTAL to "blue things that sparkle",
            Ingredient.FLOWER to "yellow things that smell sweet",
            Ingredient.MUSHROOM to "purple things that grow in the dark",
        )
        val parts = steps.map { s -> "${number(s.count)} ${clue.getValue(s.ingredient).let { if (s.count == 1) singular(it) else it }}" }
        return "I need " + parts.joinToString(", then ") + ", and then stir ${number(stirs)} times."
    }

    private fun singular(clue: String) = clue.replaceFirst("things", "thing").replace(" grow ", " grows ").replace(" sparkle", " sparkles").replace(" smell ", " smells ")

    fun map(level: Int, seed: Long, doors: List<MapDoor>, target: Int, why: String): MapChallenge {
        val door = doors[target]
        val clue = if (level <= 2 || doors.map { it.side }.distinct().size < doors.size) {
            "$why behind the ${door.hue.word.uppercase()} door."
        } else {
            "$why behind the door on the ${door.side.word.uppercase()}."
        }
        return MapChallenge(level, seed, Speech.of("$clue Which door should we take?"), doors, target)
    }

    /** The right number and its nearest neighbors, so wrong choices are close, not silly. */
    fun numberOptions(answer: Int, count: Int, r: Random): List<Int> {
        val near = (1..maxOf(answer + count, count + 1)).filter { it != answer }.sortedBy { kotlin.math.abs(it - answer) * 10 + r.nextInt(10) }
        return (listOf(answer) + near.take(count - 1)).sorted()
    }
}
