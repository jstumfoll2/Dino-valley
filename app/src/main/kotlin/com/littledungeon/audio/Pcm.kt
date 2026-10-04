package com.littledungeon.audio

import android.content.res.AssetManager
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/** Mono audio, samples in -1..1. Every sound the game plays goes through here, so all of it can be loudness-matched. */
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

    /**
     * Higher (or lower) and a little faster (or slower), for the small and the big characters:
     * the same samples played as if they were recorded at a different rate. Matches the build's
     * recorder (scripts/render-voice.py), so a sentence made on the phone sounds like the packed ones.
     */
    fun pitched(factor: Float): Pcm {
        if (abs(factor - 1f) < 0.001f || samples.isEmpty()) return this
        return Pcm(samples, (rate * factor).toInt()).at(rate)
    }

    /** The same sound at another sample rate (straight-line resampling, fine for speech). */
    fun at(newRate: Int): Pcm {
        if (newRate == rate || samples.isEmpty()) return this
        val n = (samples.size.toLong() * newRate / rate).toInt()
        val step = rate.toDouble() / newRate
        return Pcm(
            FloatArray(n) { i ->
                val x = i * step
                val k = x.toInt().coerceAtMost(samples.size - 1)
                val f = (x - k).toFloat()
                samples[k] * (1 - f) + samples[minOf(k + 1, samples.size - 1)] * f
            },
            newRate,
        )
    }

    /**
     * How loud each 1/50 s is, 0..1 against the loudest moment: what the characters' mouths
     * follow while this plays.
     */
    val envelope: FloatArray by lazy {
        val window = maxOf(1, rate / ENVELOPE_RATE)
        val out = FloatArray((samples.size + window - 1) / window)
        for (w in out.indices) {
            var s = 0.0
            val from = w * window
            val to = minOf(samples.size, from + window)
            for (k in from until to) s += samples[k] * samples[k]
            out[w] = sqrt(s / maxOf(1, to - from)).toFloat()
        }
        val top = out.maxOrNull()?.takeIf { it > 0f } ?: 1f
        for (i in out.indices) out[i] = out[i] / top
        out
    }

    /** Kept on the phone as 16-bit sound, half the size of the float samples. */
    fun write(file: File) {
        file.parentFile?.mkdirs()
        val tmp = File(file.path + ".tmp")
        DataOutputStream(BufferedOutputStream(FileOutputStream(tmp))).use { out ->
            out.writeInt(rate)
            out.writeInt(samples.size)
            val bytes = ByteBuffer.allocate(samples.size * 2).order(ByteOrder.LITTLE_ENDIAN)
            for (v in samples) bytes.putShort((v.coerceIn(-1f, 1f) * 32767).toInt().toShort())
            out.write(bytes.array())
        }
        tmp.renameTo(file)
    }

    companion object {
        const val TARGET_RMS = 0.11f

        const val ENVELOPE_RATE = 50

        /** Reads a sound file into mono samples. Null if the phone can't decode it. */
        fun decode(file: File): Pcm? = runCatching { decodeOrThrow { setDataSource(file.path) } }.getOrNull()

        /** Reads a sound packed in the app (stored uncompressed, see build.gradle.kts). */
        fun decode(assets: AssetManager, path: String): Pcm? = runCatching {
            assets.openFd(path).use { fd -> decodeOrThrow { setDataSource(fd.fileDescriptor, fd.startOffset, fd.length) } }
        }.getOrNull()

        /** Reads a sound saved by [write]. */
        fun read(file: File): Pcm? = runCatching {
            DataInputStream(BufferedInputStream(FileInputStream(file))).use { input ->
                val rate = input.readInt()
                val n = input.readInt()
                val bytes = ByteArray(n * 2)
                input.readFully(bytes)
                val shorts = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
                Pcm(FloatArray(n) { shorts.get(it) / 32768f }, rate)
            }
        }.getOrNull()

        private fun decodeOrThrow(source: MediaExtractor.() -> Unit): Pcm? {
            val extractor = MediaExtractor()
            extractor.source()
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
            var out = FloatArray(1 shl 16)
            var size = 0
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
                                    if (size == out.size) out = out.copyOf(out.size * 2)
                                    out[size++] = frame / channels
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
            return if (size == 0) null else Pcm(out.copyOf(size), rate)
        }
    }
}

