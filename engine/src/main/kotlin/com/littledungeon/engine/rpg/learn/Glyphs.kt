package com.littledungeon.engine.rpg.learn

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * Capital letters and numbers as a child is taught to write them: the strokes in order, each
 * drawn from its start in its direction. Drawn in a letter box (u across, v down, both 0..1),
 * then placed in the middle of the tracing card.
 */
object Glyphs {
    /** Letters and numbers by how hard they are to write: straight lines, slants, curves, then the twisty ones. */
    val byLevel: List<String> = listOf("LTIHEF147", "AVWNMKXYZ17", "COUDPJ023", "BRSGQ5689", "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789")

    fun strokes(c: Char): List<List<Point>> = shapes.getValue(c).map { stroke -> place(resample(stroke)) }

    val all: Set<Char> get() = shapes.keys

    // ------------------------------------------------------------- building blocks

    private class Pen {
        val points = mutableListOf<Pair<Float, Float>>()
        fun to(u: Float, v: Float): Pen = apply { points += u to v }

        /** An arc around ([cu], [cv]) from angle [from] to [to] in degrees; 0 is right, 90 is down. */
        fun arc(cu: Float, cv: Float, ru: Float, rv: Float, from: Float, to: Float): Pen = apply {
            val n = 24
            for (i in 0..n) {
                val a = (from + (to - from) * i / n) * PI / 180
                points += (cu + ru * cos(a).toFloat()) to (cv + rv * sin(a).toFloat())
            }
        }
    }

    private fun line(vararg p: Float): List<Pair<Float, Float>> = p.toList().chunked(2).map { it[0] to it[1] }

    private fun pen(block: Pen.() -> Unit) = Pen().apply(block).points.toList()

