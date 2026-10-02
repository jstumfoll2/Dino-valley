package com.dinovalley.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

/**
 * Speaks instructions. For now this uses the phone's built-in text-to-speech as a placeholder;
 * recorded family voice lines replace it (PROJECT_DECISIONS.md #16, #22).
 */
class Narrator(context: Context) : TextToSpeech.OnInitListener {
    private val tts = TextToSpeech(context.applicationContext, this)

    @Volatile
    private var ready = false

    override fun onInit(status: Int) {
        if (status != TextToSpeech.SUCCESS) return
        tts.language = Locale.US
        tts.setSpeechRate(0.9f)
        tts.setPitch(1.15f)
        ready = true
    }

    /** Says [text], cutting off anything still playing unless [queue] is set. */
    fun say(text: String, queue: Boolean = false) {
        if (!ready) return
        val mode = if (queue) TextToSpeech.QUEUE_ADD else TextToSpeech.QUEUE_FLUSH
        tts.speak(text, mode, null, text)
    }

    fun shutdown() {
        tts.stop()
        tts.shutdown()
    }

    companion object {
        private val words = listOf("zero", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten")

        fun numberWord(n: Int): String = words.getOrElse(n) { n.toString() }
    }
}
