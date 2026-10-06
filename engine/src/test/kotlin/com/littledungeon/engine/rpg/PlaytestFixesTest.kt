package com.littledungeon.engine.rpg

import com.littledungeon.engine.model.Speech
import com.littledungeon.engine.model.Voice
import com.littledungeon.engine.model.Who
import com.littledungeon.engine.rpg.learn.ChallengeFactory
import com.littledungeon.engine.rpg.run.Fx
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** What the first playtest found that still applies: speech by character, letter sounds, magic you can see, longer answer lists. */
class PlaytestFixesTest {
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
}
