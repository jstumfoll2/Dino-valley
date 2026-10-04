package com.littledungeon.audio

import android.content.Context
import com.littledungeon.engine.model.Who
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
 * sherpa-onnx (decision #43). Almost every sentence is recorded by the build ahead of time
 * (decision #45); the model only speaks the few the build couldn't know, like sentences with a
 * dragon name typed on the phone. It is loaded only when first needed and uses two cores, so
 * the phone stays cool.
 */
class KokoroVoice(private val context: Context) {
    private val thread = Executors.newSingleThreadExecutor { Thread(it, "kokoro") }.asCoroutineDispatcher()
    private var tts: OfflineTts? = null
    private var tried = false

    /** Loads the model once. False when it isn't in this build or the phone can't run it. */
    suspend fun load(): Boolean = withContext(thread) {
        if (tried) return@withContext tts != null
        tried = true
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
                    numThreads = 2,
                ),
            )
            tts = OfflineTts(context.assets, config)
            true
        }.getOrDefault(false)
    }

    /** The words as sound in [who]'s voice, at the shared loudness. Null if the voice can't load. */
    suspend fun say(text: String, who: Who = Who.NARRATOR): Pcm? {
        if (!load()) return null
        return withContext(thread) {
            val voice = tts ?: return@withContext null
            val audio = runCatching { voice.generate(text, sid = who.sid, speed = who.speed) }.getOrNull() ?: return@withContext null
            Pcm(audio.samples, audio.sampleRate).pitched(who.pitch).normalized()
        }
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

    }
}
