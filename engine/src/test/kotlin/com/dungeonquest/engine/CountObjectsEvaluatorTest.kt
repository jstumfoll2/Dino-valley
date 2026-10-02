package com.dungeonquest.engine

import com.dungeonquest.engine.activity.count.CountObjectsEvaluator
import com.dungeonquest.engine.model.ChildResponse.NumberChosen
import com.dungeonquest.engine.model.CountObjectsInstance
import com.dungeonquest.engine.model.Hint
import com.dungeonquest.engine.model.Scene
import com.dungeonquest.engine.model.SkillId
import com.dungeonquest.engine.model.TemplateId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CountObjectsEvaluatorTest {
    private val question = CountObjectsInstance(
        templateId = TemplateId("t"), skill = SkillId("counting"), level = 2,
        scene = Scene(emptyList()), seed = 1, answer = 3, choices = listOf(2, 3, 4), tapToCount = true,
    )

    @Test
    fun `right answer needs no hint`() {
        val result = CountObjectsEvaluator.evaluate(question, NumberChosen(3), tryNumber = 1)
        assertTrue(result.correct)
        assertNull(result.nextHint)
    }

    @Test
    fun `help grows with each wrong try and ends with two choices`() {
        assertEquals(Hint.TryAgain, CountObjectsEvaluator.evaluate(question, NumberChosen(4), 1).nextHint)
        assertEquals(Hint.CountTogether, CountObjectsEvaluator.evaluate(question, NumberChosen(4), 2).nextHint)
        val last = CountObjectsEvaluator.evaluate(question, NumberChosen(4), 3).nextHint
        assertEquals(Hint.NarrowChoices(listOf(2, 3)), last)
    }
}
