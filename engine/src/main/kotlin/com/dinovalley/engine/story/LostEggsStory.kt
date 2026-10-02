package com.dinovalley.engine.story

import com.dinovalley.engine.activity.count.CountObjectsGenerator
import com.dinovalley.engine.activity.count.PrototypeCountingLevels
import com.dinovalley.engine.model.Ease
import com.dinovalley.engine.util.GameRandom
import kotlin.random.Random

/**
 * "The Lost Eggs": a storm blows Mama's eggs across the valley and the child's dino finds them.
 * The words stay the same every read; the numbers, letters and egg spots are new each time.
 */
class LostEggsStory(private val counting: CountObjectsGenerator) {

    fun write(level: StoryLevel, seed: Long): StoryBook {
        val seeds = GameRandom(seed)
        val random = Random(seeds.nextSeed())
        val countLevel = PrototypeCountingLevels.level(level.countingLevel)
        val meadow = counting.generate(countLevel, Ease.NORMAL, seeds.nextSeed())
        val cave = counting.generate(countLevel, Ease.GENTLE, seeds.nextSeed())
        val stone = random.nextInt(1, level.numeralMax + 1)
        val stones = pick((1..level.numeralMax).toList(), stone, level.numeralChoices, random).sorted()
        val letter = level.letters.random(random)
        val bushes = pick(level.letters.toList(), letter, level.letterChoices, random)
        val word = LETTER_WORDS.getValue(letter)

        val pages = listOf(
            StoryPage(
                backdrop = Backdrop.STORM,
                cast = setOf(Cast.HERO),
                narration = Speech.of("One night, a big storm came to Dino Valley. Tap the clouds to make the thunder go boom!"),
                prompt = Speech.of("Tap the clouds!"),
                challenge = Challenge.TapClouds(taps = 3),
                afterward = Speech.of("Whoosh! The wind blew and blew, all night long."),
            ),
            StoryPage(
                backdrop = Backdrop.VALLEY,
                cast = setOf(Cast.HERO, Cast.MAMA),
                narration = Speech.of(
                    "In the morning, Mama Dino looked in her nest. Oh no! Her eggs were gone! " +
                        "The wind blew them all over the valley. {name} said, I will find them, Mama!",
                ),
            ),
            StoryPage(
                backdrop = Backdrop.MEADOW,
                cast = setOf(Cast.HERO),
                narration = Speech.of("{name} looked in the flower meadow. Eggs! How many eggs can you find?"),
                prompt = Speech.of("How many eggs?"),
                challenge = Challenge.CountEggs(meadow, inTheDark = false),
                afterward = Speech.of("${capitalized(meadow.answer)}! {name} put them in a basket."),
            ),
            StoryPage(
                backdrop = Backdrop.RIVER,
                cast = setOf(Cast.HERO),
                narration = Speech.of(
                    "Splash! There is an egg on the other side of the river. " +
                        "Help {name} hop on stone number ${numberWord(stone)}!",
                ),
                prompt = Speech.of("Find the number ${numberWord(stone)}."),
                challenge = Challenge.FindNumeral(stone, stones),
                afterward = Speech.of("Hop, hop, hop! {name} got the egg!"),
            ),
            StoryPage(
                backdrop = Backdrop.FOREST,
                cast = setOf(Cast.HERO),
                narration = Speech.of(
                    "In the big fern forest, an egg is hiding behind a bush. " +
                        "It is the bush with the letter $letter. $letter, as in $word.",
                ),
                prompt = Speech.of("Find the letter $letter. $letter, as in $word."),
                challenge = Challenge.FindLetter(letter, bushes, word),
                afterward = Speech.of("Peekaboo! There it is! $letter is for $word."),
            ),
            StoryPage(
                backdrop = Backdrop.CAVE,
                cast = setOf(Cast.HERO),
                narration = Speech.of("The last eggs rolled into a dark cave. Tap the mushrooms to light it up, then count the eggs!"),
                prompt = Speech.of("How many eggs are in the cave?"),
                challenge = Challenge.CountEggs(cave, inTheDark = true),
                afterward = Speech.of("${capitalized(cave.answer)} eggs. That's all of them!"),
            ),
            StoryPage(
                backdrop = Backdrop.HOME,
                cast = setOf(Cast.HERO, Cast.MAMA),
                narration = Speech.of("{name} brought every egg home to Mama. Then, crack, crack, crack! Tap the eggs!"),
                prompt = Speech.of("Tap the eggs!"),
                challenge = Challenge.HatchEggs(eggs = 3),
                afterward = Speech.of("Baby dinos! Thank you, {name}! You are the best egg finder in Dino Valley. The end."),
            ),
        )
        return StoryBook(title = Speech.of("{name} and the Lost Eggs"), pages = pages, level = level.level, seed = seed)
    }

    private fun <T> pick(pool: List<T>, answer: T, count: Int, random: Random): List<T> =
        (listOf(answer) + pool.filter { it != answer }.shuffled(random).take(count - 1)).shuffled(random)

    companion object {
        private val NUMBER_WORDS = listOf(
            "zero", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten",
            "eleven", "twelve", "thirteen", "fourteen", "fifteen", "sixteen", "seventeen", "eighteen", "nineteen", "twenty",
        )

        fun numberWord(n: Int): String = NUMBER_WORDS.getOrElse(n) { n.toString() }

        private fun capitalized(n: Int): String = numberWord(n).replaceFirstChar { it.uppercase() }

        /** A picture word for every letter, all things a four-year-old can see in their head. */
        val LETTER_WORDS = mapOf(
            'A' to "apple", 'B' to "ball", 'C' to "cat", 'D' to "dinosaur", 'E' to "egg", 'F' to "fish",
            'G' to "goat", 'H' to "hat", 'I' to "igloo", 'J' to "jelly", 'K' to "kite", 'L' to "leaf",
            'M' to "moon", 'N' to "nest", 'O' to "octopus", 'P' to "pig", 'Q' to "queen", 'R' to "rain",
            'S' to "sun", 'T' to "tree", 'U' to "umbrella", 'V' to "volcano", 'W' to "water", 'X' to "x-ray",
            'Y' to "yo-yo", 'Z' to "zebra",
        )
    }
}
