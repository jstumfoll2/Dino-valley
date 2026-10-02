package com.dinovalley.audio

import android.content.Context
import android.util.LruCache
import com.dinovalley.engine.model.Voice
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * Where every spoken sentence comes from, fastest first: sentences already in memory; the
 * recordings the build packed into the app (decision #45); ones made on this phone before and
 * kept; and only then the Kokoro model, whose result is kept for next time.
 */
class VoiceBank(context: Context) {
    private val assets = context.assets
    private val kokoro = KokoroVoice(context)
    private val made = File(context.filesDir, "voice")
    private val memory = LruCache<String, Pcm>(MEMORY)
    private val packed: Set<String> = runCatching { assets.list(PACKED).orEmpty().map { it.substringBefore('.') }.toSet() }.getOrDefault(emptySet())
    private val sounds = ConcurrentHashMap<String, Pcm>()
    private val inFlight = ConcurrentHashMap<String, CompletableDeferred<Pcm?>>()

    /** True when the sentence can play without the model making it first. */
    fun ready(sentence: String): Boolean {
        val key = Voice.key(sentence)
        return memory.get(key) != null || key in packed || File(made, key).exists()
    }

    /** Loads the model in the background, for when sentences will need making (a typed name). */
    suspend fun warmUp() {
        kokoro.load()
    }

    /** The sentence as sound, or null if there is no way to say it (no recording, no model). */
    suspend fun say(sentence: String): Pcm? {
        val key = Voice.key(sentence)
        memory.get(key)?.let { return it }
        val mine = CompletableDeferred<Pcm?>()
        val other = inFlight.putIfAbsent(key, mine)
        if (other != null) return other.await()
        return try {
            val pcm = find(key) ?: kokoro.say(sentence)?.also { fresh -> withContext(Dispatchers.IO) { runCatching { fresh.write(File(made, key)) } } }
            pcm?.let { memory.put(key, it) }
            mine.complete(pcm)
            pcm
        } catch (e: Throwable) {
            mine.complete(null)
            throw e
        } finally {
            inFlight.remove(key)
        }
    }

    private suspend fun find(key: String): Pcm? = withContext(Dispatchers.IO) {
        if (key in packed) Pcm.decode(assets, "$PACKED/$key.ogg")?.at(Speaker.RATE)?.let { return@withContext it }
        File(made, key).takeIf { it.exists() }?.let { Pcm.read(it) }
    }

    /** A story sound effect ("[creak]"), or null if this build has none by that name. */
    suspend fun sound(id: String): Pcm? {
        sounds[id]?.let { return it }
        return withContext(Dispatchers.IO) { Pcm.decode(assets, "sfx/$id.ogg")?.at(Speaker.RATE) }?.also { sounds[id] = it }
    }

    private companion object {
        const val PACKED = "voice"
        const val MEMORY = 40
    }
}
