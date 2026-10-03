package com.dinovalley.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext

/**
 * The one loudspeaker every voice line and story sound goes through, one after another. It is
 * a single audio stream that stays open, so the end of a sentence is never clipped and the
 * next one starts without a click. It also knows how loud the current moment is, which the
 * characters' mouths follow.
 */
class Speaker {
    private val track: AudioTrack = AudioTrack.Builder()
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build(),
        )
        .setAudioFormat(
            AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                .setSampleRate(RATE)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build(),
        )
        .setTransferMode(AudioTrack.MODE_STREAM)
        .setBufferSizeInBytes(RATE / 5 * 4)
        .build()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var idle: Job? = null
    private val lock = Mutex()

    @Volatile
    private var current: Pcm? = null

    @Volatile
    private var startFrame = 0L

    @Volatile
    private var talking = false

    /** How loud the voice is right now, 0..1; 0 when nothing (or only a sound effect) plays. */
    fun level(): Float {
        val pcm = current ?: return 0f
        if (!talking) return 0f
        val played = track.playbackHeadPosition.toLong() - startFrame
        if (played < 0) return 0f
        val i = (played * Pcm.ENVELOPE_RATE / RATE).toInt()
        return pcm.envelope.getOrElse(i) { 0f }
    }

    /**
     * Plays [pcm] to the end and returns once it has been heard. Cancelling the caller stops it
     * at once. [voice] marks speech (mouths move) rather than a sound effect.
     */
    suspend fun play(pcm: Pcm, voice: Boolean) {
        val clip = pcm.at(RATE)
        if (clip.samples.isEmpty()) return
        idle?.cancel()
        // One clip at a time: a clip that was cut off finishes silencing before the next starts.
        lock.withLock { playLocked(clip, voice) }
        // Keep the stream open between sentences; close it after a quiet moment to save power.
        idle = scope.launch {
            delay(2000)
            lock.withLock { runCatching { track.pause() } }
        }
    }

    private suspend fun playLocked(clip: Pcm, voice: Boolean) {
        withContext(Dispatchers.Default) {
            try {
                if (track.playState != AudioTrack.PLAYSTATE_PLAYING) track.play()
                startFrame = track.playbackHeadPosition.toLong()
                current = clip
                talking = voice
                var written = 0
                while (written < clip.samples.size) {
                    coroutineContext.ensureActive()
                    val n = minOf(CHUNK, clip.samples.size - written)
                    val w = track.write(clip.samples, written, n, AudioTrack.WRITE_BLOCKING)
                    if (w <= 0) break
                    written += w
                }
                // Wait until it has been heard, not just handed over.
                val end = startFrame + written
                val limit = System.currentTimeMillis() + clip.samples.size * 1000L / RATE + 1000
                while (track.playbackHeadPosition < end && System.currentTimeMillis() < limit) delay(15)
                // The phone's speaker is a little behind the play position; let the last sound out.
                if (voice) delay(TAIL_MILLIS)
            } catch (e: Throwable) {
                stopNow()
                throw e
            } finally {
                talking = false
                current = null
            }
        }
    }

    /** Silences whatever is playing. */
    fun stopNow() {
        talking = false
        current = null
        runCatching {
            track.pause()
            track.flush()
        }
    }

    fun release() {
        scope.cancel()
        runCatching { track.release() }
    }

    companion object {
        /** Kokoro speaks at 24 kHz; everything else is brought to it. */
        const val RATE = 24_000
        private const val CHUNK = RATE / 20
        private const val TAIL_MILLIS = 120L
    }
}
