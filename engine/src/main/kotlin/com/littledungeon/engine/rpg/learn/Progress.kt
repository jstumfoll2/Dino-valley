package com.littledungeon.engine.rpg.learn

/** How a skill is going lately compared with before. Words, never grades. */
enum class Trend { NEW, GETTING_EASIER, STEADY, NEEDS_TIME }

/** One skill as a grown-up sees it: where the child is, how much they have done, and whether it is getting easier. */
data class SkillProgress(val skill: Skill, val level: Int, val answered: Int, val recentFirstTry: Double?, val trend: Trend)

/**
 * What the challenge log says about each skill. The last [RECENT] puzzles are compared with the ones before them (first-try share at any
 * level): up by a fifth is getting easier, down by a fifth needs time. Practice puzzles (a level below the child's) are left out, as they are
 * for the levels.
 */
fun progressOf(records: List<ChallengeRecord>, book: SkillBook): List<SkillProgress> = Skill.entries.map { skill ->
    val mine = records.filter { it.skill == skill && it.level >= book.level(skill) - 1 }
    val recent = mine.takeLast(RECENT)
    val before = mine.dropLast(RECENT)
    fun share(rs: List<ChallengeRecord>) = rs.count { it.firstTry }.toDouble() / rs.size
    val trend = when {
        mine.size < MIN_FOR_TREND -> Trend.NEW
        before.size < MIN_FOR_TREND / 2 -> Trend.STEADY
        share(recent) - share(before) >= 0.2 -> Trend.GETTING_EASIER
        share(before) - share(recent) >= 0.2 -> Trend.NEEDS_TIME
        else -> Trend.STEADY
    }
    SkillProgress(skill, book.level(skill), mine.size, recent.takeIf { it.isNotEmpty() }?.let(::share), trend)
}

private const val RECENT = 8
private const val MIN_FOR_TREND = 6
