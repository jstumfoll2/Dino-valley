package com.dinovalley.engine.story

import com.dinovalley.engine.model.Speech

import com.dinovalley.engine.activity.count.CountObjectsGenerator
import com.dinovalley.engine.model.SpriteId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class StoryTest {
    private val story = LostEggsStory(CountObjectsGenerator(listOf(SpriteId("egg")), emptyList()))

    @Test
    fun `the name placeholder splits narration into words and the name`() {
        assertEquals(
            listOf(Speech.Words("Thank you,"), Speech.Name, Speech.Words("!")),
            Speech.of("Thank you, {name}!"),
        )
        assertEquals(listOf(Speech.Name, Speech.Words("looked.")), Speech.of("{name} looked."))
    }

    @Test
    fun `the same seed tells the same story and every answer is among its choices`() {
        for (level in StoryLevels.all) {
            for (seed in 1L..40L) {
                val book = story.write(level, seed)
                assertEquals(book, story.write(level, seed))
                assertEquals(4, book.questionCount)
                for (page in book.pages) {
                    when (val c = page.challenge) {
                        is Challenge.FindNumeral -> {
                            assertTrue(c.target in c.choices && c.target in 1..level.numeralMax)
                            assertEquals(level.numeralChoices, c.choices.toSet().size)
                        }
                        is Challenge.FindLetter -> {
                            assertTrue(c.target in c.choices && c.target in level.letters)
                            assertEquals(level.letterChoices, c.choices.toSet().size)
                        }
                        is Challenge.CountEggs -> assertTrue(c.question.answer in c.question.choices)
                        else -> Unit
                    }
                }
            }
        }
    }

    @Test
    fun `difficulty moves up after a clean read and down after two misses`() {
        assertEquals(3, StoryLevels.next(2, questions = 4, firstTryCorrect = 4))
        assertEquals(2, StoryLevels.next(2, questions = 4, firstTryCorrect = 3))
        assertEquals(1, StoryLevels.next(2, questions = 4, firstTryCorrect = 2))
        assertEquals(5, StoryLevels.next(5, questions = 4, firstTryCorrect = 4))
        assertEquals(1, StoryLevels.next(1, questions = 4, firstTryCorrect = 0))
    }

    @Test
    fun `a wrong pick narrows the choices only from the second try`() {
        val choices = listOf('B', 'M', 'S')
        val first = ChoiceJudge.judge(choices, 'B', 'M', tryNumber = 1)
        assertEquals(false, first.correct)
        assertNull(first.keep)
        val second = ChoiceJudge.judge(choices, 'B', 'M', tryNumber = 2)
        assertEquals(listOf('B', 'S'), second.keep)
        assertTrue(ChoiceJudge.judge(choices, 'B', 'B', tryNumber = 3).correct)
    }
}
