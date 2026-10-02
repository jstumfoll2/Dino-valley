package com.dinovalley.data

import android.content.Context
import androidx.core.content.edit
import com.dinovalley.engine.story.StoryLevels

/** What the game remembers between reads, kept on the phone only. */
class Progress(context: Context) {
    private val prefs = context.getSharedPreferences("progress", Context.MODE_PRIVATE)

    var storyLevel: Int
        get() = prefs.getInt(KEY_LEVEL, StoryLevels.FIRST)
        set(value) = prefs.edit { putInt(KEY_LEVEL, value) }

    var storiesRead: Int
        get() = prefs.getInt(KEY_READS, 0)
        set(value) = prefs.edit { putInt(KEY_READS, value) }

    private companion object {
        const val KEY_LEVEL = "story_level"
        const val KEY_READS = "stories_read"
    }
}
