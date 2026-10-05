package com.littledungeon.engine.rpg.run

import com.littledungeon.engine.model.Speech
import com.littledungeon.engine.rpg.hero.Attribute
import com.littledungeon.engine.rpg.hero.Unlock
import com.littledungeon.engine.rpg.learn.Challenge
import com.littledungeon.engine.rpg.learn.Hue
import com.littledungeon.engine.rpg.world.RoomKind

/**
 * Where a beat happens: the name of a painted backdrop (`art_scene_<id>`). Places are open data so
 * a new town or cave only needs a picture, not new code.
 */
data class Place(val id: String) {
    companion object {
        val CAMP = Place("camp")
        val RUNE_HALL = Place("rune_hall")
        val BRIDGE = Place("bridge")
        val CRYSTAL_CAVE = Place("crystal_cave")
        val LIBRARY = Place("library")
        val TUNNEL = Place("tunnel")
        val MIRROR_HALL = Place("mirror_hall")
        val VAULT = Place("vault")
        val STOREROOM = Place("storeroom")
        val POND = Place("pond")
        val MOSAIC_HALL = Place("mosaic_hall")
        val WORKSHOP = Place("workshop")
        val BELFRY = Place("belfry")
        val MARKET_STALL = Place("market_stall")
        val RHYME_BRIDGE = Place("rhyme_bridge")
        val BAT_CAVE = Place("bat_cave")

        /** Hoot's map table is the old parchment backdrop, `art_scene_map`. */
        val MAP_TABLE = Place("map")
        val WORLD_MAP = Place("world_map")
    }
}

/** Where each kind of room is drawn. */
fun placeOf(kind: RoomKind): Place = when (kind) {
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
    RoomKind.WORKSHOP -> Place.WORKSHOP
    RoomKind.BELFRY -> Place.BELFRY
    RoomKind.MARKET_STALL -> Place.MARKET_STALL
    RoomKind.RHYME_BRIDGE -> Place.RHYME_BRIDGE
    RoomKind.BAT_CAVE -> Place.BAT_CAVE
    RoomKind.MAP_ROOM -> Place.MAP_TABLE
}

/** Who can be on screen. COMPANION is the child's baby dragon. */
enum class Actor { HERO, COMPANION, GOBLIN, WIZARD, DRAGON, RUBY, SHADOW }

/** How the scene feels right now, so characters can react. */
enum class Mood { CALM, HAPPY, SURPRISED, SILLY }

/** A person on screen: their painted art is `art_<art>_*`, and [who] is their voice. */
data class NpcView(val id: String, val name: String, val art: String, val who: com.littledungeon.engine.model.Who)

/** A monster on screen, with its health, and the hero's, for the battle bars. */
data class FoeView(
    val id: String, val name: String, val art: String, val hp: Int, val maxHp: Int, val boss: Boolean,
    val who: com.littledungeon.engine.model.Who = com.littledungeon.engine.model.Who.GROWLER,
)

data class BattleView(val foe: FoeView, val heroHp: Int, val heroMaxHp: Int, val round: Int)

data class Scene(
    val place: Place,
    val cast: Set<Actor>,
    val mood: Mood = Mood.CALM,
    /** The room's obstacle is cleared (door open, bridge whole, lights on). */
    val cleared: Boolean = false,
    /** Someone from the world is here talking. */
    val npc: NpcView? = null,
    /** A fight is happening. */
    val battle: BattleView? = null,
)

/** A picture choice: [icon] is a painted icon (`art_<icon>`) the child taps; the narrator says [said]. */
data class Choice(val icon: String, val said: String)

/** Things drawn along with a challenge. */
enum class Prop { NONE, CHEST }

enum class LootKind { COINS, GEM, MAGIC_KEY, POTION, TREASURE, ITEM }

/** [hue] is the color of a gem, so the picture matches the words; [itemId] is the item for [LootKind.ITEM]. */
data class Loot(val kind: LootKind, val count: Int, val words: String, val hue: Hue? = null, val itemId: String? = null)

