package com.littledungeon.engine.rpg

import com.littledungeon.engine.model.Voice
import com.littledungeon.engine.rpg.learn.ChallengeFactory
import com.littledungeon.engine.rpg.learn.LetterChallenge
import com.littledungeon.engine.rpg.learn.LetterMode
import com.littledungeon.engine.rpg.learn.Words
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Letters the way a child is taught them: sounds that can really be made, little letters, look-alikes kept apart. */
class PhonicsTest {
    private fun said(c: com.littledungeon.engine.rpg.learn.Challenge) = Voice.caption(c.prompt)

    private fun letters(level: Int, n: Int = 400): List<LetterChallenge> = (1L..n).map { ChallengeFactory.letter(level, it, "") }

    @Test
    fun `held sounds are only the ones that can be made without a vowel on the end`() {
        assertEquals("MSFLNRZV".toSet(), Words.LETTER_SOUNDS.keys, "no vowels and no stop sounds like buh, duh, puh")
        for (level in 1..5) for (c in letters(level)) {
            if (c.mode == LetterMode.SOUND) assertTrue(c.letter in Words.LETTER_SOUNDS.keys && c.options.all { it in Words.LETTER_SOUNDS.keys })
            if (c.mode == LetterMode.FIRST_SOUND) assertTrue(c.letter != 'X' && c.options.none { it == 'X' }, "x-ray does not start with the sound of X")
        }
    }

    @Test
    fun `letter names stay in the game, and little letters are asked for`() {
        assertTrue(letters(1).all { it.mode == LetterMode.NAME } && letters(2).all { it.mode == LetterMode.NAME })
        assertEquals(setOf(LetterMode.MATCH_CASE, LetterMode.SOUND), letters(3).map { it.mode }.toSet())
        assertEquals(setOf(LetterMode.FIRST_SOUND), letters(4).map { it.mode }.toSet())
        assertEquals(LetterMode.entries.filter { it != LetterMode.NAME }.toSet(), letters(5).map { it.mode }.toSet())
        for (c in letters(3).filter { it.mode == LetterMode.MATCH_CASE }) {
            assertTrue(c.letter.isLowerCase() && c.options.all { it.isLowerCase() }, "the answer cards show little letters: ${c.options}")
            assertTrue(c.options.indexOf(c.letter) == c.answer)
            // The big letter is what is said; a lone little letter is read unpredictably by a speech model.
            assertTrue("big ${c.letter.uppercaseChar()}" in said(c), said(c))
        }
    }

    @Test
    fun `letters that look alike are never offered together until level four`() {
        for (level in 1..3) for (c in letters(level).filter { it.mode != LetterMode.FIRST_SOUND || true }) {
            val others = c.options.filter { it != c.letter }
            assertTrue(others.none { Words.confusable(it, c.letter) }, "level $level: $others next to ${c.letter}")
        }
        // At level 4 and up they may meet, which is what makes it harder.
        assertTrue(letters(4, 1500).any { c -> c.options.any { it != c.letter && Words.confusable(it, c.letter) } })
        assertTrue(Words.confusable('b', 'D') && Words.confusable('M', 'w') && !Words.confusable('A', 'B'))
    }

    @Test
    fun `skip counting asks for the number on the pad, not how many are on it`() {
        for (level in 1..5) assertTrue("What number goes on the last lily pad?" in said(ChallengeFactory.skipCount(level, 3L)))
    }
}
