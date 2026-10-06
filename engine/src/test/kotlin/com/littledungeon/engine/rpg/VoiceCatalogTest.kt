package com.littledungeon.engine.rpg

import com.littledungeon.engine.model.Speech
import com.littledungeon.engine.model.Voice
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VoiceCatalogTest {
    @Test
    fun `the catalog covers adventures it never played`() {
        val (catalog, _) = VoiceCatalog.speech()
        val (other, _) = VoiceCatalog.speech(runs = 1500, seed = 99)
        val missing = other - catalog
        assertTrue(missing.isEmpty(), "not recorded: ${missing.take(60)}")
    }

    @Test
    fun `the name is said inside its sentence and sounds are their own pieces`() {
        val pieces = Voice.pieces(Speech.of("Thank you, {name}! [creak] The door opens."), "Rex")
        assertEquals(listOf(Voice.Piece.Say("Thank you, Rex!"), Voice.Piece.Sound("creak"), Voice.Piece.Say("The door opens.")), pieces)
        assertEquals("Look at the BLUE door, Rex!", Voice.caption(Speech.of("Look at the BLUE door, {name}!"), "Rex"))
        assertEquals(listOf(Voice.Piece.Say("Look at the blue door.")), Voice.pieces(Speech.of("Look at the BLUE door.")))
    }

    @Test
    fun `clip names match the build script`() {
        // scripts/render-voice.py: hashlib.sha1(f"{VOICE_ID}|{sentence}").hexdigest()[:16]
        assertEquals("0bf130af87c18ae9", Voice.key("Hello, Sparky!"))
    }
}
