package com.dinovalley.engine.model

import java.security.MessageDigest

/**
 * How narration is cut up for the voice. The narrator speaks one sentence at a time, and every
 * sentence the game can say is recorded ahead of time by the build (scripts/render-voice.py),
 * so the phone only plays sound files. The build and the app must cut sentences the same way,
 * which is why this lives in the engine.
 */
object Voice {
    /** Splits narration into sentences, keeping their end marks. */
    fun sentences(text: String): List<String> =
        text.split(Regex("(?<=[.!?])\\s+")).map { it.trim() }.filter { it.isNotEmpty() }

    /**
     * Shouted words (the BLUE door) are in capitals so grown-ups see the stress on screen;
     * speech engines can read capitals as letters, so they are said in lower case. Single
     * capital letters (Find the letter B) stay as they are.
     */
    fun spoken(text: String): String =
        text.replace(Regex("\\b[A-Z]{2,}\\b")) { it.value.lowercase() }
            // A held letter sound ("nnnn") is read by speech engines as the letter's name said over
            // and over ("en en en en"); phoneme markup makes it one steady sound.
            .replace(SOUND, "[[$1:]]")

    private val SOUND = Regex("\\b([mnsflrzv])\\1{2,}\\b")

    /** A sentence for an engine that can't read phoneme markup: the sound written out again. */
    fun plain(spoken: String): String = spoken.replace(Regex("\\[\\[([a-z]):\\]\\]")) { it.groupValues[1].repeat(3) }

    /** One thing to play: a sentence to say, or a sound effect. */
    sealed interface Piece {
        data class Say(val text: String, val who: Who = Who.NARRATOR) : Piece
        data class Sound(val id: String) : Piece
    }

    /**
     * The pieces to play in order. The dragon's [name] is said by the narrator inside its
     * sentence, so it flows like every other word.
     */
    fun pieces(speech: List<Speech>, name: String = DEFAULT_NAME): List<Piece> {
        val out = mutableListOf<Piece>()
        val text = StringBuilder()
        var who = Who.NARRATOR
        fun flush() {
            sentences(text.toString()).forEach { out += Piece.Say(spoken(it), who) }
            text.clear()
        }
        for (part in speech) {
            when (part) {
                is Speech.Words -> text.glue(part.text)
                Speech.Name -> text.glue(name)
                is Speech.As -> {
                    flush()
                    who = part.who
                }
                is Speech.Sound -> {
                    flush()
                    out += Piece.Sound(part.id)
                }
            }
        }
        flush()
        return out
    }

    /** The words as text, for the caption: sounds left out, the name filled in. */
    fun caption(speech: List<Speech>, name: String = DEFAULT_NAME): String {
        val text = StringBuilder()
        for (part in speech) {
            when (part) {
                is Speech.Words -> text.glue(part.text)
                Speech.Name -> text.glue(name)
                is Speech.Sound, is Speech.As -> Unit
            }
        }
        return text.toString()
    }

    /** Adds words with a space between, but none before punctuation ("Sparky" + "!" is "Sparky!"). */
    private fun StringBuilder.glue(words: String) {
        if (isNotEmpty() && !endsWith(' ') && words.firstOrNull()?.let { it in ",.!?;:" } != true) append(' ')
        append(words)
    }

    /** The file name of a recorded sentence: a fingerprint of exactly what is said. */
    fun key(spokenSentence: String, who: Who = Who.NARRATOR): String {
        val digest = MessageDigest.getInstance("SHA-1").digest("${voiceIdOf(spokenSentence, who)}|$spokenSentence".toByteArray())
        return digest.take(8).joinToString("") { "%02x".format(it) }
    }

    /** A held letter sound ("[[s:]]."): made from scratch by the build (scripts/letter_sounds.py), not spoken. */
    const val LETTER_SOUND_ID = "letter-sounds-v1"

    private val HELD_SOUND = Regex("^\\[\\[[a-z]:]][.!?]?$")

    /** Which voice makes this sentence: the letter-sound maker for a held sound, otherwise [who]'s. */
    fun voiceIdOf(spokenSentence: String, who: Who): String =
        if (HELD_SOUND.matches(spokenSentence)) LETTER_SOUND_ID else who.voiceId

    /** Changes when the narrator's voice settings change, so every clip is made again. */
    const val VOICE_ID = "kokoro-en-v0_19/sid1/speed0.9"

    /** The default name of the baby dragon, said by the narrator until the child records one. */
    const val DEFAULT_NAME = "Sparky"
}
