package com.littledungeon.engine.rpg

import com.littledungeon.engine.model.Voice
import com.littledungeon.engine.rpg.content.Content
import com.littledungeon.engine.rpg.learn.ChallengeFactory
import com.littledungeon.engine.rpg.learn.Coach
import com.littledungeon.engine.rpg.learn.Thing
import com.littledungeon.engine.rpg.learn.Words
import com.littledungeon.engine.rpg.run.PICK_ONE_SKILLS
import com.littledungeon.engine.rpg.run.RoomLines
import com.littledungeon.engine.rpg.story.Effect
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Spoken teaching after a miss, honest wording, and people whose puzzles belong to their own story. */
class LearningFixesTest {
    @Test
    fun `a missed puzzle is explained, with the right answer, for every pick-one kind`() {
        val count = ChallengeFactory.count(2, 4L, Thing.STONE, "How many?")
        val says = Voice.caption(Coach.explain(count))
        assertTrue(says.startsWith("Let's count them together. One! Two!") && says.endsWith("That makes ${Words.number(count.count)}."), says)
        val add = ChallengeFactory.add(2, 5L, Thing.COIN) { _, _, _ -> "" }
        assertTrue("make ${Words.number(add.total)}" in Voice.caption(Coach.explain(add)))
        for (level in 1..5) for (seed in 1L..40L) {
            val all = listOf(
                ChallengeFactory.count(level, seed, Thing.GEM, ""), ChallengeFactory.numeral(level, seed, ""), ChallengeFactory.add(level, seed, Thing.GEM) { _, _, _ -> "" },
                ChallengeFactory.color(level, seed, ""), ChallengeFactory.pattern(level, seed), ChallengeFactory.letter(level, seed, ""), ChallengeFactory.skipCount(level, seed),
            )
            for (c in all) assertTrue(Coach.explain(c).isNotEmpty(), "no explanation for ${c::class.simpleName}")
        }
        // Hands-on puzzles explain themselves on screen.
        assertTrue(Coach.explain(ChallengeFactory.write(1, 1L, "")).isEmpty())
    }

    @Test
    fun `a room that is one try never says try again`() {
        val lines = RoomLines(Random(1))
        val oops = listOf(
            lines::runeOops, lines::bridgeOops, lines::crystalOops, lines::libraryOops, lines::tunnelOops, lines::mirrorOops,
            lines::storeroomOops, lines::pondOops, lines::mosaicOops, lines::vaultOops,
        )
        for (f in oops) repeat(40) {
            val line = f()
            assertTrue(!Regex("(?i)try (again|another)|count again").containsMatchIn(line), "promises another go: $line")
        }
    }

    @Test
    fun `people who have a puzzle of their own set it up in their own story`() {
        val puzzles = Content.npcs.flatMap { n -> n.nodes.flatMap { it.effects + it.options.flatMap { o -> o.effects } }.filterIsInstance<Effect.Puzzle>().map { n to it } }
        val own = puzzles.filter { it.second.skill != null }
        assertTrue(own.size >= 6, "Henrietta, Hazel, Brogan, Merlo, Rascal and Otto bring their own puzzles")
        for ((npc, p) in own) {
            assertTrue(p.skill in PICK_ONE_SKILLS, "${npc.id}: ${p.skill} is not a pick-one skill")
            assertTrue(!p.ask.isNullOrBlank(), "${npc.id} has no words for their puzzle")
        }
        val hen = Content.npc("henrietta_hen")!!
        assertTrue(Regex("(?i)\\b(one|two|three|four|five|six|seven|eight|nine|ten)\\b").findAll(hen.node("found").says).none(), "her thanks must not claim a number the puzzle did not have")
    }
}
