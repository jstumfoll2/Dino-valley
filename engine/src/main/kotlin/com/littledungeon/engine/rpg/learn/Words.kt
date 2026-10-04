package com.littledungeon.engine.rpg.learn

/** Spoken forms used across the narration. */
object Words {
    private val NUMBERS = listOf(
        "zero", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten",
        "eleven", "twelve", "thirteen", "fourteen", "fifteen", "sixteen", "seventeen", "eighteen", "nineteen", "twenty",
    )

    private val TENS = listOf("", "", "twenty", "thirty", "forty", "fifty", "sixty", "seventy", "eighty", "ninety")

    /** Numbers as words, up to a hundred. */
    fun number(n: Int): String = when {
        n in NUMBERS.indices -> NUMBERS[n]
        n in 21..99 -> TENS[n / 10] + if (n % 10 == 0) "" else "-" + NUMBERS[n % 10]
        n == 100 -> "one hundred"
        else -> n.toString()
    }

    fun capital(n: Int): String = number(n).replaceFirstChar { it.uppercase() }

    /** A picture word for every letter, picked to fit a fantasy world a four-year-old can imagine. */
    val LETTER_WORDS = mapOf(
        'A' to "apple", 'B' to "bat", 'C' to "castle", 'D' to "dragon", 'E' to "egg", 'F' to "fairy",
        'G' to "goblin", 'H' to "helmet", 'I' to "igloo", 'J' to "jewel", 'K' to "king", 'L' to "lantern",
        'M' to "moon", 'N' to "nest", 'O' to "octopus", 'P' to "potion", 'Q' to "queen", 'R' to "ruby",
        'S' to "sword", 'T' to "tower", 'U' to "umbrella", 'V' to "volcano", 'W' to "wizard", 'X' to "x-ray",
        'Y' to "yo-yo", 'Z' to "zebra",
    )

    /** How the narrator makes a letter's sound. Only letters with a clear, sayable sound. */
    val LETTER_SOUNDS = mapOf(
        'M' to "mmmm", 'S' to "ssss", 'F' to "ffff", 'L' to "llll", 'N' to "nnnn", 'R' to "rrrr",
        'Z' to "zzzz", 'V' to "vvvv", 'A' to "ah", 'B' to "buh", 'D' to "duh", 'P' to "puh", 'T' to "tuh",
    )
}
