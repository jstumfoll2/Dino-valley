package com.dungeonquest.engine.activity.count

import com.dungeonquest.engine.model.Arrangement
import com.dungeonquest.engine.model.CountObjectsInstance
import com.dungeonquest.engine.model.Ease
import com.dungeonquest.engine.model.PlacedObject
import com.dungeonquest.engine.model.Scene
import com.dungeonquest.engine.model.SkillId
import com.dungeonquest.engine.model.SpriteId
import com.dungeonquest.engine.model.TemplateId
import com.dungeonquest.engine.util.GameRandom
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.hypot
import kotlin.random.Random

/**
 * Turns a level description and a seed into one concrete "how many?" question, including where
 * every object sits. The same seed always produces the same question.
 */
class CountObjectsGenerator(
    private val sprites: List<SpriteId>,
    private val distractorSprites: List<SpriteId>,
    private val templateId: TemplateId = TemplateId("count_dino_eggs"),
    private val skill: SkillId = SkillId("counting"),
) {
    init {
        require(sprites.isNotEmpty()) { "Need at least one sprite to count" }
    }

    fun generate(level: CountObjectsLevel, ease: Ease, seed: Long): CountObjectsInstance {
        val random = GameRandom.forQuestion(seed)
        val highest = when (ease) {
            Ease.NORMAL -> level.maxCount
            Ease.GENTLE -> maxOf(level.minCount, (level.minCount + level.maxCount) / 2)
        }
        val answer = random.nextInt(level.minCount, highest + 1)
        val sprite = sprites.random(random)
        val distractors = if (distractorSprites.isEmpty()) 0 else level.distractors
        val positions = Layout.place(level.arrangement, answer + distractors, level.objectSize.scale, random)
        val countableSlots = positions.indices.shuffled(random).take(answer).toSet()
        val objects = positions.mapIndexed { i, p ->
            val countable = i in countableSlots
            PlacedObject(
                sprite = if (countable) sprite else distractorSprites.random(random),
                x = p.x,
                y = p.y,
                scale = level.objectSize.scale,
                rotationDeg = p.rotationDeg,
                countable = countable,
            )
        }
        return CountObjectsInstance(
            templateId = templateId,
            skill = skill,
            level = level.level,
            scene = Scene(objects),
            seed = seed,
            answer = answer,
            choices = choicesFor(answer, level, random),
            tapToCount = level.tapToCount,
        )
    }

    /** The answer plus the nearest other numbers, shown in counting order. */
    private fun choicesFor(answer: Int, level: CountObjectsLevel, random: Random): List<Int> {
        val upper = maxOf(level.maxCount, level.choiceCount)
        val others = (1..upper)
            .filter { it != answer }
            .shuffled(random)
            .sortedBy { abs(it - answer) }
            .take(level.choiceCount - 1)
        return (others + answer).sorted()
    }
}

/** Places objects in a 0..1 play area. Distances account for a landscape phone (about 2:1). */
internal object Layout {
    data class Point(val x: Float, val y: Float, val rotationDeg: Float = 0f)

    private const val ASPECT = 2f
    private const val LEFT = 0.08f
    private const val RIGHT = 0.92f
    private const val TOP = 0.18f
    private const val BOTTOM = 0.82f

    fun place(arrangement: Arrangement, count: Int, scale: Float, random: Random): List<Point> = when (arrangement) {
        Arrangement.LINE -> line(count, random)
        Arrangement.GRID -> grid(count)
        Arrangement.SCATTER -> scatter(count, 0.17f * scale, random)
        Arrangement.CLUSTERS -> clusters(count, scale, random)
    }

    private fun line(count: Int, random: Random): List<Point> {
        val step = minOf(0.16f, (RIGHT - LEFT) / count)
        return List(count) { i ->
            val x = 0.5f + (i - (count - 1) / 2f) * step
            Point(x, 0.5f + random.nextFloat(-0.03f, 0.03f))
        }
    }

    private fun grid(count: Int): List<Point> {
        val cols = minOf(count, 5)
        val rows = ceil(count / cols.toFloat()).toInt()
        return List(count) { i ->
            val col = i % cols
            val row = i / cols
            val inRow = if (row == rows - 1) count - row * cols else cols
            val x = 0.5f + (col - (inRow - 1) / 2f) * 0.15f
            val y = 0.5f + (row - (rows - 1) / 2f) * 0.25f
            Point(x, y)
        }
    }

    private fun scatter(count: Int, startDistance: Float, random: Random): List<Point> {
        var minDistance = startDistance
        while (true) {
            val points = mutableListOf<Point>()
            var attempts = 0
            while (points.size < count && attempts < 400) {
                attempts++
                val candidate = Point(random.nextFloat(LEFT, RIGHT), random.nextFloat(TOP, BOTTOM), random.nextFloat(-12f, 12f))
                if (points.all { distance(it, candidate) >= minDistance }) points += candidate
            }
            if (points.size == count) return points
            minDistance *= 0.9f
        }
    }

    private fun clusters(count: Int, scale: Float, random: Random): List<Point> {
        val sizes = mutableListOf<Int>()
        var left = count
        while (left > 0) {
            val size = if (left <= 3) left else random.nextInt(2, 4)
            sizes += size
            left -= size
        }
        val centers = scatter(sizes.size, 0.3f, random)
        val spread = 0.07f * scale
        return sizes.zip(centers).flatMap { (size, c) ->
            List(size) { i ->
                val angle = (i * 2 * Math.PI / size) + random.nextDouble(0.0, 0.6)
                Point(
                    (c.x + spread * kotlin.math.cos(angle).toFloat()).coerceIn(LEFT, RIGHT),
                    (c.y + spread * ASPECT * kotlin.math.sin(angle).toFloat()).coerceIn(TOP, BOTTOM),
                    random.nextFloat(-12f, 12f),
                )
            }
        }
    }

    private fun distance(a: Point, b: Point): Float = hypot((a.x - b.x) * ASPECT, a.y - b.y)

    private fun Random.nextFloat(from: Float, until: Float): Float = from + nextFloat() * (until - from)
}
