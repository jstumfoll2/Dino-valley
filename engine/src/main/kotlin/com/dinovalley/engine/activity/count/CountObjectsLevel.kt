package com.dinovalley.engine.activity.count

import com.dinovalley.engine.model.Arrangement
import com.dinovalley.engine.model.ObjectSize

data class CountObjectsLevel(
    val level: Int,
    val minCount: Int,
    val maxCount: Int,
    val arrangement: Arrangement,
    val choiceCount: Int,
    val distractors: Int = 0,
    val tapToCount: Boolean = true,
    val objectSize: ObjectSize = ObjectSize.LARGE,
) {
    init {
        require(minCount in 1..maxCount) { "minCount must be between 1 and maxCount" }
        require(choiceCount in 2..4) { "choiceCount must be 2..4" }
    }
}

/**
 * The prototype's levels, hard-coded until the JSON content pack exists (ARCHITECTURE.md §7, §11).
 */
object PrototypeCountingLevels {
    val all = listOf(
        CountObjectsLevel(1, minCount = 1, maxCount = 3, arrangement = Arrangement.LINE, choiceCount = 2),
        CountObjectsLevel(2, minCount = 1, maxCount = 5, arrangement = Arrangement.LINE, choiceCount = 3),
        CountObjectsLevel(3, minCount = 1, maxCount = 5, arrangement = Arrangement.SCATTER, choiceCount = 3, objectSize = ObjectSize.MEDIUM),
        CountObjectsLevel(4, minCount = 2, maxCount = 7, arrangement = Arrangement.SCATTER, choiceCount = 3, distractors = 2, objectSize = ObjectSize.MEDIUM),
        CountObjectsLevel(5, minCount = 3, maxCount = 10, arrangement = Arrangement.CLUSTERS, choiceCount = 4, distractors = 2, tapToCount = false, objectSize = ObjectSize.SMALL),
    )

    fun level(n: Int): CountObjectsLevel = all[(n - 1).coerceIn(0, all.lastIndex)]
}