/**
 * One moment of the adventure. The app shows the beat, the child does something, and the app
 * sends back a [Reply]. A beat never leaves the child stuck: a miss is met with a hint, a joke or the right answer, and
 * the story goes on.
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
        /**
         * One try: a wrong answer shows the right one and the reply says it failed (no second go).
         * [allowedMisses] is how many slips a fiddly puzzle (drawing, sorting) allows before that.
         */
        val oneTry: Boolean = false,
        val allowedMisses: Int = 1,
        /** Answers already ruled out (a second guess after a charm), crossed out and not tappable. */
        val tried: List<Int> = emptyList(),
        /** Said with the right answer showing when the last try is missed: why it is right (see `Coach.explain`). */
        val explain: List<Speech> = emptyList(),
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
     * Pick where to go on the map of the kingdom. Reply [Reply.Picked] with the index of the route.
     */
    data class Travel(override val scene: Scene, val prompt: List<Speech>, val here: String, val routes: List<Route>) : Beat

    /** A shop. Reply [Reply.Bought] to buy something (the shop shows again), or [Reply.Next] to leave. */
    data class Shop(
        override val scene: Scene,
        val prompt: List<Speech>,
        val shopName: String,
        val coins: Int,
        val stock: List<ShopItem>,
    ) : Beat

    /** Something found. Reply [Reply.Next]. */
    data class Found(override val scene: Scene, val loot: Loot, val lines: List<Speech>) : Beat

    /**
     * Night falls and the party camps. The hero is rested (health full) and the child can stop here: the journey is saved
     * either way, so putting the game down at the fire loses nothing. [day] is the day that is ending. Reply [Reply.Next].
     */
    data class Night(override val scene: Scene, val lines: List<Speech>, val day: Int) : Beat

    /** The end of the adventure: stars earned, levels gained, what was unlocked. */
    data class Finale(override val scene: Scene, val summary: Summary) : Beat
}

/** A road out of where the hero is. [danger] is null when the hero is not wise enough yet to tell. */
data class Route(
    val to: String,
    val name: String,
    val kind: com.littledungeon.engine.rpg.world.LocationKind,
    val terrain: com.littledungeon.engine.rpg.world.Terrain,
    val danger: Int?,
    val visited: Boolean,
    /** The story points this way next. */
    val marked: Boolean,
    /** Closed for now: the hero was turned back here. */
    val blocked: Boolean,
    /** Said when this road is looked at. */
    val said: List<Speech>,
)

data class ShopItem(val itemId: String, val name: String, val price: Int, val owned: Int, val canAfford: Boolean)

data class Summary(
    val lines: List<Speech>,
    val starsEarned: Map<Attribute, Int>,
    val levelBefore: Int,
    val levelAfter: Int,
    val unlocked: List<Unlock>,
    val treasure: String,
    val ending: String,
    /** Coins carried home, pages of the Storybook now home, and monsters beaten this adventure. */
    val coins: Int = 0,
    val pages: Int = 0,
    val monsters: Int = 0,
)

/** What the child did in answer to a beat. */
sealed interface Reply {
    data object Next : Reply

    /**
     * A challenge finished: how many tries and hints it took, and how long. For a one-try challenge,
     * [failed] says it was not solved and [wrong] which answers were tried.
     */
    data class Solved(val tries: Int, val hints: Int, val millis: Long, val failed: Boolean = false, val wrong: List<Int> = emptyList()) : Reply

    /** Something was bought in a shop. */
    data class Bought(val itemId: String) : Reply

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
        (challenge as? com.littledungeon.engine.rpg.learn.MemoryChallenge)?.let { add(it.remember) }
        (challenge as? com.littledungeon.engine.rpg.learn.BellChallenge)?.let { add(it.listen) }
        add(challenge.prompt)
        add(yay)
        add(oops)
        if (explain.isNotEmpty()) add(explain)
    }
    is Beat.Roll -> listOf(why)
    is Beat.Choose -> listOf(prompt)
    is Beat.Travel -> listOf(prompt) + routes.map { it.said }
    is Beat.Shop -> listOf(prompt)
    is Beat.Night -> listOf(lines)
    is Beat.Finale -> listOf(summary.lines)
}

/** Narration runs at about 140 words a minute. */
private const val WORDS_PER_SECOND = 2.3

/**
 * How long a child takes over this beat, in seconds: the words said, plus time to look, think and tap. The same
 * figure measures a day's play (so the party camps for the night) and the balance report's session lengths.
 */
fun Beat.effortSeconds(): Double {
    val words = speech().firstOrNull()?.let { com.littledungeon.engine.model.Voice.caption(it).split(Regex("\\s+")).count { w -> w.isNotBlank() } } ?: 0
    val doing = when (this) {
        is Beat.Tell, is Beat.Found -> 1.5
        is Beat.Ask -> 10.0
        is Beat.Roll -> 3.0
        is Beat.Choose -> 4.0
        is Beat.Travel -> 6.0
        is Beat.Shop -> 5.0
        is Beat.Night, is Beat.Finale -> 0.0
    }
    return doing + words / WORDS_PER_SECOND
}
