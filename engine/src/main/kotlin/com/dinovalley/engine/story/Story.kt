package com.dinovalley.engine.story

import com.dinovalley.engine.model.CountObjectsInstance

/** A piece of narration: spoken words, or the dino's name (the child's own recording, or "Rex"). */
sealed interface Speech {
    data class Words(val text: String) : Speech
    data object Name : Speech

    companion object {
        /** "Thank you, {name}!" becomes words, the name, then words again. */
        fun of(template: String): List<Speech> =
            template.split(NAME).flatMapIndexed { i, part ->
                listOfNotNull(if (i > 0) Name else null, part.takeIf { it.isNotBlank() }?.let { Words(it.trim()) })
            }

        private const val NAME = "{name}"
    }
}

/** The painted background of a page. The app maps each one to its artwork. */
enum class Backdrop { VALLEY, STORM, MEADOW, RIVER, FOREST, CAVE, HOME }

/** Who stands on the page. */
enum class Cast { HERO, MAMA }

/** The game part of a page. Every challenge ends in success; help grows with each try. */
sealed interface Challenge {
    /** Tap the storm clouds to make thunder. Nothing to get wrong. */
    data class TapClouds(val taps: Int) : Challenge

    /** How many eggs? In the cave the eggs stay dark until the mushrooms are tapped. */
    data class CountEggs(val question: CountObjectsInstance, val inTheDark: Boolean) : Challenge

    /** Hop on the stone with this numeral. */
    data class FindNumeral(val target: Int, val choices: List<Int>) : Challenge

    /** Find the bush with this letter. [word] is a picture word for it ("B, as in ball"). */
    data class FindLetter(val target: Char, val choices: List<Char>, val word: String) : Challenge

    /** Tap the eggs in the nest to hatch them. */
    data class HatchEggs(val eggs: Int) : Challenge
}

data class StoryPage(
    val backdrop: Backdrop,
    val cast: Set<Cast>,
    /** Read when the page opens. */
    val narration: List<Speech>,
    /** Read again by the replay button while the challenge waits; usually the question alone. */
    val prompt: List<Speech> = emptyList(),
    val challenge: Challenge? = null,
    /** Read once the challenge is done, before the page can turn. */
    val afterward: List<Speech> = emptyList(),
)

data class StoryBook(val title: List<Speech>, val pages: List<StoryPage>, val level: Int, val seed: Long) {
    /** Pages with a learning question (not just taps), for the difficulty rules. */
    val questionCount: Int
        get() = pages.count { it.challenge is Challenge.CountEggs || it.challenge is Challenge.FindNumeral || it.challenge is Challenge.FindLetter }
}
