package com.dinovalley.engine.rpg.learn

/**
 * The hint ladder (brief: "Failure philosophy"). A miss is a story event, never a dead end:
 * first something funny happens and the child tries again; on the second miss only the right
 * picture and one other stay; after a third miss the right one glows.
 */
object Coach {
    data class Verdict(val correct: Boolean, val keep: List<Int>?, val glow: Int?) {
        /** How many hints this verdict gave, for the challenge record. */
        val hints: Int get() = (if (keep != null) 1 else 0) + (if (glow != null) 1 else 0)
    }

    /** [tryNumber] is 1 for the first answer. */
    fun judge(optionCount: Int, answer: Int, chosen: Int, tryNumber: Int): Verdict {
        if (chosen == answer) return Verdict(true, null, null)
        if (tryNumber <= 1) return Verdict(false, null, null)
        val other = (0 until optionCount).firstOrNull { it != answer && it != chosen } ?: chosen
        val keep = if (optionCount > 2) listOf(answer, other).sorted() else null
        return Verdict(false, keep, if (tryNumber >= 3) answer else null)
    }

    fun judge(c: PickOne, chosen: Int, tryNumber: Int): Verdict = judge(c.optionCount, c.answer, chosen, tryNumber)
}
