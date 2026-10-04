package com.littledungeon.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.littledungeon.engine.model.Speech
import com.littledungeon.engine.model.Voice
import com.littledungeon.engine.model.Who
import com.littledungeon.engine.rpg.run.Fx
import com.littledungeon.feedback.FeedbackLog
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * Reads the story aloud. Every sentence is a recording made by the build in the Kokoro voice
 * (decision #45), or made once on the phone and kept; story sounds ("[creak]") are sound
 * effects (decision #47). The dragon's name is said by the narrator inside its sentence
 * (decision #46). [speak] returns when the words have been heard.
 *
 * [preparing] is true while the next sentence is still being made, for the loading bubble;
 * [speaking] is true only while words are actually heard, and [level] says how loud they are
 * right now, so mouths move with the sound.
 */
class Narrator(context: Context, private val name: () -> String) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val bank = VoiceBank(context)
    private val speaker = Speaker()

    private val generation = AtomicInteger(0)
    private val _speaking = MutableStateFlow(false)
    private val _preparing = MutableStateFlow(false)

    /** True while words are heard; the characters move their mouths along. */
    val speaking: StateFlow<Boolean> = _speaking.asStateFlow()

    /** True while waiting for a sentence that is still being made. */
    val preparing: StateFlow<Boolean> = _preparing.asStateFlow()

    private val _who = MutableStateFlow<Who?>(null)

    /** Who is speaking right now: only that character moves their mouth. Null when nobody is. */
    val speakingAs: StateFlow<Who?> = _who.asStateFlow()

    private val _magic = MutableSharedFlow<Fx>(extraBufferCapacity = 8)

    /** Magic to show: emitted as the sentence or sound that describes it starts. */
    val magic: SharedFlow<Fx> = _magic.asSharedFlow()

    /** How loud the voice is at this moment, 0..1. */
    fun level(): Float = speaker.level()

    @Volatile
    private var current: Job? = null

    /** Clips being found or made, shared by speech and look-ahead, kept until used. */
    private val pending = ConcurrentHashMap<Voice.Piece, Deferred<Pcm?>>()

    init {
        // A name typed on the phone means some sentences must be made here; get the voice ready.
        if (name() != Voice.DEFAULT_NAME) scope.launch(Dispatchers.Default) { bank.warmUp() }
    }

    /**
     * Says everything in order and returns when it has been heard. A newer [speak], [blurt] or
     * [stop] cuts it off; then this waits until the voice is quiet again, so whoever is waiting on
     * these words (the next scene, the answer buttons) never starts over the top of someone still
     * talking. True if it was said to the end.
     */
    suspend fun speak(speech: List<Speech>): Boolean {
        val mine = generation.incrementAndGet()
        current?.cancel()
        var job: Job? = null
        coroutineScope {
            job = launch { play(Voice.pieces(speech, name()), mine) }
            current = job
            job?.join()
        }
        val whole = generation.get() == mine
        if (!whole) {
            FeedbackLog.note("voice", "cut off: ${Voice.caption(speech, name())}")
            while (true) {
                val other = current ?: break
                if (other === job || !other.isActive) break
                other.join()
            }
        }
        return whole
    }

    suspend fun speak(text: String): Boolean = speak(Speech.of(text))

    /** Says something short right away, like the count when a coin is tapped. Interrupts. */
    fun blurt(text: String) {
        scope.launch { speak(text) }
    }

    /**
     * Starts finding or making these words now, so they play without a pause when their turn
     * comes. Called for the next scenes while the current one plays.
     */
    fun prepare(speech: List<Speech>) {
        // Branches not taken leave clips behind; let go of finished ones now and then.
        if (pending.size > MAX_PENDING) pending.entries.removeIf { it.value.isCompleted }
        Voice.pieces(speech, name()).forEach { clip(it) }
    }

    private companion object {
        const val MAX_PENDING = 60
    }

    fun stop() {
        generation.incrementAndGet()
        current?.cancel()
        speaker.stopNow()
        if (ttsOk) tts.stop()
    }

    fun shutdown() {
        stop()
        tts.shutdown()
        speaker.release()
        scope.cancel()
    }

    /** Tells the screen what is about to be heard, so magic shows as it is described. */
    private fun announce(piece: Voice.Piece) {
        when (piece) {
            is Voice.Piece.Say -> {
                FeedbackLog.note("voice", "${piece.who.tag}: ${piece.text}")
                Fx.forSentence(piece.text)?.let { _magic.tryEmit(it) }
            }
            is Voice.Piece.Sound -> {
                FeedbackLog.note("sound", piece.id)
                Fx.forSound(piece.id)?.let { _magic.tryEmit(it) }
            }
        }
    }

    private fun clip(piece: Voice.Piece): Deferred<Pcm?> = pending.getOrPut(piece) {
        scope.async(Dispatchers.Default) {
            when (piece) {
                is Voice.Piece.Say -> bank.say(piece.text, piece.who)
                is Voice.Piece.Sound -> bank.sound(piece.id)
            }
        }
    }

    private suspend fun play(pieces: List<Voice.Piece>, mine: Int) {
        // Ask for every piece at once: the first plays as soon as it's ready, the rest catch up.
        val clips = pieces.map { it to clip(it) }
        try {
            for ((piece, deferred) in clips) {
                if (generation.get() != mine) return
                if (!deferred.isCompleted) {
                    _preparing.value = true
                }
                val pcm = try {
                    deferred.await()
                } finally {
                    _preparing.value = false
                }
                pending.remove(piece)
                when {
                    pcm != null -> {
                        val voice = piece is Voice.Piece.Say
                        announce(piece)
                        if (voice) {
                            _speaking.value = true
                            _who.value = (piece as Voice.Piece.Say).who
                        }
                        try {
                            speaker.play(pcm, voice)
                        } finally {
                            _speaking.value = false
                            _who.value = null
                        }
                    }
                    piece is Voice.Piece.Say -> {
                        announce(piece)
                        sayWithPhoneVoice(Voice.plain(piece.text), piece.who)
                    }
                }
                if (piece is Voice.Piece.Sound) delay(120)
            }
        } finally {
            // Look-ahead clips stay; this speech's leftovers are dropped if it was cut off.
            if (generation.get() != mine) clips.forEach { (p, d) -> if (d.isCompleted) pending.remove(p) }
        }
    }

    // ------------------------------------------------------------- the phone's own voice, a last resort

    private val ttsReady = CompletableDeferred<Boolean>()
    private val waiting = ConcurrentHashMap<String, CompletableDeferred<Unit>>()

    @Volatile
    private var ttsOk = false

    private val tts: TextToSpeech = TextToSpeech(context.applicationContext) { status ->
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale.US
            tts.setSpeechRate(0.9f)
        }
        ttsOk = status == TextToSpeech.SUCCESS
        ttsReady.complete(ttsOk)
    }.also {
        it.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
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

    private suspend fun sayWithPhoneVoice(text: String, who: Who) {
        if (!ttsReady.await()) {
            delay(text.length * 55L) // no voice at all: leave time to look at the screen
            return
        }
        val id = UUID.randomUUID().toString()
        val done = CompletableDeferred<Unit>()
        waiting[id] = done
        _speaking.value = true
        _who.value = who
        try {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, id)
            withTimeoutOrNull(3_000L + text.length * 150L) { done.await() }
        } finally {
            _speaking.value = false
            _who.value = null
            waiting.remove(id)
        }
    }
}
