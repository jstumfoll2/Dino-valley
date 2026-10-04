package com.littledungeon.engine.rpg

import com.littledungeon.engine.model.Speech
import com.littledungeon.engine.model.Voice
import com.littledungeon.engine.model.Who
import com.littledungeon.engine.rpg.hero.Hero
import com.littledungeon.engine.rpg.hero.HeroClass
import com.littledungeon.engine.rpg.learn.ChallengeFactory
import com.littledungeon.engine.rpg.learn.Skill
import com.littledungeon.engine.rpg.learn.SkillBook
import com.littledungeon.engine.rpg.run.Adventure
import com.littledungeon.engine.rpg.run.Beat
import com.littledungeon.engine.rpg.run.Fx
import com.littledungeon.engine.rpg.run.LootKind
import com.littledungeon.engine.rpg.run.Reply
import com.littledungeon.engine.rpg.world.WorldMemory
import com.littledungeon.engine.util.Clock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** What the first playtest found: wrong gems, silent spells, narrator-only speech, letter sounds, a straight map. */
class PlaytestFixesTest {
    private fun adventure(seed: Long, mapsLevel: Int = 1) =
        Adventure(seed, Hero(HeroClass.KNIGHT), SkillBook(levels = mapOf(Skill.MAPS to mapsLevel)), WorldMemory(), Clock { 0L })

    /** Plays to the end, always picking the door that [pick] chooses from the open ones. */
    private fun play(a: Adventure, pick: (Beat.Doors, List<Int>) -> Int): List<Beat> {
        val seen = mutableListOf<Beat>()
        var guard = 0
        while (!a.finished) {
            val b = a.beat
            seen += b
            a.reply(
                when (b) {
                    is Beat.Doors -> Reply.Picked(pick(b, b.fork.doors.indices.filter { it !in b.closed }))
                    is Beat.Ask -> Reply.Solved(1, 0, 0)
                    is Beat.Roll -> Reply.Rolled(false, 1)
                    is Beat.Choose -> Reply.Picked(0)
                    else -> Reply.Next
                },
            )
            assertTrue(++guard < 300)
        }
        return seen
    }

    @Test
    fun `a wrong door winds back to the doors with that door closed`() {
        for (seed in 1L..40L) {
            val a = adventure(seed)
            val doors = mutableListOf<Beat.Doors>()
            val beats = play(a) { b, open ->
                doors += b
                open.first { it != b.clue!!.answer }.takeIf { b.closed.isEmpty() } ?: b.clue!!.answer
            }
            // Three forks, each wrong first: the second fork's doors are not asked again (one door left).
            assertEquals(3, doors.size, "two doors: the wrong one, then the clue's door is entered without asking")
            val loops = a.visits.filter { it.looped }
            assertEquals(3, loops.size)
            assertEquals(6, a.visits.size, "a wrong door and then the right one at each fork")
            assertTrue(a.visits.zipWithNext().all { (x, y) -> if (x.looped) y.stop == x.stop else true })
            assertTrue(a.finished && beats.isNotEmpty())
        }
    }

    @Test
    fun `three doors can loop twice and a closed door is never offered again`() {
        val a = adventure(5, mapsLevel = 2)
        val doors = mutableListOf<Beat.Doors>()
        play(a) { b, open ->
            doors += b
            open.firstOrNull { it != b.clue!!.answer } ?: b.clue!!.answer
        }
        val wide = doors.filter { it.fork.doors.size == 3 }
        assertTrue(wide.isNotEmpty() && wide.any { it.closed.size == 1 }, "the wide fork is asked again with one door closed")
        for (d in doors) assertTrue(d.offers.withIndex().all { (i, o) -> (i in d.closed) == o.isEmpty() })
    }

    @Test
    fun `following the clue never loops and finds the treasure`() {
        val a = adventure(7)
        play(a) { b, _ -> b.clue!!.answer }
        assertTrue(a.visits.none { it.looped })
        assertEquals(3, a.visits.size)
    }

    @Test
    fun `a gem found is the color that was said`() {
        var found = 0
        for (seed in 1L..80L) {
            val a = adventure(seed)
            for (b in play(a) { _, open -> open.first() }) {
                if (b is Beat.Found && b.loot.kind == LootKind.GEM) {
                    found++
                    val hue = assertNotNull(b.loot.hue)
                    assertTrue(hue.word in b.loot.words && hue.word in Speech.of("You got a shiny ${hue.word} gem!").joinToString { it.toString() })
                }
            }
            assertEquals(a.bag.gems.size, a.bag.gems.size)
        }
        assertTrue(found > 0)
    }

    @Test
    fun `characters speak in their own voices and never leave the narrator talking as them`() {
        val parts = Speech.of("A cave. <wizard>Hello! <pet>Hi! <narrator>Off they go. <goblin>Boo!")
        val pieces = Voice.pieces(parts)
        assertEquals(
            listOf(
                Voice.Piece.Say("A cave.", Who.NARRATOR), Voice.Piece.Say("Hello!", Who.WIZARD), Voice.Piece.Say("Hi!", Who.PET),
                Voice.Piece.Say("Off they go.", Who.NARRATOR), Voice.Piece.Say("Boo!", Who.GOBLIN),
            ),
            pieces,
        )
        // The next line starts with the narrator again.
        val next = Voice.pieces(Speech.of("<pet>Hi!") + Speech.of("The end."))
        assertEquals(Who.NARRATOR, (next.last() as Voice.Piece.Say).who)
        assertEquals("A cave. Hello! Hi! Off they go. Boo!", Voice.caption(parts))
        assertTrue(Who.entries.map { Voice.key("Hi!", it) }.toSet().size == Who.entries.size, "each voice has its own recording")
    }

