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
}
