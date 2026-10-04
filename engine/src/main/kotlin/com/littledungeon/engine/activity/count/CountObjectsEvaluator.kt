package com.littledungeon.engine.activity.count

import com.littledungeon.engine.model.ChildResponse
import com.littledungeon.engine.model.CountObjectsInstance
import com.littledungeon.engine.model.Evaluation
import com.littledungeon.engine.model.Hint
import kotlin.math.abs

/**
 * Judges an answer and picks the next hint. Help grows with each wrong try:
 * look again, then count together, then only two choices remain.
 */
object CountObjectsEvaluator {

    /** [tryNumber] counts this answer: 1 for the first try. */
    fun evaluate(instance: CountObjectsInstance, response: ChildResponse, tryNumber: Int): Evaluation {
        val chosen = (response as? ChildResponse.NumberChosen)?.value
        if (chosen == instance.answer) return Evaluation(correct = true, nextHint = null)
        val hint = when (tryNumber) {
            1 -> Hint.TryAgain
            2 -> Hint.CountTogether
            else -> Hint.NarrowChoices(narrowed(instance, chosen))
        }
        return Evaluation(correct = false, nextHint = hint)
    }

    private fun narrowed(instance: CountObjectsInstance, chosen: Int?): List<Int> {
        val wrong = instance.choices
            .filter { it != instance.answer && it != chosen }
            .minByOrNull { abs(it - instance.answer) }
            ?: chosen
            ?: instance.choices.first { it != instance.answer }
        return listOf(instance.answer, wrong).sorted()
    }
}
