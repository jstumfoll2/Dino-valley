package com.dungeonquest.engine.model

/** What the child did. Taps and (later) voice answers produce the same responses. */
sealed interface ChildResponse {
    data class NumberChosen(val value: Int) : ChildResponse
}

/** Which kind of help to give next, from lightest to strongest. */
sealed interface Hint {
    /** "Hmm, let's look again!" with a gentle wiggle. */
    data object TryAgain : Hint

    /** The character counts aloud, highlighting each object. */
    data object CountTogether : Hint

    /** Only these choices remain; the right one glows softly. */
    data class NarrowChoices(val keep: List<Int>) : Hint
}

data class Evaluation(val correct: Boolean, val nextHint: Hint?)

/** One finished question. Every item ends in success; this records how much help it took. */
data class ItemOutcome(
    val templateId: TemplateId,
    val activityType: String,
    val skill: SkillId,
    val level: Int,
    val firstTryCorrect: Boolean,
    val tries: Int,
    val hintsUsed: Int,
    val responseTimeMs: Long,
    val completedAtMillis: Long,
    val seed: Long,
)
