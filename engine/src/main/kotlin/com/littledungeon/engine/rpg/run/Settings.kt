package com.littledungeon.engine.rpg.run

/**
 * What a grown-up can set for this child (behind the app's parent gate). Part of what decides a journey, so it is saved with the
 * adventure and a replay uses the same one.
 *
 * - [twoTries]: a missed one-try puzzle gets a second look, with the wrong answer crossed out, before the story moves on.
 * - [levelFloor] and [levelCeiling]: the puzzle level is never below or above these, whatever the child has shown.
 * - [dayMinutes]: how long a day of play is before the party camps for the night.
 */
data class Settings(
    val twoTries: Boolean = false,
    val levelFloor: Int = 1,
    val levelCeiling: Int = 5,
    val dayMinutes: Int = 9,
) {
    init {
        require(levelFloor in 1..5 && levelCeiling in levelFloor..5) { "levels must be 1 to 5, floor at most ceiling" }
        require(dayMinutes in 3..30) { "a day is 3 to 30 minutes" }
    }

    companion object {
        /** The settings that can be offered, as fixed steps a grown-up taps through. */
        val DAY_CHOICES = listOf(5, 9, 15, 20)
    }
}
