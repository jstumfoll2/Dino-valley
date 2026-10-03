package com.dinovalley.engine.rpg.run

import com.dinovalley.engine.model.Speech
import com.dinovalley.engine.rpg.battle.Monster
import com.dinovalley.engine.rpg.content.Content
import com.dinovalley.engine.rpg.items.Item
import com.dinovalley.engine.rpg.items.Obstacle
import com.dinovalley.engine.rpg.learn.Challenge
import com.dinovalley.engine.rpg.learn.ChallengeFactory
import com.dinovalley.engine.rpg.learn.PickOne
import com.dinovalley.engine.rpg.learn.Skill
import com.dinovalley.engine.rpg.learn.Thing
import com.dinovalley.engine.rpg.learn.Words

/** The one-try puzzle for something blocking the way. All are pick-one puzzles so a try means one tap. */
internal fun Journey.obstaclePuzzle(o: Obstacle): PickOne = when (o) {
    Obstacle.CLIMB -> ChallengeFactory.count(level(Skill.COUNTING), nextSeed(), Thing.STONE, "Count the stones in the staircase. How many stones are there?")
    Obstacle.CROSS -> ChallengeFactory.skipCount(level(Skill.SKIP_COUNTING), nextSeed(), "Hop across the river on the lily pads.")
    Obstacle.DARK -> ChallengeFactory.pattern(level(Skill.PATTERNS), nextSeed(), "Glowing marks show the safe path. Which mark comes next?")
    Obstacle.LOCK -> ChallengeFactory.numeral(level(Skill.NUMBERS), nextSeed(), "The lock wants a number.")
    Obstacle.RIDDLE -> ChallengeFactory.letter(level(Skill.LETTERS), nextSeed(), "The stone face wants a letter.")
}

/** A monster's attack puzzle: one of its own skills, dressed in its name. */
internal fun Journey.battlePuzzle(m: Monster, skill: Skill): PickOne {
    val lvl = level(skill)
    val seed = nextSeed()
    return when (skill) {
        Skill.COUNTING -> ChallengeFactory.count(lvl, seed, Thing.COIN, "The ${m.name} hid some coins. How many coins can you count?")
        Skill.NUMBERS -> ChallengeFactory.numeral(lvl, seed, "The ${m.name} holds up a card.")
        Skill.ADDITION -> ChallengeFactory.add(lvl, seed, Thing.GEM) { have, more, missing ->
            if (missing) {
                "The ${m.name} wants ${Words.number(have + more)} gems and has ${Words.number(have)}. How many more does it need?"
            } else {
                "The ${m.name} has ${Words.number(have)} ${Thing.GEM.words(have)} and finds ${Words.number(more)} more. How many gems is that altogether?"
            }
        }
        Skill.COLORS -> ChallengeFactory.color(lvl, seed, "The ${m.name}", "gem")
        Skill.PATTERNS -> ChallengeFactory.pattern(lvl, seed, "The ${m.name} makes a pattern. Which symbol comes next?")
        Skill.LETTERS -> ChallengeFactory.letter(lvl, seed, "The ${m.name} scribbles a letter.")
        Skill.SKIP_COUNTING -> ChallengeFactory.skipCount(lvl, seed, "The ${m.name} hops along.")
        else -> ChallengeFactory.numeral(lvl, seed, "The ${m.name} holds up a card.")
    }
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
    return JStep(Beat.Ask(scene, c, Speech.of(oops), Speech.of(yay), prop, oneTry = true, allowedMisses = misses, tried = tried)) { reply ->
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
    val owned = hero.bag.keys.mapNotNull { Content.item(it) }
    val tool = if (obstacle != null) owned.firstOrNull { obstacle in it.opens } else null
    val charms = if (charmUsed) emptyList() else owned.filter { it.isCharm }.sortedBy { it.guess }.take(2)
    if (tool == null && charms.isEmpty()) return onFail()
    val options = buildList {
        tool?.let { add(it to "tool") }
        charms.forEach { add(it to "charm") }
    }
    val choices = options.map { (item, _) -> Choice("item_${item.id}", "Use the ${item.name}") } + Choice("hub_leave", "Give up")
    val ask = Speech.of(say.rescueAsk("the " + options.joinToString(" and the ") { it.first.name }))
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
                    hero = hero.take(picked.first.id)
                    val more = extraWrong(c, picked.first, wrong)
                    listOf(
                        tell(scene, say.charmWorks(picked.first.name)),
                        askOnce(scene, c, oops, yay, obstacle, prop, wrong + more, charmUsed = true, onWin = onWin, onFail = onFail),
                    )
                }
            }
        },
    )
}

/** Wrong answers a charm takes away: as many as it says, never the right one or one already out. */
internal fun Journey.extraWrong(c: Challenge, charm: Item, alreadyOut: List<Int>): List<Int> {
    if (c !is PickOne || charm.guess <= 0) return emptyList()
    val candidates = (0 until c.optionCount).filter { it != c.answer && it !in alreadyOut }.shuffled(random)
    // Always leave at least two answers standing, so it is still a real choice.
    val room = (c.optionCount - alreadyOut.size - 2).coerceAtLeast(0)
    return candidates.take(minOf(charm.guess, room))
}

/** An obstacle on the road: a puzzle with one try. Winning goes on; failing turns the hero back. */
internal fun Journey.obstacle(o: Obstacle, scene: Scene, next: () -> List<JStep>, turnBack: () -> List<JStep>): List<JStep> {
    val c = obstaclePuzzle(o)
    return listOf(
        tell(scene, say.obstacleIntro(o)),
        askOnce(scene, c, say.obstacleOops(o), say.obstacleYay(o), o, onWin = { next() }, onFail = { listOf(tell(scene, say.failedFor(o))) + turnBack() }),
    )
}
