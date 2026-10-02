package com.dinovalley.engine.story

/**
 * Judges a pick between pictures (a stone, a bush) and decides how much help comes next:
 * first a gentle "try again", then only the right choice and one other stay.
 */
object ChoiceJudge {
    data class Verdict<T>(val correct: Boolean, val keep: List<T>?)

    /** [tryNumber] is 1 for the first try. [keep] is null while every choice stays. */
    fun <T> judge(choices: List<T>, answer: T, chosen: T, tryNumber: Int): Verdict<T> {
        if (chosen == answer) return Verdict(true, null)
        if (tryNumber < 2 || choices.size <= 2) return Verdict(false, null)
        val other = choices.firstOrNull { it != answer && it != chosen } ?: chosen
        return Verdict(false, choices.filter { it == answer || it == other })
    }
}
