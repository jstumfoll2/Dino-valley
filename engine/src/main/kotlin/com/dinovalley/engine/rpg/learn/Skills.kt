package com.dinovalley.engine.rpg.learn

import com.dinovalley.engine.rpg.hero.Attribute

/** What the learning engine tracks. Each skill feeds one hero attribute. */
enum class Skill(val attribute: Attribute) {
    COUNTING(Attribute.CLEVERNESS),
    NUMBERS(Attribute.CLEVERNESS),
    ADDITION(Attribute.CLEVERNESS),
    COLORS(Attribute.MAGIC),
    PATTERNS(Attribute.MAGIC),
    LETTERS(Attribute.WISDOM),
    TRACING(Attribute.MAGIC),
    MAPS(Attribute.WISDOM),
    MEMORY(Attribute.WISDOM),
    RECIPES(Attribute.MAGIC),
}

/**
 * One finished challenge, kept on the phone. This is the data a small on-device model could
 * learn from later (brief: "Future machine learning"); for now the rules below read it.
 */
data class ChallengeRecord(
    val skill: Skill,
    val kind: String,
    val level: Int,
    val tries: Int,
    val hintsUsed: Int,
    val millis: Long,
    val atMillis: Long,
    val seed: Long,
) {
    val firstTry: Boolean get() = tries == 1
}

/**
 * The child's level in every skill, 1..5. One rule, easy to explain: two first-try wins in a
 * row move a skill up; a challenge that needed two hints (three or more tries) moves it down.
 */
data class SkillBook(
    val levels: Map<Skill, Int> = emptyMap(),
    val streaks: Map<Skill, Int> = emptyMap(),
    val lastPracticed: Map<Skill, Long> = emptyMap(),
) {
    fun level(skill: Skill): Int = levels[skill] ?: START

    fun record(r: ChallengeRecord): SkillBook {
        val streak = if (r.firstTry) (streaks[r.skill] ?: 0) + 1 else 0
        val level = level(r.skill)
        val (newLevel, newStreak) = when {
            streak >= 2 -> (level + 1) to 0
            r.tries >= GLOW_TRY -> (level - 1) to 0
            else -> level to streak
        }
        return copy(
            levels = levels + (r.skill to newLevel.coerceIn(1, MAX)),
            streaks = streaks + (r.skill to newStreak),
            lastPracticed = lastPracticed + (r.skill to r.atMillis),
        )
    }

    companion object {
        const val START = 1
        const val MAX = 5

        /** Answering on this try means two hints were needed (see Coach). */
        const val GLOW_TRY = 3
    }
}
