package com.dinovalley.engine.rpg.run

import com.dinovalley.engine.rpg.hero.HeroClass
import com.dinovalley.engine.rpg.learn.Hue
import com.dinovalley.engine.rpg.learn.Words

/**
 * What the screens say on their own: hints, counting along, dice, the camp. Kept here, next to
 * the story's lines, so the build can record every sentence the game can say (see
 * [VoiceCatalog]). [all] must list every sentence these functions can make.
 */
object Say {
    const val TRY_AGAIN = "Let's try again!"
    const val SPARKLE = "Sparkle magic! Your wizard power gives you a hint."
    const val GLOW = "Look! {name} makes the right one glow!"
    const val WHISPER = "{name} whispers a hint!"
    const val SLOW_DOWN = "Whoa, slow down! Look carefully, then pick one."
    const val ONE_TRY_MISS = "Oh no, that is not the one. Look, this is the right answer."
    const val NOT_THAT = "Not that one."
    const val KEEP_GOING = "Keep going!"
    const val NEXT_LINE = "Now the next line!"
    const val TRACE_HELP = "Start at the green star, and follow the glowing line."
    const val WHICH_NEXT_DOOR = "Which door comes next?"
    const val LOOK_RECIPE = "Look at the recipe!"
    const val PICK_DOOR = "Which door will you pick?"
    const val ROLL_AGAIN = "Knight power! Do you want to roll again? Tap the dice to roll, or tap the check to keep them."
    const val FIRST_ROLL_BETTER = "The first roll was better. Let's keep it!"
    const val HEART_POPS = "Oh no, a heart pops! Let's count the dots together."
    const val TAP_TO_ROLL = "Tap the dice to roll them!"
    const val SORT_YES = "Yes!"
    const val PIECE_FITS = "It fits!"
    const val WELCOME_NEW = "Welcome to the Little Dungeon! This is your baby dragon, {name}. Pick your hero, then tap the big green button!"
    const val WELCOME_BACK = "Welcome back, adventurer! {name} is ready. Pick your hero, then tap the big green button!"
    const val NAME_ASK = "What should we call your baby dragon? Ask a grown-up to type the name."
    const val NAME_SET = "Hello, {name}! What a great name! Let's go on an adventure!"

    /** "One!" when counting things with a finger. */
    fun count(n: Int) = Words.capital(n) + "!"

    fun doorPicked(hue: Hue) = "The ${hue.word.uppercase()} door! [creak]"

    fun doorName(hue: Hue) = "The ${hue.word.uppercase()} door!"

    fun option(said: String, last: Boolean) = if (last) "Or... $said?" else "$said?"

    fun chosen(said: String) = "$said!"

    fun stir(n: Int) = "Now stir ${Words.number(n)} times! Tap the spoon."

    fun diceSum(a: Int, b: Int) = "You rolled ${Words.number(a).uppercase()} and ${Words.number(b).uppercase()}. How many dots is that altogether?"

    fun diceTotal(total: Int) = "${Words.capital(total)} dots altogether! Now find ${Words.number(total).uppercase()}."

    fun diceRight(a: Int, b: Int) = "Yes! ${Words.capital(a)} and ${Words.number(b)} make ${Words.number(a + b)}!"

    fun heroLine(c: HeroClass) = when (c) {
        HeroClass.KNIGHT -> "The Knight! Brave and strong."
        HeroClass.WIZARD -> "The Wizard! Full of magic."
        HeroClass.RANGER -> "The Ranger! Sharp eyes that see behind doors."
        HeroClass.GUARDIAN -> "The Guardian! Everyone wants to be your friend."
        HeroClass.SPELLKEEPER -> "The Spellkeeper! Keeper of runes and stories."
    }

    fun heroLocked(level: Int) = "This hero joins at level ${Words.number(level)}. Keep adventuring!"

    /** Every sentence above, with every value it can take. */
    fun all(choices: List<String>): List<String> = buildList {
        addAll(
            listOf(
                TRY_AGAIN, SPARKLE, GLOW, WHISPER, SLOW_DOWN, ONE_TRY_MISS, NOT_THAT, KEEP_GOING, NEXT_LINE, TRACE_HELP, WHICH_NEXT_DOOR,
                LOOK_RECIPE, PICK_DOOR, ROLL_AGAIN, FIRST_ROLL_BETTER, HEART_POPS, TAP_TO_ROLL, SORT_YES, PIECE_FITS,
                WELCOME_NEW, WELCOME_BACK, NAME_ASK, NAME_SET,
            ),
        )
        (1..100).forEach { add(count(it)) }
        Hue.entries.forEach { add(doorPicked(it)); add(doorName(it)) }
        choices.forEach { add(option(it, true)); add(option(it, false)); add(chosen(it)) }
        (1..12).forEach { add(stir(it)) }
        for (a in 1..6) for (b in 1..6) {
            add(diceSum(a, b))
            add(diceRight(a, b))
        }
        (2..12).forEach { add(diceTotal(it)) }
        HeroClass.entries.forEach { add(heroLine(it)); add(heroLocked(it.unlockLevel)) }
    }
}
