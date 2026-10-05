package com.littledungeon.engine.rpg.run

import com.littledungeon.engine.model.Speech
import com.littledungeon.engine.rpg.battle.Monster
import com.littledungeon.engine.rpg.content.Content
import com.littledungeon.engine.rpg.items.Item
import com.littledungeon.engine.rpg.items.ItemKind
import com.littledungeon.engine.rpg.items.Obstacle
import com.littledungeon.engine.rpg.learn.Challenge
import com.littledungeon.engine.rpg.learn.ChallengeFactory
import com.littledungeon.engine.rpg.learn.Coach
import com.littledungeon.engine.rpg.learn.PickOne
import com.littledungeon.engine.rpg.learn.PictureFactory
import com.littledungeon.engine.rpg.learn.Skill
import com.littledungeon.engine.rpg.learn.Thing
import com.littledungeon.engine.rpg.learn.Words

/**
 * A puzzle dressed for a place: which skill it practices, and the words that set it up. The place decides
 * the costume (a river has lily pads, a lock has a number); the child's needs decide which costume a
 * given obstacle wears this time (see [pickSkill]). For colors, [intro] is who is asking ("The river frog").
 */
internal class Costume(val skill: Skill, val intro: String, val thing: Thing = Thing.STONE)

/** What can dress each kind of obstacle. All are pick-one puzzles, so a try means one tap. */
internal fun costumesFor(o: Obstacle): List<Costume> = when (o) {
    Obstacle.CLIMB -> listOf(
        Costume(Skill.COUNTING, "Count the stones in the staircase."),
        Costume(Skill.ADDITION, "Some stones in the staircase are missing."),
        Costume(Skill.NUMBERS, "Numbers are carved into the steps."),
        Costume(Skill.MONEY, "A toll keeper sells a rope for coins."),
        Costume(Skill.SHARING, "Hungry bats guard the trail."),
    )
    Obstacle.CROSS -> listOf(
        Costume(Skill.SKIP_COUNTING, "Hop across the river on the lily pads."),
        Costume(Skill.COUNTING, "Count the stepping stones across the river."),
        Costume(Skill.COLORS, "The river frog", Thing.GEM),
        Costume(Skill.RHYMES, "The planks of the bridge are pictures."),
    )
    Obstacle.DARK -> listOf(
        Costume(Skill.PATTERNS, "Glowing marks show the safe path."),
        Costume(Skill.COLORS, "The marsh sprite", Thing.GEM),
        Costume(Skill.LETTERS, "Glowing letters show the safe path."),
        Costume(Skill.SHARING, "Hungry bats squeak in the dark."),
    )
    Obstacle.LOCK -> listOf(
        Costume(Skill.NUMBERS, "The lock wants a number."),
        Costume(Skill.ADDITION, "The lock counts the coins you put in.", Thing.COIN),
        Costume(Skill.MONEY, "The lock only takes exact coins."),
    )
    Obstacle.RIDDLE -> listOf(
        Costume(Skill.LETTERS, "The stone face wants a letter."),
        Costume(Skill.COLORS, "The stone face", Thing.GEM),
        Costume(Skill.PATTERNS, "The stone face shows a pattern."),
        Costume(Skill.RHYMES, "The stone face loves rhymes."),
    )
}

/**
 * Which of [candidates] to practice now. Skills already asked this journey wait for the others, so one
 * skill never takes over a journey; after that the one practiced longest ago and the one at the lowest
 * level come first, with a little shuffle so no two journeys ask in the same order. [preferred] skills
 * (a monster's own tricks) get a small head start, never enough to beat a skill that has not been asked yet.
 */
internal fun Journey.pickSkill(candidates: List<Skill>, preferred: Collection<Skill> = emptyList()): Skill {
    val oldestFirst = Skill.entries.sortedBy { skills.lastPracticed[it] ?: 0L }
    return candidates.minBy { skill ->
        (skillUse[skill] ?: 0) * 100.0 + oldestFirst.indexOf(skill) * 4.0 + skills.level(skill) * 3.0 +
            random.nextDouble() * 12.0 - if (skill in preferred) 40.0 else 0.0
    }
}

/** The skills a monster can ask in a fight (each dresses as the monster); the newer picture skills are asked at obstacles. */
internal val BATTLE_SKILLS = listOf(
    Skill.COUNTING, Skill.NUMBERS, Skill.ADDITION, Skill.COLORS, Skill.PATTERNS, Skill.LETTERS, Skill.SKIP_COUNTING,
)

/** Every skill that has a pick-one puzzle (one tap per try). */
internal val PICK_ONE_SKILLS = BATTLE_SKILLS + listOf(Skill.RHYMES, Skill.MONEY, Skill.SHARING)

/**
 * One pick-one puzzle for [skill], set up with [intro] and counting [thing]s. The words always say what to
 * do, so the same sentence works wherever the costume is worn.
 */
