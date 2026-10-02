package com.dinovalley.engine

import com.dinovalley.engine.activity.count.CountObjectsGenerator
import com.dinovalley.engine.activity.count.PrototypeCountingLevels
import com.dinovalley.engine.model.Ease
import com.dinovalley.engine.model.SpriteId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CountObjectsGeneratorTest {
    private val generator = CountObjectsGenerator(
        sprites = listOf(SpriteId("egg_blue"), SpriteId("egg_green")),
        distractorSprites = listOf(SpriteId("leaf")),
    )

    @Test
    fun `every level produces the right number of countable objects inside the play area`() {
        for (level in PrototypeCountingLevels.all) {
            repeat(200) { seed ->
                val q = generator.generate(level, Ease.NORMAL, seed.toLong())
                assertEquals(q.answer, q.scene.objects.count { it.countable })
                assertTrue(q.answer in level.minCount..level.maxCount)
                assertTrue(q.scene.objects.all { it.x in 0f..1f && it.y in 0f..1f })
            }
        }
    }

    @Test
    fun `choices are distinct, in counting order, and include the answer`() {
        for (level in PrototypeCountingLevels.all) {
            repeat(200) { seed ->
                val q = generator.generate(level, Ease.NORMAL, seed.toLong())
                assertEquals(level.choiceCount, q.choices.size)
                assertEquals(q.choices.sorted().distinct(), q.choices)
                assertTrue(q.answer in q.choices)
                assertTrue(q.choices.all { it >= 1 })
            }
        }
    }

    @Test
    fun `the same seed gives the same question`() {
        val level = PrototypeCountingLevels.level(4)
        assertEquals(generator.generate(level, Ease.NORMAL, 42), generator.generate(level, Ease.NORMAL, 42))
    }

    @Test
    fun `gentle questions stay in the lower half of the level`() {
        val level = PrototypeCountingLevels.level(5)
        repeat(200) { seed ->
            val q = generator.generate(level, Ease.GENTLE, seed.toLong())
            assertTrue(q.answer <= (level.minCount + level.maxCount) / 2)
        }
    }

    @Test
    fun `scattered objects do not sit on top of each other`() {
        val level = PrototypeCountingLevels.level(3)
        repeat(200) { seed ->
            val objects = generator.generate(level, Ease.NORMAL, seed.toLong()).scene.objects
            for (i in objects.indices) for (j in i + 1 until objects.size) {
                val dx = (objects[i].x - objects[j].x) * 2
                val dy = objects[i].y - objects[j].y
                assertTrue(dx * dx + dy * dy > 0.05f * 0.05f, "objects $i and $j overlap at seed $seed")
            }
        }
    }
}
