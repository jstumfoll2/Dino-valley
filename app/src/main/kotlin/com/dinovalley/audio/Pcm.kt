package com.dinovalley.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import kotlinx.coroutines.delay
import java.io.File
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/** Mono audio, samples in -1..1. Every voice the game plays goes through here, so all of it can be loudness-matched. */
class Pcm(val samples: FloatArray, val rate: Int) {
    val seconds: Float get() = samples.size.toFloat() / rate

    /**
     * Brings the voice to one loudness: the speaking parts (not the pauses) are scaled to
     * [TARGET_RMS], without letting the loudest peak clip.
     */
    fun normalized(): Pcm {
        val window = rate / 50
        var sum = 0.0
        var count = 0
        var peak = 0f
        var i = 0
        val gate = 0.02f
        while (i < samples.size) {
            val end = min(samples.size, i + window)
            var s = 0.0
            for (k in i until end) s += samples[k] * samples[k]
            val rms = sqrt(s / (end - i)).toFloat()
            if (rms > gate) {
                sum += s
                count += end - i
            }
            for (k in i until end) peak = max(peak, abs(samples[k]))
            i = end
        }
        if (count == 0 || peak == 0f) return this
        val rms = sqrt(sum / count).toFloat()
        val gain = min(TARGET_RMS / rms, 0.98f / peak)
        return Pcm(FloatArray(samples.size) { samples[it] * gain }, rate)
    }

    /** Cuts the quiet before and after the words, which a held-down record button always adds. */
    fun trimmed(): Pcm {
        val window = rate / 50
        val peak = samples.maxOfOrNull { abs(it) } ?: return this
        if (peak == 0f) return this
        val gate = peak * 0.08f
        fun loud(at: Int): Boolean {
            val end = min(samples.size, at + window)
            var s = 0.0
            for (k in at until end) s += samples[k] * samples[k]
            return sqrt(s / max(1, end - at)).toFloat() > gate
        }
        var start = 0
        while (start < samples.size && !loud(start)) start += window
        var end = samples.size
        while (end > start && !loud(max(start, end - window))) end -= window
        val pad = rate / 25
        val from = max(0, start - pad)
        val to = min(samples.size, end + pad)
        return if (to - from < rate / 10) this else Pcm(samples.copyOfRange(from, to), rate)
    }

    /** Plays to the end, or stops right away if the caller is cancelled. */
    suspend fun play() {
        if (samples.isEmpty()) return
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build(),
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                    .setSampleRate(rate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build(),
            )
            .setTransferMode(AudioTrack.MODE_STATIC)
            .setBufferSizeInBytes(samples.size * 4)
            .build()
        try {
            track.write(samples, 0, samples.size, AudioTrack.WRITE_BLOCKING)
            track.play()
            val frames = samples.size
            val limit = System.currentTimeMillis() + (seconds * 1000).toLong() + 1500
            while (track.playbackHeadPosition < frames && System.currentTimeMillis() < limit) delay(20)
        } finally {
            runCatching { track.stop() }
            track.release()
        }
    }

    companion object {
        const val TARGET_RMS = 0.11f

        /** Reads a recorded clip (the dragon's name) into mono samples. Null if the phone can't decode it. */
        fun decode(file: File): Pcm? = runCatching { decodeOrThrow(file) }.getOrNull()

        private fun decodeOrThrow(file: File): Pcm? {
            val extractor = MediaExtractor()
            extractor.setDataSource(file.path)
            val track = (0 until extractor.trackCount).firstOrNull {
                extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
            } ?: return null
            extractor.selectTrack(track)
            val format = extractor.getTrackFormat(track)
            var rate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            var channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            val codec = MediaCodec.createDecoderByType(format.getString(MediaFormat.KEY_MIME)!!)
            codec.configure(format, null, null, 0)
            codec.start()
            val out = ArrayList<Float>()
            val info = MediaCodec.BufferInfo()
            var inputDone = false
            var outputDone = false
            var guard = 0
            try {
                while (!outputDone && guard++ < 20_000) {
                    if (!inputDone) {
                        val i = codec.dequeueInputBuffer(10_000)
                        if (i >= 0) {
                            val buf = codec.getInputBuffer(i)!!
                            val n = extractor.readSampleData(buf, 0)
                            if (n < 0) {
                                codec.queueInputBuffer(i, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                                inputDone = true
                            } else {
                                codec.queueInputBuffer(i, 0, n, extractor.sampleTime, 0)
                                extractor.advance()
                            }
                        }
                    }
                    val o = codec.dequeueOutputBuffer(info, 10_000)
                    when {
                        o == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                            rate = codec.outputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                            channels = codec.outputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                        }
                        o >= 0 -> {
                            val buf = codec.getOutputBuffer(o)!!
                            buf.position(info.offset)
                            buf.limit(info.offset + info.size)
                            val shorts = buf.order(ByteOrder.nativeOrder()).asShortBuffer()
                            var frame = 0f
                            var c = 0
                            while (shorts.hasRemaining()) {
                                frame += shorts.get() / 32768f
                                if (++c == channels) {
                                    out += frame / channels
                                    frame = 0f
                                    c = 0
                                }
                            }
                            codec.releaseOutputBuffer(o, false)
                            if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) outputDone = true
                        }
                    }
                }
            } finally {
                codec.stop()
                codec.release()
                extractor.release()
            }
            return if (out.isEmpty()) null else Pcm(out.toFloatArray(), rate)
        }
    }
}