internal fun Journey.puzzleFor(skill: Skill, intro: String, thing: Thing = Thing.STONE): PickOne {
    val lvl = level(skill)
    val seed = nextSeed()
    return when (skill) {
        Skill.COUNTING -> ChallengeFactory.count(lvl, seed, thing, "$intro How many ${thing.many} are there?")
        Skill.NUMBERS -> ChallengeFactory.numeral(lvl, seed, intro)
        Skill.ADDITION -> ChallengeFactory.add(lvl, seed, thing) { have, more, missing -> addStory(intro, thing, have, more, missing) }
        Skill.COLORS -> ChallengeFactory.color(lvl, seed, intro, thing.one)
        Skill.PATTERNS -> ChallengeFactory.pattern(lvl, seed, "$intro Which symbol comes next?")
        Skill.LETTERS -> ChallengeFactory.letter(lvl, seed, intro)
        Skill.SKIP_COUNTING -> ChallengeFactory.skipCount(lvl, seed, intro)
        Skill.RHYMES -> PictureFactory.rhyme(lvl, seed, intro)
        Skill.MONEY -> PictureFactory.money(lvl, seed, intro)
        Skill.SHARING -> PictureFactory.share(lvl, seed, intro)
        else -> error("$skill has no pick-one puzzle")
    }
}

/**
 * The words of a sum: what is there and what is added, or what is needed and what there is. A sentence a number is in never
 * holds the intro (a monster's name), so every sentence can be recorded ahead of time (see `VoiceCatalog`).
 */
internal fun addStory(intro: String, thing: Thing, have: Int, more: Int, missing: Boolean): String =
    if (missing) {
        "$intro It needs ${Words.number(have + more)} ${thing.many} and has ${Words.number(have)}. How many more does it need?"
    } else {
        "$intro There ${if (have == 1) "is" else "are"} ${Words.number(have)} ${thing.words(have)}, and ${Words.number(more)} more. How many ${thing.many} is that altogether?"
    }

/** The one-try puzzle for something blocking the way, in the costume the child most needs. */
internal fun Journey.obstaclePuzzle(o: Obstacle): PickOne {
    val options = costumesFor(o)
    val skill = pickSkill(options.map { it.skill })
    val c = options.first { it.skill == skill }
    return puzzleFor(skill, c.intro, c.thing)
}

/** How a monster dresses a puzzle of [skill]. */
internal fun battleCostume(m: Monster, skill: Skill): Costume {
    val name = m.name
    return when (skill) {
        Skill.COUNTING -> Costume(skill, "The $name hid some coins.", Thing.COIN)
        Skill.NUMBERS -> Costume(skill, "The $name holds up a card.")
        Skill.ADDITION -> Costume(skill, "The $name is counting its gems.", Thing.GEM)
        Skill.COLORS -> Costume(skill, "The $name", Thing.GEM)
        Skill.PATTERNS -> Costume(skill, "The $name makes a pattern.")
        Skill.LETTERS -> Costume(skill, "The $name scribbles a letter.")
        Skill.SKIP_COUNTING -> Costume(skill, "The $name hops along.")
        else -> Costume(Skill.NUMBERS, "The $name holds up a card.")
    }
}

/** A monster's attack puzzle: one of its own skills leaning to the one the child needs most, dressed in its name. */
internal fun Journey.battlePuzzle(m: Monster): PickOne {
    val c = battleCostume(m, pickSkill(BATTLE_SKILLS, preferred = m.skills))
    return puzzleFor(c.skill, c.intro, c.thing)
}

/**
 * Asks a puzzle with one try. A right answer goes to [onWin]. A wrong one shows the right answer and
 * then offers help from the bag, if there is any: a tool that gets past the [obstacle], or a charm
 * for a second guess. With nothing to use, or if the hero gives up, it goes to [onFail]. A charm can
 * only be used once per puzzle, so there is never a way to simply guess until it is right.
 */
internal fun Journey.askOnce(
    scene: Scene,
    c: Challenge,
    oops: String,
    yay: String,
    obstacle: Obstacle?,
    prop: Prop = Prop.NONE,
    tried: List<Int> = emptyList(),
    charmUsed: Boolean = false,
    onWin: (Reply.Solved) -> List<JStep>,
    onFail: () -> List<JStep>,
): JStep {
    val misses = if (c is PickOne) 1 else 2
    return JStep(Beat.Ask(scene, c, Speech.of(oops), Speech.of(yay), prop, oneTry = true, allowedMisses = misses, tried = tried, explain = Coach.explain(c))) { reply ->
        val s = reply as? Reply.Solved ?: Reply.Solved(1, 0, 0)
        record(c, if (charmUsed) maxOf(2, s.tries) else s.tries, s.hints, s.millis, s.failed)
        if (!s.failed) {
            onWin(s)
        } else {
            // Every third slip, the baby dragon finds a lucky clover, so nobody gets stuck for good.
            slips++
            val gift = if (slips % 3 == 0 && !charmUsed && !hero.has("lucky_clover")) {
                hero = hero.give("lucky_clover")
                listOf(tell(scene, say.petClover()))
            } else {
                emptyList()
            }
            gift + rescue(scene, c, oops, yay, obstacle, prop, (tried + s.wrong).distinct(), charmUsed, onWin, onFail)
        }
    }
}

