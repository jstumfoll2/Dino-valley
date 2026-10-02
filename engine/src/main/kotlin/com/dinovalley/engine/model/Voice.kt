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
    fun spoken(text: String): String = text.replace(Regex("\\b[A-Z]{2,}\\b")) { it.value.lowercase() }

    /** One thing to play: a sentence to say, or a sound effect. */
    sealed interface Piece {
        data class Say(val text: String) : Piece
        data class Sound(val id: String) : Piece
    }

    /**
     * The pieces to play in order. The dragon's [name] is said by the narrator inside its
     * sentence, so it flows like every other word.
     */
    fun pieces(speech: List<Speech>, name: String = DEFAULT_NAME): List<Piece> {
        val out = mutableListOf<Piece>()
        val text = StringBuilder()
        fun flush() {
            sentences(text.toString()).forEach { out += Piece.Say(spoken(it)) }
            text.clear()
        }
        for (part in speech) {
            when (part) {
                is Speech.Words -> text.glue(part.text)
                Speech.Name -> text.glue(name)
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
                is Speech.Sound -> Unit
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
    fun key(spokenSentence: String): String {
        val digest = MessageDigest.getInstance("SHA-1").digest("$VOICE_ID|$spokenSentence".toByteArray())
        return digest.take(8).joinToString("") { "%02x".format(it) }
    }

    /** Changes when the narrator's voice settings change, so every clip is made again. */
    const val VOICE_ID = "kokoro-en-v0_19/sid1/speed0.9"

    /** The default name of the baby dragon, said by the narrator until the child records one. */
    const val DEFAULT_NAME = "Sparky"
}
