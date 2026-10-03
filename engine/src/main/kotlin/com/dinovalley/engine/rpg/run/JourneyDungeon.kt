package com.dinovalley.engine.rpg.run

import com.dinovalley.engine.model.Speech
import com.dinovalley.engine.model.Who
import com.dinovalley.engine.rpg.content.Content
import com.dinovalley.engine.rpg.hero.Attribute
import com.dinovalley.engine.rpg.items.Obstacle
import com.dinovalley.engine.rpg.learn.ChallengeFactory
import com.dinovalley.engine.rpg.learn.Skill
import com.dinovalley.engine.rpg.learn.Thing
import com.dinovalley.engine.rpg.learn.Words
import com.dinovalley.engine.rpg.story.BOOK_PAGES
import com.dinovalley.engine.rpg.world.Location
import com.dinovalley.engine.rpg.world.RoomKind

// ------------------------------------------------------------- dungeons

/** A dungeon: a few puzzle rooms, then its guardian, who may hold what the story needs. */
internal fun Journey.dungeon(l: Location, first: Boolean): List<JStep> {
    val s = sceneAt(l)
    val steps = mutableListOf<JStep>()
    if (first) steps += tell(s, say.arriveWild(l.name), l.blurb)
    if ("dungeon:${l.id}" in done) return steps + tell(s, say.alreadyDone(l.name))
    val choices = listOf(Choice("hub_enter", "Go in"), Choice("hub_leave", "Not now"))
    steps += JStep(Beat.Choose(s, Speech.of(say.dungeonAsk(l.name)), choices)) { reply ->
        if ((reply as? Reply.Picked)?.index == 0) enterDungeon(l) else listOf(tell(s, say.dungeonSkip()))
    }
    return steps
}

private val dealt = HashMap<Pair<Journey, String>, List<RoomKind>>()

/** The rooms of a dungeon: the kinds of puzzle the hero has practised least come up most. */
private fun Journey.plan(l: Location): List<RoomKind> = synchronized(dealt) {
    dealt.getOrPut(this to l.id) {
        RoomKind.learningRooms.sortedBy { kind ->
            val skill = kind.skill!!
            (skills.lastPracticed[skill] ?: 0L) / 60_000.0 + skills.level(skill) * 30.0 + random.nextDouble() * 120.0
        }.take(l.rooms)
    }
}

private fun Journey.enterDungeon(l: Location): List<JStep> {
    val s = sceneAt(l)
    return listOf(tell(s, say.dungeonEnter(l.name))) + rooms(l, plan(l), dungeonRooms[l.id] ?: 0)
}

private fun Journey.rooms(l: Location, plan: List<RoomKind>, i: Int): List<JStep> {
    if (i >= plan.size) return guardian(l)
    val kind = plan[i]
    val s = scene(placeOf(kind), *(if (kind == RoomKind.CRYSTAL_CAVE) arrayOf(Actor.WIZARD) else emptyArray()))
    return roomPuzzle(
        kind, s,
        solved = {
            dungeonRooms[l.id] = i + 1
            val more = coins(s, 2 + random.nextInt(4)) + if (random.nextInt(100) < 25) item(s, listOf("berry", "rusty_key", "lucky_clover", "bubble_shield").random(random)) else emptyList()
            more + tell(s.copy(mood = Mood.HAPPY), say.roomCleared(), if (i + 1 < plan.size) say.roomsLeft(plan.size - i - 1) else null) + rooms(l, plan, i + 1)
        },
        failed = {
            dungeonRooms[l.id] = i
            listOf(tell(sceneAt(l), say.thrownOut(l.name)))
        },
    )
}

