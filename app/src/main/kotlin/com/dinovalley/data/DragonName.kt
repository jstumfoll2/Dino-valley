package com.dinovalley.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.dinovalley.engine.model.Voice
import java.io.File

/**
 * The baby dragon's name, typed by a grown-up and said by the narrator in the same voice as
 * every other word (decision #46). It stays on this phone.
 */
class DragonName(context: Context) {
    private val prefs = context.getSharedPreferences("dragon", Context.MODE_PRIVATE)

    var name by mutableStateOf(prefs.getString(KEY, null)?.takeIf { it.isNotBlank() } ?: Voice.DEFAULT_NAME)
        private set

    init {
        // The old recorded name is no longer used; don't keep a child's voice around.
        File(context.filesDir, "dino_name.m4a").delete()
    }

    /** Sets a new name: letters, spaces and hyphens, at most 16 characters. Returns false if nothing is left. */
    fun set(typed: String): Boolean {
        val clean = typed.filter { it.isLetter() || it == ' ' || it == '-' || it == '\'' }.trim().take(16)
        if (clean.isEmpty()) return false
        val proper = clean.split(' ').filter { it.isNotEmpty() }.joinToString(" ") { w -> w.lowercase().replaceFirstChar { it.uppercase() } }
        name = proper
        prefs.edit().putString(KEY, proper).apply()
        return true
    }

    private companion object {
        const val KEY = "name"
    }
}
