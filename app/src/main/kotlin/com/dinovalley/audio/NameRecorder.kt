package com.dinovalley.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.io.File

/**
 * Records the child saying their dino's name. The clip stays in the app's private storage and is
 * never sent anywhere (the app has no internet permission at all); uninstalling deletes it.
 */
class NameRecorder(private val context: Context) {
    val clip: File = File(context.filesDir, "dino_name.m4a")
    private val take = File(context.cacheDir, "dino_name_take.m4a")
    private var recorder: MediaRecorder? = null
    private var startedAt = 0L

    /** Whether the dragon has a recorded name (otherwise it is "Sparky"). */
    var hasName by mutableStateOf(clip.exists() && clip.length() > 0)
        private set

    /** Starts listening. Returns false if the microphone could not start. */
    fun start(): Boolean {
        stop()
        val r = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(context) else @Suppress("DEPRECATION") MediaRecorder()
        return try {
            r.setAudioSource(MediaRecorder.AudioSource.MIC)
            r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            r.setAudioSamplingRate(44_100)
            r.setAudioEncodingBitRate(96_000)
            r.setMaxDuration(MAX_MS)
            r.setOutputFile(take.path)
            r.prepare()
            r.start()
            recorder = r
            startedAt = SystemClock.elapsedRealtime()
            true
        } catch (e: Exception) {
            r.release()
            false
        }
    }

    /** Stops listening. Returns true when the take was long enough to keep as the new name. */
    fun stop(): Boolean {
        val r = recorder ?: return false
        recorder = null
        val long = SystemClock.elapsedRealtime() - startedAt >= MIN_MS
        val ok = try {
            r.stop()
            true
        } catch (e: RuntimeException) {
            false // stopped before any sound was captured
        } finally {
            r.release()
        }
        val keep = ok && long && take.length() > 0
        if (keep) {
            take.copyTo(clip, overwrite = true)
            hasName = true
        }
        take.delete()
        return keep
    }

    private companion object {
        const val MIN_MS = 450L
        const val MAX_MS = 6_000
    }
}
