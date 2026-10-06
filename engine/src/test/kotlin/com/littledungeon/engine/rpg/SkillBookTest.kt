package com.littledungeon.engine.rpg

import com.littledungeon.engine.rpg.learn.ChallengeRecord
import com.littledungeon.engine.rpg.learn.Skill
import com.littledungeon.engine.rpg.learn.SkillBook
import kotlin.test.Test
import kotlin.test.assertEquals

/** The mastery rule: levels move on what a child shows, never on a lucky streak or a bad day. */
class SkillBookTest {
    private val skill = Skill.COUNTING
    private var clock = 0L

    private fun win(level: Int) = ChallengeRecord(skill, "c", level, 1, 0, 1000, clock++, 0)
    private fun help(level: Int) = ChallengeRecord(skill, "c", level, 2, 1, 1000, clock++, 0)
    private fun fail(level: Int) = ChallengeRecord(skill, "c", level, 1, 0, 1000, clock++, 0, failed = true)

    private fun SkillBook.play(level: Int, vararg results: (Int) -> ChallengeRecord): SkillBook =
        results.fold(this) { book, r -> book.record(r(book.level(skill).coerceAtLeast(level))) }

    @Test
    fun `two wins in a row do not move a level, five good ones do`() {
        var book = SkillBook()
        book = book.record(win(1)).record(win(1))
        assertEquals(1, book.level(skill), "two wins is a streak, not evidence")
        book = book.record(win(1)).record(win(1))
        assertEquals(1, book.level(skill), "four puzzles are not yet enough to judge")
        book = book.record(win(1))
        assertEquals(2, book.level(skill), "five first-try wins at a level is")
    }

    @Test
    fun `a failed one-try puzzle is a miss, not a try that went well enough`() {
        var book = SkillBook()
        repeat(4) { book = book.record(win(1)) }
        book = book.record(fail(1))
        assertEquals(1, book.level(skill), "4 of 5 is 80 percent, under the 85 needed")
        assertEquals(0, book.streaks[skill], "a failure ends the streak even though tries says 1")
        // Three more wins make 7 of the last 8 with the last three right.
        repeat(3) { book = book.record(win(1)) }
        assertEquals(2, book.level(skill))
    }

    @Test
    fun `needing help is not a first-try win`() {
        var book = SkillBook()
        repeat(5) { book = book.record(help(1)) }
        assertEquals(1, book.level(skill))
        assertEquals(0, book.streaks[skill])
    }

    @Test
    fun `three misses in a row step down at once, and the window starts again`() {
        var book = SkillBook(levels = mapOf(skill to 3))
        book = book.record(fail(3)).record(fail(3))
        assertEquals(3, book.level(skill))
        book = book.record(help(3))
        assertEquals(2, book.level(skill))
        assertEquals(emptyList(), book.recent[skill], "a fresh window at the new level")
    }

    @Test
    fun `mostly missing over a window steps down even without a run of misses`() {
        var book = SkillBook(levels = mapOf(skill to 4))
        // win, miss, win, miss, miss: 2 of 5 is under 60 percent but never three misses in a row.
        for (r in listOf(win(4), fail(4), win(4), fail(4))) book = book.record(r)
        assertEquals(4, book.level(skill))
        book = book.record(fail(4))
        assertEquals(3, book.level(skill))
    }

    @Test
    fun `a child who only guesses never climbs`() {
        // One right in four is about what pointing at pictures at random gives.
        var book = SkillBook()
        val pattern = listOf(false, false, true, false)
        repeat(60) { i -> book = book.record(if (pattern[i % 4]) win(book.level(skill)) else fail(book.level(skill))) }
        assertEquals(1, book.level(skill))
    }

    @Test
    fun `a warm-up puzzle below the level is practice, not evidence`() {
        var book = SkillBook(levels = mapOf(skill to 3))
        repeat(10) { book = book.record(win(2)) }
        assertEquals(3, book.level(skill))
        assertEquals(emptyList(), book.recent[skill] ?: emptyList())
        assertEquals(true, book.lastPracticed[skill] != null, "but it still counts as practiced for the dungeon deal")
        // And missing one doesn't push the level down.
        book = book.record(fail(2)).record(fail(2)).record(fail(2))
        assertEquals(3, book.level(skill))
    }

    @Test
    fun `levels stay between one and five`() {
        var book = SkillBook(levels = mapOf(skill to 5))
        repeat(20) { book = book.record(win(5)) }
        assertEquals(5, book.level(skill))
        var low = SkillBook()
        repeat(20) { low = low.record(fail(1)) }
        assertEquals(1, low.level(skill))
    }
}
