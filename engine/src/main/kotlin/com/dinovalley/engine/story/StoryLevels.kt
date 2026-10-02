package com.dinovalley.engine.story

/**
 * How hard each read of the story is. One number drives every page, so the rules stay easy to
 * explain: get nearly everything right on the first try and the next read is a step harder.
 */
data class StoryLevel(
    val level: Int,
    val countingLevel: Int,
    val numeralMax: Int,
    val numeralChoices: Int,
    val letters: String,
    val letterChoices: Int,
)

object StoryLevels {
    private const val EARLY_LETTERS = "ABCDEMOST"
    private const val ALL_LETTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"

    val all = listOf(
        StoryLevel(1, countingLevel = 1, numeralMax = 3, numeralChoices = 2, letters = EARLY_LETTERS, letterChoices = 2),
        StoryLevel(2, countingLevel = 2, numeralMax = 5, numeralChoices = 3, letters = EARLY_LETTERS, letterChoices = 3),
        StoryLevel(3, countingLevel = 3, numeralMax = 10, numeralChoices = 3, letters = ALL_LETTERS, letterChoices = 3),
        StoryLevel(4, countingLevel = 4, numeralMax = 10, numeralChoices = 4, letters = ALL_LETTERS, letterChoices = 4),
        StoryLevel(5, countingLevel = 5, numeralMax = 20, numeralChoices = 4, letters = ALL_LETTERS, letterChoices = 4),
    )

    const val FIRST = 2

    fun level(n: Int): StoryLevel = all[(n - 1).coerceIn(0, all.lastIndex)]

    /**
     * The rule between reads: all questions right on the first try moves up a level; two or more
     * that needed help moves down. Anything else stays put.
     */
    fun next(current: Int, questions: Int, firstTryCorrect: Int): Int {
        val missed = questions - firstTryCorrect
        val next = when {
            questions > 0 && missed == 0 -> current + 1
            missed >= 2 -> current - 1
            else -> current
        }
        return next.coerceIn(1, all.size)
    }
}
