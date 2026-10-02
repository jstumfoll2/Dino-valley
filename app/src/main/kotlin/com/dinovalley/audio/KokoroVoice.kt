package com.dinovalley.audio

import android.content.Context
import com.k2fsa.sherpa.onnx.OfflineTts
import com.k2fsa.sherpa.onnx.OfflineTtsConfig
import com.k2fsa.sherpa.onnx.OfflineTtsKokoroModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsModelConfig
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.Executors

/**
 * The narrator's natural voice: the Kokoro speech model, running entirely on the phone through
 * sherpa-onnx (decision #43). The model files are packed into the app by CI
 * (scripts/fetch-voice.sh). Speech is made one sentence at a time on a single background
 * thread, and recent sentences are remembered so repeated lines play instantly.
 */
class KokoroVoice(private val context: Context) {
    private val thread = Executors.newSingleThreadExecutor { Thread(it, "kokoro") }.asCoroutineDispatcher()
    private var tts: OfflineTts? = null
    private val cache = object : LinkedHashMap<String, Pcm>(64, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Pcm>?) = size > CACHE
    }

    /** Loads the model. False when it isn't in this build or the phone can't run it. */
    suspend fun load(): Boolean = withContext(thread) {
        runCatching {
            if (context.assets.list(DIR)?.contains(MODEL) != true) return@runCatching false
            val config = OfflineTtsConfig(
                model = OfflineTtsModelConfig(
                    kokoro = OfflineTtsKokoroModelConfig(
                        model = "$DIR/$MODEL",
                        voices = "$DIR/voices.bin",
                        tokens = "$DIR/tokens.txt",
                        dataDir = copyEspeakData(),
                    ),
                    numThreads = 4,
                ),
            )
            tts = OfflineTts(context.assets, config)
            true
        }.getOrDefault(false)
    }

    /** The words as sound, at the shared loudness. Null if the voice isn't loaded. */
    suspend fun say(text: String): Pcm? = withContext(thread) {
        val voice = tts ?: return@withContext null
        synchronized(cache) { cache[text] }?.let { return@withContext it }
        val audio = runCatching { voice.generate(text, sid = SPEAKER, speed = SPEED) }.getOrNull() ?: return@withContext null
        val pcm = Pcm(audio.samples, audio.sampleRate).normalized()
        synchronized(cache) { cache[text] = pcm }
        pcm
    }

    /**
     * The pronunciation data must be real files, not inside the APK. Copied once per app
     * version.
     */
    private fun copyEspeakData(): String {
        val target = File(context.filesDir, "espeak-ng-data")
        val stamp = File(target, ".version-${appVersion()}")
        if (!stamp.exists()) {
            target.deleteRecursively()
            copyAsset("$DIR/espeak-ng-data", target)
            stamp.createNewFile()
        }
        return target.absolutePath
    }

    private fun copyAsset(path: String, to: File) {
        val children = context.assets.list(path).orEmpty()
        if (children.isEmpty()) {
            to.parentFile?.mkdirs()
            context.assets.open(path).use { input -> to.outputStream().use { input.copyTo(it) } }
        } else {
            to.mkdirs()
            children.forEach { copyAsset("$path/$it", File(to, it)) }
        }
    }

    @Suppress("DEPRECATION")
    private fun appVersion(): Long = context.packageManager.getPackageInfo(context.packageName, 0).versionCode.toLong()

    private companion object {
        const val DIR = "kokoro"
        const val MODEL = "model.int8.onnx"

        /** af_bella: a warm, clear storyteller among Kokoro's English voices. */
        const val SPEAKER = 1

        /** A touch slower than normal speech, for a four-year-old. */
        const val SPEED = 0.9f
        const val CACHE = 160
    }
}
