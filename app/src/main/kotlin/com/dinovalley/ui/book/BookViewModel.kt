package com.dinovalley.ui.book

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.dinovalley.data.Progress
import com.dinovalley.engine.activity.count.CountObjectsGenerator
import com.dinovalley.engine.model.SpriteId
import com.dinovalley.engine.story.Challenge
import com.dinovalley.engine.story.LostEggsStory
import com.dinovalley.engine.story.StoryBook
import com.dinovalley.engine.story.StoryLevels

/**
 * Holds which page is open and remembers how each question went, so the next read can be a
 * little easier or harder (StoryLevels.next). Page -1 is the cover; pages.size is "The End".
 */
class BookViewModel(app: Application) : AndroidViewModel(app) {
    private val progress = Progress(app)
    private val story = LostEggsStory(
        CountObjectsGenerator(sprites = EGG_SPRITES.map(::SpriteId), distractorSprites = emptyList()),
    )
    private val firstTries = mutableMapOf<Int, Boolean>()

    var book: StoryBook by mutableStateOf(newBook())
        private set
    var page by mutableIntStateOf(COVER)
        private set

    val atEnd: Boolean get() = page >= book.pages.size

    fun next() {
        if (atEnd) return
        page += 1
        if (atEnd) finish()
    }

    /** Called once per page when its challenge is done; only the first call per page counts. */
    fun solved(pageIndex: Int, firstTry: Boolean) {
        val c = book.pages.getOrNull(pageIndex)?.challenge
        if (c is Challenge.CountEggs || c is Challenge.FindNumeral || c is Challenge.FindLetter) {
            firstTries.putIfAbsent(pageIndex, firstTry)
        }
    }

    fun readAgain() {
        book = newBook()
        page = 0
    }

    fun toCover() {
        book = newBook()
        page = COVER
    }

    private fun finish() {
        progress.storyLevel = StoryLevels.next(book.level, book.questionCount, firstTries.values.count { it })
        progress.storiesRead += 1
    }

    private fun newBook(): StoryBook {
        firstTries.clear()
        return story.write(StoryLevels.level(progress.storyLevel), System.nanoTime())
    }

    companion object {
        const val COVER = -1
        val EGG_SPRITES = listOf("egg_blue", "egg_green", "egg_orange")
    }
}
