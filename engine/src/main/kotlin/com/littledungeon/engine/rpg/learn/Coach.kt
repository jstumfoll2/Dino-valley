package com.littledungeon.engine.rpg.learn

import com.littledungeon.engine.model.Speech

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

    /**
     * What the narrator says, with the right answer showing, after a puzzle was missed with no try left: not
     * "wrong", but why the right one is right. Puzzles that are done with hands (tracing, sorting, jigsaws,
     * memory) have no spoken explanation; their own help is on screen.
     */
    fun explain(c: Challenge): List<Speech> = when (c) {
        is CountChallenge -> {
            val counted = (1..c.count).joinToString(" ") { "${Words.capital(it)}!" }
            Speech.of("Let's count them together. $counted That makes ${Words.number(c.count)}.")
        }
        is AddChallenge ->
            Speech.of("${Words.capital(c.have)} and ${Words.number(c.more)} make ${Words.number(c.total)}." + if (c.missingAddend) " So it needs ${Words.number(c.more)} more." else "")
        is NumberChallenge -> Speech.of("This is the number ${Words.number(c.number).uppercase()}.")
        is ColorChallenge -> Speech.of("This one is the ${if (c.target.size == GemSize.SMALL) "small " else ""}${c.target.hue.word.uppercase()} one.")
        is PatternChallenge -> Speech.of("See how the pattern goes round and round? This one comes next.")
        is LetterChallenge -> {
            val big = c.letter.uppercaseChar()
            Speech.of(
                when (c.mode) {
                    LetterMode.NAME -> "This is the letter $big, as in ${c.word}."
                    LetterMode.MATCH_CASE -> "This is the little partner of the big $big."
                    LetterMode.SOUND -> "This letter makes the sound ${Words.LETTER_SOUNDS.getValue(big)}."
                    LetterMode.FIRST_SOUND -> "${c.word.uppercase()} starts with the letter $big."
                },
            )
        }
        is SkipCountChallenge -> {
            val by = when (c.step) {
                2 -> "twos"
                3 -> "threes"
                5 -> "fives"
                else -> "tens"
            }
            val said = (1..c.shown).joinToString(", ") { Words.number(c.step * it).uppercase() }
            Speech.of("Count by $by: $said.")
        }
        is PictureChallenge -> c.because
        is GridChallenge -> c.because
        else -> emptyList()
    }
}
