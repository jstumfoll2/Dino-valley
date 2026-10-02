package com.dinovalley.audio

import android.content.Context
import android.media.MediaPlayer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.dinovalley.engine.model.Speech
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.resume

/**
 * Reads the story aloud. Words use the phone's text-to-speech as a placeholder until the family's
 * recordings arrive (PROJECT_DECISIONS.md #16, #22); the dino's name is the child's own recording
 * when there is one. [speak] returns when the words are finished, so pages can wait for them.
 */
class Narrator(context: Context, private val nameClip: () -> File?) {
    private val ready = CompletableDeferred<Boolean>()
    private val waiting = ConcurrentHashMap<String, CompletableDeferred<Unit>>()
    private val active = AtomicInteger(0)

    /** Bumped by every new [speak] and by [stop]; an older reading notices and steps aside. */
    private val generation = AtomicInteger(0)
    private val _speaking = MutableStateFlow(false)

    /** True while words or the name are playing; the dino moves its mouth along. */
    val speaking: StateFlow<Boolean> = _speaking.asStateFlow()

    private val tts: TextToSpeech = TextToSpeech(context.applicationContext) { status ->
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale.US
            tts.setSpeechRate(0.9f)
            tts.setPitch(1.15f)
        }
        ttsOk = status == TextToSpeech.SUCCESS
        ready.complete(ttsOk)
    }

    @Volatile
    private var ttsOk = false

    @Volatile
    private var player: MediaPlayer? = null

    init {
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String) = Unit
            override fun onDone(utteranceId: String) = finish(utteranceId)

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String) = finish(utteranceId)
            override fun onStop(utteranceId: String, interrupted: Boolean) = finish(utteranceId)
        })
    }

    private fun finish(id: String) {
        waiting.remove(id)?.complete(Unit)
    }

    /** Says everything in order and returns when done. Cancelling the caller stops the voice. */
    suspend fun speak(speech: List<Speech>) {
        val mine = generation.incrementAndGet()
        _speaking.value = active.incrementAndGet() > 0
        try {
            for (part in speech) {
                if (generation.get() != mine) return
                when (part) {
                    is Speech.Words -> say(part.text)
                    Speech.Name -> sayName()
                }
            }
        } catch (e: CancellationException) {
            if (generation.get() == mine) stop()
            throw e
        } finally {
            _speaking.value = active.decrementAndGet() > 0
        }
    }

    suspend fun speak(text: String) = speak(Speech.of(text))

    /** Says something short right away without waiting, like the count when an egg is tapped. */
    fun blurt(text: String) {
        if (ttsOk) tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "blurt")
    }

    fun stop() {
        generation.incrementAndGet()
        tts.stop()
        player?.let { runCatching { it.stop() } }
    }

    private suspend fun say(text: String) {
        if (!ready.await()) {
            delay(text.length * 55L) // no voice on this phone: leave time to look at the page
            return
        }
        val id = UUID.randomUUID().toString()
        val done = CompletableDeferred<Unit>()
        waiting[id] = done
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, id)
        withTimeoutOrNull(3_000L + text.length * 150L) { done.await() }
        waiting.remove(id)
    }

    private suspend fun sayName() {
        val clip = nameClip()?.takeIf { it.exists() && it.length() > 0 }
        if (clip == null) {
            say(DEFAULT_NAME)
            return
        }
        val mp = runCatching {
            MediaPlayer().apply {
                setDataSource(clip.path)
                prepare()
            }
        }.getOrNull() ?: return say(DEFAULT_NAME)
        player = mp
        try {
            suspendCancellableCoroutine { cont ->
                mp.setOnCompletionListener { if (cont.isActive) cont.resume(Unit) }
                mp.setOnErrorListener { _, _, _ -> if (cont.isActive) cont.resume(Unit); true }
                cont.invokeOnCancellation { runCatching { mp.stop() } }
                mp.start()
            }
        } finally {
            player = null
            mp.release()
        }
    }

    fun shutdown() {
        stop()
        tts.shutdown()
    }

    companion object {
        const val DEFAULT_NAME = "Rex"
    }
}