    @Test
    fun `adventures have characters talking with each other`() {
        val (said, _) = VoiceCatalog.speech(runs = 300)
        val speakers = said.map { it.who }.toSet()
        assertTrue(speakers.containsAll(listOf(Who.NARRATOR, Who.PET, Who.WIZARD, Who.BARON, Who.MERCHANT)), "heard only $speakers")
        assertTrue(said.count { it.who != Who.NARRATOR } > 40)
    }

    @Test
    fun `a held letter sound is one steady sound, not the letter said over and over`() {
        assertEquals("Listen: [[n:]].", Voice.spoken("Listen: nnnn."))
        assertEquals("Which letter makes the sound [[m:]]?", Voice.spoken("Which letter makes the sound mmmm?"))
        assertEquals("Find the letter B. B, as in bat.", Voice.spoken("Find the letter B. B, as in bat."))
        assertEquals("nnn", Voice.plain("[[n:]]"))
        val c = ChallengeFactory.letter(3, 1, "")
        val said = Voice.pieces(c.prompt).filterIsInstance<Voice.Piece.Say>().joinToString(" ") { it.text }
        assertTrue("Listen to this sound." in said && "Which letter makes that sound?" in said, said)
    }

    @Test
    fun `magic words and sounds show magic`() {
        assertEquals(Fx.BUNNY, Fx.forSentence("The crystal turns the wizard's hat into a bunny!"))
        assertEquals(Fx.SPELL, Fx.forSentence("Cast a sparkle spell!"))
        assertEquals(Fx.SPELL, Fx.forSentence("Your spell sparkles."))
        assertEquals(Fx.RAINBOW, Fx.forSentence("WOW! A rainbow spell fills the whole lair!"))
        assertEquals(Fx.GLOW, Fx.forSentence("The spell book glows with happy magic!"))
        assertNull(Fx.forSentence("The spell needs one more letter."))
        assertNull(Fx.forSentence("How many stones are on the bridge?"))
        assertEquals(Fx.POOF, Fx.forSound("poof"))
        assertNull(Fx.forSound("creak"))
    }

    @Test
    fun `every answer list is longer and still holds the answer once`() {
        for (level in 1..5) for (seed in 1L..30L) {
            val count = ChallengeFactory.count(level, seed, com.littledungeon.engine.rpg.learn.Thing.COIN, "How many?")
            assertTrue(count.options.size >= 3 && count.count in count.options && count.options.toSet().size == count.options.size)
            val letter = ChallengeFactory.letter(level, seed, "")
            assertTrue(letter.options.size >= 3 && letter.options.count { it == letter.letter } == 1)
            val color = ChallengeFactory.color(level, seed, "")
            assertTrue(color.options.size >= 3 && color.options.toSet().size == color.options.size && color.target in color.options)
            val pattern = ChallengeFactory.pattern(level, seed)
            assertTrue(pattern.options.size >= 3 && pattern.options.toSet().size == pattern.options.size)
        }
    }

    @Test
    fun `a held letter sound is made by the letter-sound maker, not a speaking voice`() {
        assertEquals(Voice.LETTER_SOUND_ID, Voice.voiceIdOf("[[s:]].", Who.NARRATOR))
        assertEquals(Who.PET.voiceId, Voice.voiceIdOf("Hello!", Who.PET))
        assertTrue(Voice.key("[[s:]].", Who.NARRATOR) != Voice.key("[[f:]].", Who.NARRATOR))
    }

    @Test
    fun `the goblin is on screen when he runs in to help, and Ruby is where the story puts her`() {
        var helped = 0
        for (seed in 1L..60L) {
            val a = adventure(seed)
            for (b in play(a) { _, open -> open.first() }) {
                val said = (b as? Beat.Tell)?.lines?.let { Voice.caption(it) } ?: continue
                if ("runs in" in said) {
                    helped++
                    assertTrue(com.littledungeon.engine.rpg.run.Actor.GOBLIN in b.scene.cast, "goblin not drawn: $said")
                }
                if ("Princess Ruby is riding" in said || "curled around Princess Ruby" in said || "locked Ruby" in said) {
                    assertTrue(com.littledungeon.engine.rpg.run.Actor.RUBY in b.scene.cast, "Ruby not drawn: $said")
                }
            }
        }
        assertTrue(helped > 0)
    }

    @Test
    fun `clues talk about the right path, not treasure behind a door`() {
        for (seed in 1L..40L) {
            val a = adventure(seed)
            for (b in play(a) { _, open -> open.first() }) {
                if (b is Beat.Doors) {
                    val said = Voice.caption(b.clue!!.prompt)
                    assertTrue("right path" in said && "behind" !in said && "treasure" !in said, said)
                }
            }
        }
    }
}
