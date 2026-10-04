package com.littledungeon.engine.session

import com.littledungeon.engine.activity.count.CountObjectsEvaluator
import com.littledungeon.engine.activity.count.CountObjectsGenerator
import com.littledungeon.engine.activity.count.CountObjectsLevel
import com.littledungeon.engine.model.ChildResponse
import com.littledungeon.engine.model.Ease
import com.littledungeon.engine.model.Hint
import com.littledungeon.engine.model.ItemOutcome
import com.littledungeon.engine.session.SessionEvent.Answered
import com.littledungeon.engine.session.SessionEvent.CelebrationFinished
import com.littledungeon.engine.session.SessionEvent.HintShown
import com.littledungeon.engine.session.SessionEvent.IntroFinished
import com.littledungeon.engine.session.SessionState.AwaitingAnswer
import com.littledungeon.engine.session.SessionState.Celebrating
import com.littledungeon.engine.session.SessionState.Helping
import com.littledungeon.engine.session.SessionState.Introducing
import com.littledungeon.engine.session.SessionState.RoundComplete
import com.littledungeon.engine.util.Clock
import com.littledungeon.engine.util.GameRandom

/**
 * One round of counting questions as a state machine. Plain Kotlin: a test can play a whole round
 * in milliseconds. Events that don't fit the current state are ignored, so a double tap or a late
 * animation callback can't break the round.
 */
class GameSession(
    private val generator: CountObjectsGenerator,
    private val level: CountObjectsLevel,
    private val random: GameRandom,
    private val clock: Clock = Clock.System,
    val itemsInRound: Int = 5,
) {
    private val outcomes = mutableListOf<ItemOutcome>()
    private var nextEase = Ease.NORMAL

    var state: SessionState = newItem(itemNumber = 1)
        private set

    fun onEvent(event: SessionEvent): SessionState {
        state = reduce(state, event)
        return state
    }

    private fun reduce(current: SessionState, event: SessionEvent): SessionState = when {
        current is Introducing && event is IntroFinished -> AwaitingAnswer(
            item = current.item,
            choices = current.item.choices,
            tries = 0,
            hintsUsed = 0,
            startedAtMillis = clock.nowMillis(),
            itemNumber = current.itemNumber,
            itemsInRound = itemsInRound,
        )

        current is AwaitingAnswer && event is Answered -> answer(current, event)

        current is Helping && event is HintShown -> AwaitingAnswer(
            item = current.item,
            choices = current.choices,
            tries = current.tries,
            hintsUsed = current.hintsUsed,
            startedAtMillis = current.startedAtMillis,
            itemNumber = current.itemNumber,
            itemsInRound = itemsInRound,
        )

        current is Celebrating && event is CelebrationFinished ->
            if (current.itemNumber >= itemsInRound) {
                RoundComplete(outcomes.toList(), current.itemNumber, itemsInRound)
            } else {
                newItem(current.itemNumber + 1)
            }

        else -> current
    }

    private fun answer(current: AwaitingAnswer, event: Answered): SessionState {
        val tryNumber = current.tries + 1
        val evaluation = CountObjectsEvaluator.evaluate(current.item, event.response, tryNumber)
        if (evaluation.correct) {
            val outcome = ItemOutcome(
                templateId = current.item.templateId,
                activityType = current.item.activityType,
                skill = current.item.skill,
                level = current.item.level,
                firstTryCorrect = tryNumber == 1,
                tries = tryNumber,
                hintsUsed = current.hintsUsed,
                responseTimeMs = clock.nowMillis() - current.startedAtMillis,
                completedAtMillis = clock.nowMillis(),
                seed = current.item.seed,
            )
            outcomes += outcome
            nextEase = if (outcome.firstTryCorrect) Ease.NORMAL else Ease.GENTLE
            return Celebrating(current.item, outcome, current.itemNumber, itemsInRound)
        }
        val hint = evaluation.nextHint ?: Hint.TryAgain
        return Helping(
            item = current.item,
            hint = hint,
            wrongAnswer = (event.response as? ChildResponse.NumberChosen)?.value,
            choices = (hint as? Hint.NarrowChoices)?.keep ?: current.choices,
            tries = tryNumber,
            hintsUsed = current.hintsUsed + 1,
            startedAtMillis = current.startedAtMillis,
            itemNumber = current.itemNumber,
            itemsInRound = itemsInRound,
        )
    }

    private fun newItem(itemNumber: Int): Introducing = Introducing(
        item = generator.generate(level, nextEase, random.nextSeed()),
        demonstrate = itemNumber == 1,
        itemNumber = itemNumber,
        itemsInRound = itemsInRound,
    )
}
