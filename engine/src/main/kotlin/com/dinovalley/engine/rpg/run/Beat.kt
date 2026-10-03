package com.dinovalley.engine.rpg.run

import com.dinovalley.engine.model.Speech
import com.dinovalley.engine.rpg.hero.Attribute
import com.dinovalley.engine.rpg.hero.Unlock
import com.dinovalley.engine.rpg.learn.Challenge
import com.dinovalley.engine.rpg.learn.Hue
import com.dinovalley.engine.rpg.learn.MapChallenge
import com.dinovalley.engine.rpg.world.RoomKind
import com.dinovalley.engine.rpg.world.Stop

/** Where a beat happens. The app paints a background for each. */
enum class Place { CAMP, GATE, RUNE_HALL, BRIDGE, CRYSTAL_CAVE, LIBRARY, TUNNEL, MIRROR_HALL, VAULT, STOREROOM, POND, MOSAIC_HALL, GOBLIN_DEN, WORKSHOP, LAIR, MAP }

/** Where each kind of room is drawn. */
fun placeOf(kind: RoomKind): Place = when (kind) {
    RoomKind.GATE -> Place.GATE
    RoomKind.RUNE_DOOR -> Place.RUNE_HALL
    RoomKind.BRIDGE -> Place.BRIDGE
    RoomKind.CRYSTAL_CAVE -> Place.CRYSTAL_CAVE
    RoomKind.LIBRARY -> Place.LIBRARY
    RoomKind.TUNNEL -> Place.TUNNEL
    RoomKind.MIRROR_HALL -> Place.MIRROR_HALL
    RoomKind.VAULT -> Place.VAULT
    RoomKind.STOREROOM -> Place.STOREROOM
    RoomKind.POND -> Place.POND
    RoomKind.MOSAIC_HALL -> Place.MOSAIC_HALL
    RoomKind.GOBLIN_DEN -> Place.GOBLIN_DEN
    RoomKind.WORKSHOP -> Place.WORKSHOP
    RoomKind.LAIR -> Place.LAIR
}

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

/** Things drawn along with a challenge. */
enum class Prop { NONE, CHEST }

enum class LootKind { COINS, GEM, MAGIC_KEY, POTION, TREASURE }

/** [hue] is the color of a gem, so the picture matches the words. */
data class Loot(val kind: LootKind, val count: Int, val words: String, val hue: Hue? = null)

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
     * said on success. [prop] is drawn with it (a chest with a magic lock). Reply [Reply.Solved].
     */
    data class Ask(
        override val scene: Scene,
        val challenge: Challenge,
        val oops: List<Speech>,
        val yay: List<Speech>,
        val prop: Prop = Prop.NONE,
    ) : Beat

    /**
     * Roll two dice and add them up. The rolls are decided by the seed so adventures can be
     * replayed; the app animates to [dice] and asks for the total. A wrong first answer costs a
     * heart. [reroll] is the Knight's second roll, offered after a low total.
     */
    data class Roll(
        override val scene: Scene,
        val why: List<Speech>,
        val dice: List<Int>,
        val reroll: List<Int>?,
    ) : Beat {
        val total: Int get() = dice.sum()
    }

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
        /** Said while each door lifts: its color and the kind of puzzle behind it (empty for closed doors). */
        val offers: List<List<Speech>> = emptyList(),
        /** Doors already tried, whose path wound back round to these doors. They can't be picked again. */
        val closed: Set<Int> = emptySet(),
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

    /** The dice were rolled; [usedReroll] if the Knight rolled again; [sumTries] tries at adding them up. */
    data class Rolled(val usedReroll: Boolean, val sumTries: Int) : Reply

    /** A choice or door was picked, after [tries] (more than 1 only for a map clue). */
    data class Picked(val index: Int, val tries: Int = 1) : Reply
}

/**
 * Everything a beat may say, in the order it is likely to be said, for getting the words ready
 * before they're needed.
 */
fun Beat.speech(): List<List<Speech>> = when (this) {
    is Beat.Tell -> listOf(lines)
    is Beat.Found -> listOf(lines)
    is Beat.Ask -> buildList {
        (challenge as? com.dinovalley.engine.rpg.learn.MemoryChallenge)?.let { add(it.remember) }
        add(challenge.prompt)
        add(yay)
        add(oops)
    }
    is Beat.Roll -> listOf(why)
    is Beat.Choose -> listOf(prompt)
    is Beat.Doors -> listOf(prompt) + offers
    is Beat.Finale -> listOf(summary.lines)
}