    private val shapes: Map<Char, List<List<Pair<Float, Float>>>> = mapOf(
        'A' to listOf(line(0.5f, 0f, 0.05f, 1f), line(0.5f, 0f, 0.95f, 1f), line(0.24f, 0.62f, 0.76f, 0.62f)),
        'B' to listOf(
            line(0.12f, 0f, 0.12f, 1f),
            pen { to(0.12f, 0f).arc(0.52f, 0.25f, 0.34f, 0.25f, -90f, 90f).to(0.12f, 0.5f) },
            pen { to(0.12f, 0.5f).arc(0.55f, 0.75f, 0.38f, 0.25f, -90f, 90f).to(0.12f, 1f) },
        ),
        'C' to listOf(pen { arc(0.55f, 0.5f, 0.45f, 0.5f, -40f, -320f) }),
        'D' to listOf(line(0.12f, 0f, 0.12f, 1f), pen { to(0.12f, 0f).arc(0.38f, 0.5f, 0.52f, 0.5f, -90f, 90f).to(0.12f, 1f) }),
        'E' to listOf(line(0.15f, 0f, 0.15f, 1f), line(0.15f, 0f, 0.88f, 0f), line(0.15f, 0.5f, 0.75f, 0.5f), line(0.15f, 1f, 0.88f, 1f)),
        'F' to listOf(line(0.18f, 0f, 0.18f, 1f), line(0.18f, 0f, 0.88f, 0f), line(0.18f, 0.5f, 0.75f, 0.5f)),
        'G' to listOf(pen { arc(0.52f, 0.5f, 0.45f, 0.5f, -40f, -360f).to(0.6f, 0.5f) }),
        'H' to listOf(line(0.12f, 0f, 0.12f, 1f), line(0.88f, 0f, 0.88f, 1f), line(0.12f, 0.5f, 0.88f, 0.5f)),
        'I' to listOf(line(0.5f, 0f, 0.5f, 1f), line(0.2f, 0f, 0.8f, 0f), line(0.2f, 1f, 0.8f, 1f)),
        'J' to listOf(pen { to(0.72f, 0f).to(0.72f, 0.68f).arc(0.42f, 0.68f, 0.3f, 0.32f, 0f, 180f) }),
        'K' to listOf(line(0.15f, 0f, 0.15f, 1f), line(0.85f, 0f, 0.15f, 0.56f), line(0.38f, 0.4f, 0.9f, 1f)),
        'L' to listOf(line(0.2f, 0f, 0.2f, 1f, 0.85f, 1f)),
        'M' to listOf(line(0.08f, 1f, 0.08f, 0f), line(0.08f, 0f, 0.5f, 0.62f, 0.92f, 0f), line(0.92f, 0f, 0.92f, 1f)),
        'N' to listOf(line(0.15f, 1f, 0.15f, 0f), line(0.15f, 0f, 0.85f, 1f), line(0.85f, 1f, 0.85f, 0f)),
        'O' to listOf(pen { arc(0.5f, 0.5f, 0.45f, 0.5f, -90f, -450f) }),
        'P' to listOf(line(0.15f, 0f, 0.15f, 1f), pen { to(0.15f, 0f).arc(0.5f, 0.27f, 0.36f, 0.27f, -90f, 90f).to(0.15f, 0.54f) }),
        'Q' to listOf(pen { arc(0.5f, 0.5f, 0.45f, 0.5f, -90f, -450f) }, line(0.6f, 0.68f, 0.95f, 1f)),
        'R' to listOf(
            line(0.15f, 0f, 0.15f, 1f),
            pen { to(0.15f, 0f).arc(0.5f, 0.27f, 0.36f, 0.27f, -90f, 90f).to(0.15f, 0.54f) },
            line(0.45f, 0.54f, 0.9f, 1f),
        ),
        'S' to listOf(pen { arc(0.5f, 0.26f, 0.38f, 0.26f, -20f, -270f).arc(0.5f, 0.75f, 0.4f, 0.25f, -90f, 160f) }),
        'T' to listOf(line(0.08f, 0f, 0.92f, 0f), line(0.5f, 0f, 0.5f, 1f)),
        'U' to listOf(pen { to(0.15f, 0f).to(0.15f, 0.6f).arc(0.5f, 0.6f, 0.35f, 0.4f, 180f, 0f).to(0.85f, 0f) }),
        'V' to listOf(line(0.05f, 0f, 0.5f, 1f, 0.95f, 0f)),
        'W' to listOf(line(0f, 0f, 0.25f, 1f, 0.5f, 0.3f, 0.75f, 1f, 1f, 0f)),
        'X' to listOf(line(0.1f, 0f, 0.9f, 1f), line(0.9f, 0f, 0.1f, 1f)),
        'Y' to listOf(line(0.1f, 0f, 0.5f, 0.5f), line(0.9f, 0f, 0.5f, 0.5f, 0.5f, 1f)),
        'Z' to listOf(line(0.1f, 0f, 0.9f, 0f, 0.1f, 1f, 0.9f, 1f)),
        '0' to listOf(pen { arc(0.5f, 0.5f, 0.4f, 0.5f, -90f, -450f) }),
        '1' to listOf(line(0.25f, 0.2f, 0.55f, 0f, 0.55f, 1f)),
        '2' to listOf(pen { arc(0.5f, 0.3f, 0.38f, 0.3f, -160f, 20f).to(0.1f, 1f).to(0.9f, 1f) }),
        '3' to listOf(pen { arc(0.48f, 0.26f, 0.36f, 0.26f, -160f, 90f).arc(0.48f, 0.74f, 0.4f, 0.26f, -90f, 160f) }),
        '4' to listOf(line(0.65f, 0f, 0.05f, 0.68f, 0.95f, 0.68f), line(0.65f, 0f, 0.65f, 1f)),
        '5' to listOf(pen { to(0.2f, 0f).to(0.15f, 0.45f).arc(0.5f, 0.69f, 0.38f, 0.31f, -140f, 150f) }, line(0.2f, 0f, 0.85f, 0f)),
        '6' to listOf(pen { to(0.75f, 0f).to(0.18f, 0.68f).arc(0.5f, 0.72f, 0.32f, 0.28f, 180f, -180f) }),
        '7' to listOf(line(0.1f, 0f, 0.9f, 0f, 0.4f, 1f)),
        '8' to listOf(pen { arc(0.5f, 0.25f, 0.32f, 0.25f, 90f, -270f) }, pen { arc(0.5f, 0.74f, 0.38f, 0.26f, -90f, 270f) }),
        '9' to listOf(pen { arc(0.5f, 0.3f, 0.36f, 0.3f, 0f, -360f).to(0.8f, 1f) }),
    )

    /** Evenly spaced points, so long and short strokes are judged alike. */
    private fun resample(stroke: List<Pair<Float, Float>>): List<Pair<Float, Float>> {
        val segs = stroke.zipWithNext()
        val lengths = segs.map { (a, b) -> hypot(b.first - a.first, b.second - a.second) }
        val total = lengths.sum()
        val n = (total / 0.07f).toInt().coerceIn(6, 60)
        return List(n) { i ->
            var d = total * i / (n - 1)
            var k = 0
            while (k < segs.lastIndex && d > lengths[k]) {
                d -= lengths[k]
                k++
            }
            val (a, b) = segs[k]
            val t = if (lengths[k] == 0f) 0f else (d / lengths[k]).coerceIn(0f, 1f)
            (a.first + (b.first - a.first) * t) to (a.second + (b.second - a.second) * t)
        }
    }

    /** From the letter box to the tracing card: a tall letter in the middle of the wide card. */
    private fun place(stroke: List<Pair<Float, Float>>): List<Point> =
        stroke.map { (u, v) -> Point(0.5f + (u - 0.5f) * WIDTH, TOP + v * HEIGHT) }

    private const val WIDTH = 0.3f
    private const val TOP = 0.12f
    private const val HEIGHT = 0.76f
}
