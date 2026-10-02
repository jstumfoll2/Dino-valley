package com.dinovalley.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.dinovalley.engine.model.Speech
import com.dinovalley.engine.rpg.learn.Words
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * Reads the story aloud. The voice is Kokoro, a natural-sounding speech model that runs on the
 * phone (decision #43); the phone's own text-to-speech steps in if the model can't load. The
 * dragon's name is the child's own recording when there is one, trimmed and brought to the same
 * loudness as the narrator. [speak] returns when the words are finished.
 */
class Narrator(context: Context, private val nameClip: () -> File?) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val kokoro = KokoroVoice(context)
    private val kokoroReady = CompletableDeferred<Boolean>()

    /** Bumped by every new [speak], [blurt] and [stop]; an older reading notices and steps aside. */
    private val generation = AtomicInteger(0)
    private val active = AtomicInteger(0)
    private val _speaking = MutableStateFlow(false)

    /** True while words or the name are playing; the characters move their mouths along. */
    val speaking: StateFlow<Boolean> = _speaking.asStateFlow()

    @Volatile
    private var playing: Job? = null

    // ------------------------------------------------------------- the phone's voice, as a fallback

    private val ttsReady = CompletableDeferred<Boolean>()
    private val waiting = ConcurrentHashMap<String, CompletableDeferred<Unit>>()

    @Volatile
    private var ttsOk = false

    private val tts: TextToSpeech = TextToSpeech(context.applicationContext) { status ->
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale.US
            tts.setSpeechRate(0.9f)
            tts.setPitch(1.1f)
        }
        ttsOk = status == TextToSpeech.SUCCESS
        ttsReady.complete(ttsOk)
    }

    init {
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String) = Unit
            override fun onDone(utteranceId: String) = finish(utteranceId)

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String) = finish(utteranceId)
            override fun onStop(utteranceId: String, interrupted: Boolean) = finish(utteranceId)
        })
        scope.launch {
            val ok = kokoro.load()
            kokoroReady.complete(ok)
            // Warm up the little words said on every tap, so they come out instantly.
            if (ok) (1..12).forEach { kokoro.say(spoken("${Words.capital(it)}!")) }
        }
    }

    private fun finish(id: String) {
        waiting.remove(id)?.complete(Unit)
    }

    /** Says everything in order and returns when done. Cancelling the caller stops the voice. */
    suspend fun speak(speech: List<Speech>) {
        val mine = generation.incrementAndGet()
        playing?.cancel()
        _speaking.value = active.incrementAndGet() > 0
        try {
            if (kokoroReady.await()) speakNatural(speech, mine) else speakFallback(speech, mine)
        } catch (e: CancellationException) {
            if (generation.get() == mine) stop()
            throw e
        } finally {
            _speaking.value = active.decrementAndGet() > 0
        }
    }

    suspend fun speak(text: String) = speak(Speech.of(text))

    /** Says something short right away, like the count when a coin is tapped. Interrupts. */
    fun blurt(text: String) {
        val mine = generation.incrementAndGet()
        playing?.cancel()
        if (ttsOk && !kokoroReady.isCompleted) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "blurt")
            return
        }
        scope.launch {
            if (kokoroReady.await()) {
                val pcm = kokoro.say(spoken(text)) ?: return@launch
                if (generation.get() == mine) play(pcm)
            } else if (ttsOk) {
                tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "blurt")
            }
        }
    }

    fun stop() {
        generation.incrementAndGet()
        playing?.cancel()
        if (ttsOk) tts.stop()
    }

    fun shutdown() {
        stop()
        tts.shutdown()
        scope.cancel()
    }

    // ------------------------------------------------------------- Kokoro

    /**
     * Sentences are made one after another while the previous one plays, so long narration
     * starts quickly and flows without gaps.
     */
    private suspend fun speakNatural(speech: List<Speech>, mine: Int) = coroutineScope {
        val pieces: List<String?> = speech.flatMap { part ->
            when (part) {
                is Speech.Words -> sentences(part.text)
                Speech.Name -> listOf(null)
            }
        }
        val clips = Channel<Pcm?>(capacity = 2)
        val maker = launch {
            for (piece in pieces) {
                if (generation.get() != mine) break
                clips.send(if (piece == null) name() else kokoro.say(spoken(piece)))
            }
            clips.close()
        }
        for (clip in clips) {
            if (generation.get() != mine) break
            if (clip != null) play(clip)
        }
        maker.cancel()
    }

    /** Plays one clip; [stop] and newer speech cut it off. */
    private suspend fun play(pcm: Pcm) {
        val job = scope.launch { pcm.play() }
        playing = job
        job.join()
    }

    private var nameCache: Pair<Long, Pcm?>? = null

    /** The child's recording, decoded once per recording, or the default name in the narrator's voice. */
    private suspend fun name(): Pcm? {
        val clip = nameClip()?.takeIf { it.exists() && it.length() > 0 }
            ?: return kokoro.say(DEFAULT_NAME)
        val stamp = clip.lastModified()
        nameCache?.takeIf { it.first == stamp }?.let { return it.second ?: kokoro.say(DEFAULT_NAME) }
        val pcm = withContext(Dispatchers.Default) { Pcm.decode(clip)?.trimmed()?.normalized() }
        nameCache = stamp to pcm
        return pcm ?: kokoro.say(DEFAULT_NAME)
    }

    // ------------------------------------------------------------- the phone's own voice

    private suspend fun speakFallback(speech: List<Speech>, mine: Int) {
        for (part in speech) {
            if (generation.get() != mine) return
            when (part) {
                is Speech.Words -> say(part.text)
                Speech.Name -> {
                    val clip = nameClip()?.takeIf { it.exists() && it.length() > 0 }
                    val pcm = clip?.let { withContext(Dispatchers.Default) { Pcm.decode(it)?.trimmed()?.normalized() } }
                    if (pcm != null) play(pcm) else say(DEFAULT_NAME)
                }
            }
        }
    }

    private suspend fun say(text: String) {
        if (!ttsReady.await()) {
            delay(text.length * 55L) // no voice on this phone: leave time to look at the screen
            return
        }
        val id = UUID.randomUUID().toString()
        val done = CompletableDeferred<Unit>()
        waiting[id] = done
        tts.speak(spoken(text), TextToSpeech.QUEUE_FLUSH, null, id)
        withTimeoutOrNull(3_000L + text.length * 150L) { done.await() }
        waiting.remove(id)
    }

    companion object {
        const val DEFAULT_NAME = "Sparky"

        /** Splits narration into sentences, keeping their end marks. */
        fun sentences(text: String): List<String> =
            text.split(Regex("(?<=[.!?])\\s+")).map { it.trim() }.filter { it.isNotEmpty() }

        /**
         * Shouted words (the BLUE door) are in capitals so grown-ups see the stress on screen;
         * speech engines can read capitals as letters, so they are said in lower case.
         */
        fun spoken(text: String): String = text.replace(Regex("\\b[A-Z]{2,}\\b")) { it.value.lowercase() }
    }
}
