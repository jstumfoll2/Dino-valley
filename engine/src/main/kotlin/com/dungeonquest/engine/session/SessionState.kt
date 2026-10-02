package com.dungeonquest.engine.session

import com.dungeonquest.engine.model.ChildResponse
import com.dungeonquest.engine.model.CountObjectsInstance
import com.dungeonquest.engine.model.Hint
import com.dungeonquest.engine.model.ItemOutcome

/**
 * Every moment of a round. Each state is one beat of the storyboard: the UI decides how it
 * looks and sounds, and tells the session when it has finished showing it.
 */
sealed interface SessionState {
    val itemNumber: Int
    val itemsInRound: Int

    /** The character introduces the question; with [demonstrate] it counts first, showing how. */
    data class Introducing(
        val item: CountObjectsInstance,
        val demonstrate: Boolean,
        override val itemNumber: Int,
        override val itemsInRound: Int,
    ) : SessionState

    data class AwaitingAnswer(
        val item: CountObjectsInstance,
        val choices: List<Int>,
        val tries: Int,
        val hintsUsed: Int,
        val startedAtMillis: Long,
        override val itemNumber: Int,
        override val itemsInRound: Int,
    ) : SessionState

    data class Helping(
        val item: CountObjectsInstance,
        val hint: Hint,
        val wrongAnswer: Int?,
        val choices: List<Int>,
        val tries: Int,
        val hintsUsed: Int,
        val startedAtMillis: Long,
        override val itemNumber: Int,
        override val itemsInRound: Int,
    ) : SessionState

    data class Celebrating(
        val item: CountObjectsInstance,
        val outcome: ItemOutcome,
        override val itemNumber: Int,
        override val itemsInRound: Int,
    ) : SessionState

    data class RoundComplete(
        val outcomes: List<ItemOutcome>,
        override val itemNumber: Int,
        override val itemsInRound: Int,
    ) : SessionState
}

sealed interface SessionEvent {
    data object IntroFinished : SessionEvent
    data class Answered(val response: ChildResponse) : SessionEvent
    data object HintShown : SessionEvent
    data object CelebrationFinished : SessionEvent
}
