package com.littledungeon.engine.rpg

import com.littledungeon.engine.model.Voice
import com.littledungeon.engine.rpg.learn.Card
import com.littledungeon.engine.rpg.learn.PictureChallenge
import com.littledungeon.engine.rpg.learn.PictureFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The rhyme, money and sharing puzzles: exactly one right card, and the sums and rhymes are true. */
class PictureChallengeTest {
    private fun all(): List<PictureChallenge> = (1..5).flatMap { level ->
        (0L until 300L).flatMap { s -> listOf(PictureFactory.rhyme(level, s), PictureFactory.money(level, s), PictureFactory.share(level, s)) }
    }

    private fun worth(card: Card) = card.pictures.sumOf { p -> p.art.removePrefix("mini_coin_").toInt() * p.count }

    @Test
    fun `every puzzle has one right card among distinct cards`() {
        for (c in all()) {
            assertTrue(c.answer in c.options.indices, "${c.kind} L${c.level}: answer outside the cards")
            assertEquals(c.options.size, c.options.map { it.label }.toSet().size, "${c.kind} L${c.level} ${Voice.caption(c.prompt)}: two cards say the same")
            assertTrue(c.options.size in 3..5)
            assertTrue(c.because.isNotEmpty())
        }
    }

    @Test
    fun `the right rhyme rhymes, the wrong ones do not, and first sounds match`() {
        for (level in 1..5) for (s in 0L until 300L) {
            val c = PictureFactory.rhyme(level, s)
            val picked = c.options[c.answer].label
            when (c.kind) {
                "rhyme" -> if (c.scene.isNotEmpty()) {
                    val target = c.scene.single().art.removePrefix("mini_rhyme_")
                    val family = PictureFactory.RHYMES.first { target in it }
                    assertTrue(picked in family && picked != target)
                    assertTrue(c.options.filterIndexed { i, _ -> i != c.answer }.none { it.label in family }, "a wrong card also rhymes")
                } else {
                    // Odd one out: the other two rhyme with each other.
                    val rest = c.options.filterIndexed { i, _ -> i != c.answer }.map { it.label }
                    assertTrue(PictureFactory.RHYMES.any { rest.all { w -> w in it } } && picked !in rest)
                    assertTrue(PictureFactory.RHYMES.none { f -> picked in f && rest.any { it in f } })
                }
                "firstSound" -> {
                    val target = c.scene.single().art.removePrefix("mini_rhyme_")
                    assertEquals(target.first(), picked.first())
                    assertTrue(c.options.filterIndexed { i, _ -> i != c.answer }.none { it.label.first() == target.first() })
                }
            }
        }
    }

    @Test
    fun `the right coins add up to the price and the others do not`() {
        for (level in 1..3) for (s in 0L until 300L) {
            val c = PictureFactory.money(level, s)
            val price = c.options[c.answer].label.toInt()
            assertEquals(price, worth(c.options[c.answer]))
            for ((i, card) in c.options.withIndex()) assertEquals(card.label.toInt(), worth(card))
            assertTrue(c.options.indices.filter { it != c.answer }.none { worth(c.options[it]) == price })
        }
    }

    @Test
    fun `change and sharing come out right`() {
        for (s in 0L until 300L) {
            val share = PictureFactory.share(3, s)
            val bats = share.scene.first { it.art == "mini_bat_small" }.count
            val berries = share.scene.first { it.art == "item_berry" }.count
            assertEquals(berries, bats * share.options[share.answer].label.toInt())
            val change = PictureFactory.money(4, s)
            val have = change.scene.single().count
            assertTrue(have - change.options[change.answer].label.toInt() in 1 until have)
        }
    }

    @Test
    fun `a treasure map route stays on the map and ends somewhere new`() {
        for (level in 1..5) for (s in 0L until 300L) {
            val c = PictureFactory.map(level, s)
            assertTrue(c.endRow in 0 until c.rows && c.endCol in 0 until c.cols, "L$level seed $s: the route leaves the map")
            assertTrue(c.endRow != c.startRow || c.endCol != c.startCol, "the route ends where it began")
            assertEquals(c.answer, c.endRow * c.cols + c.endCol)
            // No step takes the child off the map part-way, either.
            var r = c.startRow
            var col = c.startCol
            for (m in c.moves) for (k in 1..m.steps) {
                r += m.way.dRow
                col += m.way.dCol
                assertTrue(r in 0 until c.rows && col in 0 until c.cols)
            }
            assertEquals(c.level >= 4, c.compass)
        }
    }

    @Test
    fun `tell it back asks for the real order of the trip`() {
        val all = com.littledungeon.engine.rpg.content.Content.locations.map { PictureFactory.Spot(it.name, it.theme) }
        for (level in 1..5) for (s in 0L until 200L) {
            val trail = all.shuffled(kotlin.random.Random(s)).take(6)
            val c = PictureFactory.recall(level, s, trail, all)!!
            val picked = c.options[c.answer].label
            val spots = trail.map { it.name }
            val anchor = c.scene.firstOrNull()?.art?.removePrefix("scene_")?.let { art -> trail.first { it.art == art }.name }
            when {
                anchor == null -> assertEquals(spots[1], picked)
                Voice.caption(c.prompt).startsWith("Where did you go before") -> assertEquals(spots[spots.indexOf(anchor) - 1], picked)
                else -> assertEquals(spots[spots.indexOf(anchor) + 1], picked)
            }
            assertEquals(c.options.size, c.options.map { it.label }.toSet().size)
        }
        assertTrue(PictureFactory.recall(1, 1, all.take(2), all) == null, "a trip of two places is too short to ask about")
    }

    @Test
    fun `a journey ends with remembering the trip, once`() {
        val j = com.littledungeon.engine.rpg.run.Journey(5, com.littledungeon.engine.rpg.hero.Hero(), com.littledungeon.engine.rpg.learn.SkillBook(), com.littledungeon.engine.rpg.world.WorldMemory(), com.littledungeon.engine.util.Clock { 0L })
        val r = kotlin.random.Random(5)
        var recaps = 0
        var guard = 0
        while (!j.finished && guard++ < 5000) {
            val b = j.beat
            if (b is com.littledungeon.engine.rpg.run.Beat.Ask && (b.challenge as? PictureChallenge)?.kind == "recall") recaps++
            j.reply(
                when (b) {
                    is com.littledungeon.engine.rpg.run.Beat.Ask -> com.littledungeon.engine.rpg.run.Reply.Solved(1, 0, 1)
                    is com.littledungeon.engine.rpg.run.Beat.Choose -> com.littledungeon.engine.rpg.run.Reply.Picked(r.nextInt(b.options.size))
                    is com.littledungeon.engine.rpg.run.Beat.Travel -> com.littledungeon.engine.rpg.run.Reply.Picked(b.routes.indexOfFirst { it.marked }.coerceAtLeast(0))
                    is com.littledungeon.engine.rpg.run.Beat.Roll -> com.littledungeon.engine.rpg.run.Reply.Rolled(false, 1)
                    else -> com.littledungeon.engine.rpg.run.Reply.Next
                },
            )
        }
        assertTrue(j.finished)
        assertEquals(1, recaps)
    }
}
