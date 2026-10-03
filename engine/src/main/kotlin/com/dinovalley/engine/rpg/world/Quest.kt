package com.dinovalley.engine.rpg.world

import com.dinovalley.engine.rpg.learn.PotionKind
import kotlin.random.Random

/** The three storylines from the brief. Each is retold with different details every time. */
enum class QuestKind { DRAGONS_PRISONER, LONELY_DRAGON, RUBYS_QUEST }

/** What is really going on: the part the child discovers at the end. */
enum class Twist(val quest: QuestKind) {
    RUBY_VISITING(QuestKind.DRAGONS_PRISONER),
    TRICKSTER(QuestKind.DRAGONS_PRISONER),
    LOST_AND_WARM(QuestKind.DRAGONS_PRISONER),
    COLLECTOR(QuestKind.LONELY_DRAGON),
    SPRITE_THIEF(QuestKind.LONELY_DRAGON),
    INK_SHADOW(QuestKind.RUBYS_QUEST),
    SLEEPY_SHADOW(QuestKind.RUBYS_QUEST),
}

enum class Boss { DRAGON, SHADOW }

/** One adventure's story, with every changeable detail filled in. */
data class Quest(
    val kind: QuestKind,
    val twist: Twist,
    val dungeon: String,
    val goblin: String,
    val dragon: String,
    val treasure: String,
    /** The potion brewed in the workshop; it opens the way into the lair. */
    val potion: PotionKind,
) {
    val boss: Boss get() = if (kind == QuestKind.RUBYS_QUEST) Boss.SHADOW else Boss.DRAGON
}

object QuestWriter {
    private val goblins = listOf("Pip", "Nib", "Tock", "Moss", "Bindle", "Wobble")
    private val dragons = listOf("Ember", "Cinder", "Bramble", "Smolder", "Puddle", "Glim")
    private val treasures = listOf("a crown of moonstones", "a singing seashell", "a golden telescope", "a rainbow egg", "a map of the stars", "a tiny silver bell")

    /** Picks a storyline, preferring ones not seen yet, then ones not played last time. */
    fun write(seed: Long, dungeon: String, world: WorldMemory): Quest {
        val r = Random(seed)
        val kind = when {
            world.adventures == 0 -> QuestKind.DRAGONS_PRISONER
            else -> QuestKind.entries
                .filter { it != world.lastQuest }
                .minByOrNull { (world.questsDone[it] ?: 0) * 10 + r.nextInt(10) }!!
        }
        val twist = Twist.entries.filter { it.quest == kind }.random(r)
        // Friends come back: a goblin the child befriended may be the one in this dungeon.
        val goblin = world.friends.filter { it in goblins }.takeIf { it.isNotEmpty() && r.nextInt(3) == 0 }?.random(r) ?: goblins.random(r)
        val dragon = world.dragonFriend ?: dragons.random(r)
        val potion = when (kind) {
            QuestKind.DRAGONS_PRISONER -> listOf(PotionKind.GIANT_STRENGTH, PotionKind.GLOW).random(r)
            QuestKind.LONELY_DRAGON -> listOf(PotionKind.FRIENDSHIP, PotionKind.BUBBLE).random(r)
            QuestKind.RUBYS_QUEST -> listOf(PotionKind.GLOW, PotionKind.GIANT_STRENGTH).random(r)
        }
        return Quest(kind, twist, dungeon, goblin, dragon, treasures.random(r), potion)
    }
}

/** What the kingdom remembers between adventures, so the story can notice the child. */
data class WorldMemory(
    val adventures: Int = 0,
    val friends: Set<String> = emptySet(),
    /** The boss dragon's name once the child has made friends with it; it stays the same dragon after. */
    val dragonFriend: String? = null,
    val endings: Set<String> = emptySet(),
    val treasures: List<String> = emptyList(),
    val questsDone: Map<QuestKind, Int> = emptyMap(),
    val lastQuest: QuestKind? = null,
    /** Things that happened and stay true: "finn_has_boat", "bess_is_friend". See story.Effect. */
    val flags: Set<String> = emptySet(),
    /** How well each person likes the hero (person id to a number; higher is friendlier). */
    val relations: Map<String, Int> = emptyMap(),
    /** Pages of the Great Storybook recovered so far, across adventures. */
    val pages: Int = 0,
    /** Stories finished, by arc id, with how many times. */
    val arcsDone: Map<String, Int> = emptyMap(),
    /** The story played last, so the next one is different. */
    val lastArc: String? = null,
)