private fun Journey.rescue(
    scene: Scene, c: Challenge, oops: String, yay: String, obstacle: Obstacle?, prop: Prop,
    wrong: List<Int>, charmUsed: Boolean, onWin: (Reply.Solved) -> List<JStep>, onFail: () -> List<JStep>,
): List<JStep> {
    // A grown-up can allow a second look at every missed puzzle: the wrong answer is crossed out, nothing is used up.
    if (settings.twoTries && !charmUsed && c is PickOne) {
        val more = extraWrong(c, SPARKLE, wrong)
        return listOf(
            tell(scene, say.anotherLook()),
            askOnce(scene, c, oops, yay, obstacle, prop, wrong + more, charmUsed = true, onWin = onWin, onFail = onFail),
        )
    }
    val owned = hero.bag.keys.mapNotNull { Content.item(it) }
    val tool = if (obstacle != null) owned.firstOrNull { obstacle in it.opens } else null
    val charms = if (charmUsed) emptyList() else owned.filter { it.isCharm }.sortedBy { it.guess }.take(2)
    // Sparkle magic (the Wizard's, the Spellkeeper's, the baby dragon's breath) works like a charm that is never in the bag.
    val sparkle = if (!charmUsed && sparkleLeft > 0 && c is PickOne) SPARKLE else null
    if (tool == null && charms.isEmpty() && sparkle == null) return onFail()
    val options = buildList {
        tool?.let { add(it to "tool") }
        charms.forEach { add(it to "charm") }
        sparkle?.let { add(it to "sparkle") }
    }
    // Sparkle magic has no picture of its own: it is shown as the wand it comes from.
    val choices = options.map { (item, kind) -> Choice(if (kind == "sparkle") "item_magic_wand" else "item_${item.id}", "Use the ${item.name}") } + Choice("hub_leave", "Give up")
    val ask = Speech.of(say.rescueAsk())
    return listOf(
        JStep(Beat.Choose(scene, ask, choices)) { reply ->
            val index = (reply as? Reply.Picked)?.index ?: options.size
            val picked = options.getOrNull(index)
            when {
                picked == null -> onFail()
                picked.second == "tool" -> {
                    hero = hero.take(picked.first.id)
                    listOf(tell(scene, say.toolWorks(picked.first.name, obstacle!!))) + onWin(Reply.Solved(2, 0, 0))
                }
                else -> {
                    val sparkling = picked.second == "sparkle"
                    if (sparkling) sparkleLeft-- else hero = hero.take(picked.first.id)
                    val more = extraWrong(c, picked.first, wrong)
                    listOf(
                        tell(scene, if (sparkling) say.sparkleWorks() else say.charmWorks(picked.first.name)),
                        askOnce(scene, c, oops, yay, obstacle, prop, wrong + more, charmUsed = true, onWin = onWin, onFail = onFail),
                    )
                }
            }
        },
    )
}

/** Sparkle magic as an item: not in any bag or shop, it takes one wrong answer away. */
private val SPARKLE = Item("sparkle_magic", "Sparkle Magic", ItemKind.CHARM, "Sparkle magic takes one wrong answer away.", guess = 1)

/** Wrong answers a charm takes away: as many as it says, never the right one or one already out. */
internal fun Journey.extraWrong(c: Challenge, charm: Item, alreadyOut: List<Int>): List<Int> {
    if (c !is PickOne || charm.guess <= 0) return emptyList()
    val candidates = (0 until c.optionCount).filter { it != c.answer && it !in alreadyOut }.shuffled(random)
    // Always leave at least two answers standing, so it is still a real choice.
    val room = (c.optionCount - alreadyOut.size - 2).coerceAtLeast(0)
    return candidates.take(minOf(charm.guess, room))
}

/**
 * An obstacle on the road: a puzzle with one try. Winning goes on; failing turns the hero back. With [own] it is a
 * person's own puzzle (their story is the costume, so there is no obstacle introduction, no tool that gets past it,
 * and what happens next is theirs to say), otherwise it wears the obstacle's costume that the child needs most.
 */
internal fun Journey.obstacle(
    o: Obstacle, scene: Scene, next: () -> List<JStep>, turnBack: () -> List<JStep>, own: Costume? = null,
): List<JStep> {
    if (own != null) {
        val c = puzzleFor(own.skill, own.intro, own.thing)
        return listOf(askOnce(scene, c, say.puzzleOops(), say.puzzleYay(), null, onWin = { next() }, onFail = { turnBack() }))
    }
    val c = obstaclePuzzle(o)
    return listOf(
        tell(scene, say.obstacleIntro(o)),
        askOnce(scene, c, say.obstacleOops(o), say.obstacleYay(o), o, onWin = { next() }, onFail = { listOf(tell(scene, say.failedFor(o))) + turnBack() }),
    )
}
