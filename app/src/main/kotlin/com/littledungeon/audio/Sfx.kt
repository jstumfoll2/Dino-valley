package com.littledungeon.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool

/**
 * Quick game sounds that play on top of the narrator: a pop on every tap, a chime for a right
 * answer, a soft "uh-oh" for a wrong one (decision #47). The files are in assets/sfx.
 */
class Sfx(context: Context) {
    private val pool = SoundPool.Builder()
        .setMaxStreams(4)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()

    private val ids: Map<String, Int> = runCatching {
        context.assets.list("sfx").orEmpty().filter { it.endsWith(".ogg") }.associate { file ->
            file.removeSuffix(".ogg") to context.assets.openFd("sfx/$file").use { pool.load(it, 1) }
        }
    }.getOrDefault(emptyMap())

    /** Plays a sound by name, if this build has it. Quieter than the voice, so words stay clear. */
    fun play(name: String, volume: Float = 0.7f) {
        val id = ids[name] ?: return
        pool.play(id, volume, volume, 1, 0, 1f)
    }

    fun release() = pool.release()
}