/** One room of one kind: a short scene, then its puzzle with one try. */
private fun Journey.roomPuzzle(kind: RoomKind, s: Scene, solved: () -> List<JStep>, failed: () -> List<JStep>): List<JStep> {
    val seed = nextSeed()
    fun lv(skill: Skill) = level(skill)
    val (intro, c, oops, yay, obstacle) = when (kind) {
        RoomKind.RUNE_DOOR -> Room5(rooms.runeDoor(), ChallengeFactory.pattern(lv(Skill.PATTERNS), seed), rooms.runeOops(), rooms.runeYay(), Obstacle.RIDDLE)
        RoomKind.BRIDGE -> Room5(rooms.bridge(), ChallengeFactory.count(lv(Skill.COUNTING), seed, Thing.STONE, "How many stones are on the bridge?"), rooms.bridgeOops(), rooms.bridgeYay(), Obstacle.CROSS)
        RoomKind.CRYSTAL_CAVE -> Room5(rooms.crystalCave(), ChallengeFactory.color(lv(Skill.COLORS), seed, "", speaker = Who.WIZARD), rooms.crystalOops(), rooms.crystalYay(), Obstacle.DARK)
        RoomKind.LIBRARY -> Room5(rooms.library(), ChallengeFactory.letter(lv(Skill.LETTERS), seed, rooms.letterPurpose()), rooms.libraryOops(), rooms.libraryYay(), Obstacle.RIDDLE)
        RoomKind.TUNNEL -> Room5(rooms.tunnel(), ChallengeFactory.write(lv(Skill.TRACING), seed, rooms.writePurpose()), rooms.tunnelOops(), rooms.tunnelYay(), Obstacle.DARK)
        RoomKind.MIRROR_HALL -> Room5(rooms.mirrorHall(), ChallengeFactory.memory(lv(Skill.MEMORY), seed), rooms.mirrorOops(), rooms.mirrorYay(), Obstacle.RIDDLE)
        RoomKind.VAULT -> Room5(
            rooms.vault(),
            ChallengeFactory.add(lv(Skill.ADDITION), seed, Thing.COIN) { have, more, missing ->
                if (missing) {
                    "The magic purse holds ${Words.number(have + more)} coins. You have ${Words.number(have)}. How many more do you need to fill it?"
                } else {
                    "${if (have == 1) "There is one coin" else "There are ${Words.number(have)} coins"} in the chest, and ${Words.number(more)} more on the floor. How many coins is that altogether?"
                }
            },
            rooms.vaultOops(), rooms.vaultYay(), Obstacle.LOCK,
        )
        RoomKind.STOREROOM -> Room5(rooms.storeroom(), ChallengeFactory.sort(lv(Skill.SORTING), seed), rooms.storeroomOops(), rooms.storeroomYay(), Obstacle.RIDDLE)
        RoomKind.POND -> Room5(rooms.pond(), ChallengeFactory.skipCount(lv(Skill.SKIP_COUNTING), seed), rooms.pondOops(), rooms.pondYay(), Obstacle.CROSS)
        else -> Room5(rooms.mosaic(), ChallengeFactory.puzzle(lv(Skill.PUZZLES), seed), rooms.mosaicOops(), rooms.mosaicYay(), Obstacle.RIDDLE)
    }
    return listOf(tell(s, intro), askOnce(s, c, oops, yay, obstacle, onWin = { solved() }, onFail = { listOf(tell(s, say.failedFor(obstacle))) + failed() }))
}

private data class Room5(val intro: String, val c: com.dinovalley.engine.rpg.learn.Challenge, val oops: String, val yay: String, val obstacle: Obstacle)

private fun Journey.guardian(l: Location): List<JStep> {
    val g = l.guardian?.let { Content.monster(it) } ?: return finishDungeon(l)
    val s = sceneAt(l)
    return listOf(tell(s, say.guardianAhead())) + battle(g, s.place, onWin = { finishDungeon(l) })
}

private fun Journey.finishDungeon(l: Location): List<JStep> {
    done += "dungeon:${l.id}"
    val s = sceneAt(l).copy(mood = Mood.HAPPY)
    val steps = mutableListOf<JStep>()
    // The key the story needs is always here, even if the guardian did not drop it.
    if (l.id == arc.keyDungeonId && !hero.has(arc.keyItemId)) steps += item(s, arc.keyItemId)
    if (l.id == arc.keyDungeonId) steps += tell(s, arc.keyFound)
    steps += loot(s)
    steps += tell(s, say.dungeonDone(l.name))
    return steps
}

