package com.littledungeon.engine.rpg.learn

import com.littledungeon.engine.rpg.hero.Attribute

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
    SORTING(Attribute.WISDOM),
    SKIP_COUNTING(Attribute.CLEVERNESS),
    PUZZLES(Attribute.MAGIC),
    RHYMES(Attribute.WISDOM),
    MONEY(Attribute.CLEVERNESS),
    SHARING(Attribute.CLEVERNESS),
    STORY(Attribute.WISDOM),
}

/**
 * One finished challenge, kept on the phone. This is the data a small on-device model could
 * learn from later (brief: "Future machine learning"); for now the rules below read it.
 * [failed] is a one-try puzzle that was not solved: it is a miss, whatever [tries] says.
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
    val failed: Boolean = false,
) {
    /** Solved on the first go, with nothing to help: the only kind of result that counts as knowing it. */
    val firstTry: Boolean get() = tries == 1 && !failed
}

/**
 * The child's level in every skill, 1..5, moved by what they actually show (decision #54: "first-try
 * correctness at the current level is the only input to level changes"):
 *
 * - **Up one** once at least [MIN_ITEMS] puzzles have been seen at this level, at least [PROMOTE_AT] of the
 *   last [WINDOW] were right first time, and the last [PROMOTE_STREAK] in a row were.
 * - **Down one** after [STRUGGLE] puzzles in a row that were not solved first time, or when at least
 *   [MIN_ITEMS] have been seen and fewer than [DEMOTE_BELOW] of the window were right.
 * - After any change the window starts again, so a level is never left on the strength of the last one.
 * - A puzzle below the child's level (a warm-up) is practice, not evidence.
 *
 * A one-try puzzle that was failed is a miss like any other; a child who guesses never climbs.
 */
data class SkillBook(
    val levels: Map<Skill, Int> = emptyMap(),
    /** First-try wins in a row, per skill. */
    val streaks: Map<Skill, Int> = emptyMap(),
    val lastPracticed: Map<Skill, Long> = emptyMap(),
    /** First-try results at the current level, oldest first, at most [WINDOW] of them. */
    val recent: Map<Skill, List<Boolean>> = emptyMap(),
    /** Puzzles in a row that were not solved first time, per skill. */
    val misses: Map<Skill, Int> = emptyMap(),
) {
    fun level(skill: Skill): Int = levels[skill] ?: START

    fun record(r: ChallengeRecord): SkillBook {
        val skill = r.skill
        val level = level(skill)
        val stamped = lastPracticed + (skill to r.atMillis)
        if (r.level < level) return copy(lastPracticed = stamped)

        val ok = r.firstTry
        val window = ((recent[skill] ?: emptyList()) + ok).takeLast(WINDOW)
        val streak = if (ok) (streaks[skill] ?: 0) + 1 else 0
        val missRun = if (ok) 0 else (misses[skill] ?: 0) + 1
        val accuracy = window.count { it }.toDouble() / window.size
        val step = when {
            level > 1 && missRun >= STRUGGLE -> -1
            level > 1 && window.size >= MIN_ITEMS && accuracy < DEMOTE_BELOW -> -1
            level < MAX && window.size >= MIN_ITEMS && accuracy >= PROMOTE_AT && streak >= PROMOTE_STREAK -> 1
            else -> 0
        }
        return if (step != 0) {
            copy(
                levels = levels + (skill to (level + step)), streaks = streaks + (skill to 0), lastPracticed = stamped,
                recent = recent + (skill to emptyList()), misses = misses + (skill to 0),
            )
        } else {
            copy(
                streaks = streaks + (skill to streak), lastPracticed = stamped,
                recent = recent + (skill to window), misses = misses + (skill to missRun),
            )
        }
    }

    companion object {
        const val START = 1
        const val MAX = 5

        /** How many recent results the rule looks at. */
        const val WINDOW = 8

        /** The fewest puzzles at a level before it can change on accuracy. */
        const val MIN_ITEMS = 5
        const val PROMOTE_AT = 0.85
        const val PROMOTE_STREAK = 3
        const val DEMOTE_BELOW = 0.60

        /** Puzzles in a row not solved first time that drop a level at once: frustration costs more than boredom. */
        const val STRUGGLE = 3
    }
}
