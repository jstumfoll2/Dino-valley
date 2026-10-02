package com.dinovalley.engine.rpg.run

import com.dinovalley.engine.model.Speech
import com.dinovalley.engine.rpg.hero.Attribute
import com.dinovalley.engine.rpg.hero.Unlock
import com.dinovalley.engine.rpg.learn.Challenge
import com.dinovalley.engine.rpg.learn.MapChallenge
import com.dinovalley.engine.rpg.world.Stop

/** Where a beat happens. The app paints a background for each. */
enum class Place { CAMP, GATE, RUNE_HALL, BRIDGE, CRYSTAL_CAVE, LIBRARY, TUNNEL, MIRROR_HALL, VAULT, GOBLIN_DEN, WORKSHOP, LAIR, MAP }

/** Who can be on screen. COMPANION is the child's baby dragon. */
enum class Actor { HERO, COMPANION, GOBLIN, WIZARD, DRAGON, RUBY, SHADOW }

/** How the scene feels right now, so characters can react. */
enum class Mood { CALM, HAPPY, SURPRISED, SILLY }

data class Scene(
    val place: Place,
    val cast: Set<Actor>,
    val mood: Mood = Mood.CALM,
    /** In the lair: how many of the three boss stars are lit. */
    val bossStars: Int? = null,
    /** The room's obstacle is cleared (door open, bridge whole, lights on). */
    val cleared: Boolean = false,
)

/** Picture choices. The app draws each; the narrator says [Choice.said]. */
enum class ChoicePicture { SHARE_SNACK, SING_SONG, TIPTOE, MAKE_FRIENDS, CAST_SPELL, LIGHT_SPELL, LULLABY }

data class Choice(val picture: ChoicePicture, val said: String)

enum class LootKind { COINS, GEM, MAGIC_KEY, POTION, TREASURE }

data class Loot(val kind: LootKind, val count: Int, val words: String)

/**
 * One moment of the adventure. The app shows the beat, the child does something, and the app
 * sends back a [Reply]. Beats never end in failure: every challenge is eventually solved.
 */
sealed interface Beat {
    val scene: Scene

    /** The narrator speaks; the app waits for the words (or a tap) and replies [Reply.Next]. */
    data class Tell(override val scene: Scene, val lines: List<Speech>) : Beat

    /**
     * A learning challenge. [oops] is the funny thing that happens on a first miss; [yay] is
     * said on success. Reply [Reply.Solved].
     */
    data class Ask(override val scene: Scene, val challenge: Challenge, val oops: List<Speech>, val yay: List<Speech>) : Beat

    /**
     * Roll the die. The roll is decided by the seed so adventures can be replayed; the app
     * animates to [value]. With [askSum] the child is asked "[value] + [bonus]?" before the
     * result counts. [reroll] is the Knight's second roll, offered after a 1 or 2.
     */
    data class Roll(
        override val scene: Scene,
        val why: List<Speech>,
        val value: Int,
        val bonus: Int,
        val askSum: Boolean,
        val reroll: Int?,
    ) : Beat

    /** A story choice between pictures. Reply [Reply.Picked]. */
    data class Choose(override val scene: Scene, val prompt: List<Speech>, val options: List<Choice>) : Beat

    /**
     * Pick a door on the map. With [clue] it's the map challenge (one door is right); without,
     * any door is fine. [peek] shows what's behind each door (Ranger power). Reply [Reply.Picked].
     */
    data class Doors(
        override val scene: Scene,
        val prompt: List<Speech>,
        val fork: Stop.Fork,
        val clue: MapChallenge?,
        val peek: Boolean,
        val stopIndex: Int,
    ) : Beat

    /** Something found. Reply [Reply.Next]. */
    data class Found(override val scene: Scene, val loot: Loot, val lines: List<Speech>) : Beat

    /** The end of the adventure: stars earned, levels gained, what was unlocked. */
    data class Finale(override val scene: Scene, val summary: Summary) : Beat
}

data class Summary(
    val lines: List<Speech>,
    val starsEarned: Map<Attribute, Int>,
    val levelBefore: Int,
    val levelAfter: Int,
    val unlocked: List<Unlock>,
    val treasure: String,
    val ending: String,
)

/** What the child did in answer to a beat. */
sealed interface Reply {
    data object Next : Reply

    /** A challenge finished: how many tries and hints it took, and how long. */
    data class Solved(val tries: Int, val hints: Int, val millis: Long) : Reply

    /** The die was rolled; [usedReroll] if the Knight rolled again; [sumTries] tries at the sum (0 if not asked). */
    data class Rolled(val usedReroll: Boolean, val sumTries: Int) : Reply

    /** A choice or door was picked, after [tries] (more than 1 only for a map clue). */
    data class Picked(val index: Int, val tries: Int = 1) : Reply
}