// ------------------------------------------------------------- the lair

internal fun Journey.lairArrival(l: Location): List<JStep> {
    val s = sceneAt(l)
    val boss = Content.monster(arc.bossId)!!
    val steps = mutableListOf(tell(s, say.arriveWild(l.name), l.blurb))
    if (!hero.has(arc.keyItemId)) return steps + tell(s, arc.sealed)
    if ("run:gate" !in runFlags) {
        runFlags += "run:gate"
        steps += tell(s, arc.gateOpens)
    }
    val bossScene = s.copy(npc = NpcView(boss.id, boss.name, boss.art, boss.who))
    steps += tell(bossScene, variant.meeting)
    val choices = listOf(Choice("hub_fight", arc.fightLabel), Choice("hub_peace", arc.peaceLabel))
    steps += JStep(Beat.Choose(bossScene, Speech.of(arc.ask), choices)) { reply ->
        if ((reply as? Reply.Picked)?.index == 1) {
            gain(Attribute.KINDNESS, 10)
            peace(l, 0)
        } else {
            gain(Attribute.COURAGE, 10)
            battle(boss, s.place, onWin = { ending(l, fought = true) })
        }
    }
    return steps
}

/** The peaceful way: the boss sets puzzles, and there is no fighting. These are forgiving, because this is talking. */
private fun Journey.peace(l: Location, i: Int): List<JStep> {
    if (i >= arc.peaceSteps.size) return ending(l, fought = false)
    val step = arc.peaceSteps[i]
    val boss = Content.monster(arc.bossId)!!
    val s = sceneAt(l).copy(npc = NpcView(boss.id, boss.name, boss.art, boss.who))
    val lvl = { skill: Skill -> level(skill) }
    val c = when (step.kind) {
        "letters" -> ChallengeFactory.letter(lvl(Skill.LETTERS), nextSeed(), step.intro)
        "pattern" -> ChallengeFactory.pattern(lvl(Skill.PATTERNS), nextSeed(), step.intro)
        "colors" -> ChallengeFactory.color(lvl(Skill.COLORS), nextSeed(), step.intro, "gem", speaker = boss.who)
        "numbers" -> ChallengeFactory.numeral(lvl(Skill.NUMBERS), nextSeed(), step.intro)
        else -> ChallengeFactory.count(lvl(Skill.COUNTING), nextSeed(), Thing.GEM, step.intro + " How many gems?")
    }
    return listOf(
        JStep(Beat.Ask(s, c, Speech.of(say.peaceOops()), Speech.of(step.yay))) { reply ->
            val r = reply as? Reply.Solved ?: Reply.Solved(1, 0, 0)
            record(c, r.tries, r.hints, r.millis)
            peace(l, i + 1)
        },
    )
}

/** The story ends: a page of the Storybook comes home, and what the child chose is remembered. */
private fun Journey.ending(l: Location, fought: Boolean): List<JStep> {
    val boss = Content.monster(arc.bossId)!!
    val s = sceneAt(l).copy(mood = Mood.HAPPY, cleared = true, npc = NpcView(boss.id, boss.name, boss.art, boss.who))
    val id = "${arc.id}_${variant.id}_${if (fought) "fight" else "peace"}"
    val pagesNow = world.pages + 1
    world = world.copy(
        adventures = world.adventures + 1,
        arcsDone = world.arcsDone + (arc.id to (world.arcsDone[arc.id] ?: 0) + 1),
        lastArc = arc.id,
        pages = pagesNow,
        endings = world.endings + id,
        flags = if (fought) world.flags else world.flags + "friend:${boss.id}",
    )
    gain(Attribute.COURAGE, 20)
    lastEnding = id
    finishing = true
    val steps = mutableListOf(tell(s, if (fought) variant.fightEnd else variant.peaceEnd))
    steps += item(s, "storybook_page").take(1)
    steps += tell(s, say.pageFound(), say.pagesLine(pagesNow))
    if (pagesNow % BOOK_PAGES == 0) steps += tell(s, say.bookWhole(), say.newBook())
    return steps
}
