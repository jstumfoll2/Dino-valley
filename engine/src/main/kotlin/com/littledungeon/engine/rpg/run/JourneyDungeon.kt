package com.littledungeon.engine.rpg.run

import com.littledungeon.engine.model.Speech
import com.littledungeon.engine.model.Who
import com.littledungeon.engine.rpg.content.Content
import com.littledungeon.engine.rpg.hero.Attribute
import com.littledungeon.engine.rpg.items.Obstacle
import com.littledungeon.engine.rpg.learn.ChallengeFactory
import com.littledungeon.engine.rpg.learn.PictureFactory
import com.littledungeon.engine.rpg.learn.Skill
import com.littledungeon.engine.rpg.learn.Thing
import com.littledungeon.engine.rpg.learn.Words
import com.littledungeon.engine.rpg.battle.Monster
import com.littledungeon.engine.rpg.learn.Challenge
import com.littledungeon.engine.rpg.story.BOOK_PAGES
import com.littledungeon.engine.rpg.story.PeaceStep
import com.littledungeon.engine.rpg.world.Location
import com.littledungeon.engine.rpg.world.RoomKind

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

/** The rooms of a dungeon: the kinds of puzzle the hero has practised least come up most. Dealt once per journey. */
internal fun Journey.planOf(l: Location): List<RoomKind> = dungeonPlans.getOrPut(l.id) {
    RoomKind.learningRooms.sortedBy { kind ->
        val skill = kind.skill!!
        (skills.lastPracticed[skill] ?: 0L) / 60_000.0 + skills.level(skill) * 30.0 + random.nextDouble() * 120.0
    }.take(l.rooms)
}

private fun Journey.enterDungeon(l: Location): List<JStep> {
    val s = sceneAt(l)
    return listOf(tell(s, say.dungeonEnter(l.name))) + rooms(l, planOf(l), dungeonRooms[l.id] ?: 0)
}

private fun Journey.rooms(l: Location, plan: List<RoomKind>, i: Int): List<JStep> {
    if (i >= plan.size) return guardian(l)
    val kind = plan[i]
    val s = roomScene(kind)
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

/** Where a room of this kind is drawn, and who is in it. */
internal fun Journey.roomScene(kind: RoomKind): Scene =
    scene(placeOf(kind), *(if (kind == RoomKind.CRYSTAL_CAVE) arrayOf(Actor.WIZARD) else emptyArray()))

/** One room of one kind: a short scene, then its puzzle with one try. */
internal fun Journey.roomPuzzle(kind: RoomKind, s: Scene, solved: () -> List<JStep>, failed: () -> List<JStep>): List<JStep> {
    val seed = nextSeed()
    fun lv(skill: Skill) = level(skill)
    val (intro, c, oops, yay, obstacle) = when (kind) {
        RoomKind.RUNE_DOOR -> Room5(rooms.runeDoor(), ChallengeFactory.pattern(lv(Skill.PATTERNS), seed), rooms.runeOops(), rooms.runeYay(), Obstacle.RIDDLE)
        RoomKind.BRIDGE -> Room5(rooms.bridge(), ChallengeFactory.count(lv(Skill.COUNTING), seed, Thing.STONE, "How many stones are on the bridge?"), rooms.bridgeOops(), rooms.bridgeYay(), Obstacle.CROSS)
        RoomKind.CRYSTAL_CAVE -> Room5(rooms.crystalCave(), ChallengeFactory.color(lv(Skill.COLORS), seed, "", speaker = Who.WIZARD), rooms.crystalOops(), rooms.crystalYay(), Obstacle.DARK)
        RoomKind.LIBRARY -> Room5(rooms.library(), ChallengeFactory.letter(lv(Skill.LETTERS), seed, rooms.letterPurpose()), rooms.libraryOops(), rooms.libraryYay(), Obstacle.RIDDLE)
        RoomKind.TUNNEL -> Room5(rooms.tunnel(), tunnelTrace(lv(Skill.TRACING), seed, rooms.writePurpose()), rooms.tunnelOops(), rooms.tunnelYay(), Obstacle.DARK)
        RoomKind.MIRROR_HALL -> Room5(rooms.mirrorHall(), ChallengeFactory.memory(lv(Skill.MEMORY), seed), rooms.mirrorOops(), rooms.mirrorYay(), Obstacle.RIDDLE)
        RoomKind.VAULT -> Room5(
            rooms.vault(),
            ChallengeFactory.add(lv(Skill.ADDITION), seed, Thing.COIN, ::vaultStory),
            rooms.vaultOops(), rooms.vaultYay(), Obstacle.LOCK,
        )
        RoomKind.STOREROOM -> Room5(rooms.storeroom(), ChallengeFactory.sort(lv(Skill.SORTING), seed), rooms.storeroomOops(), rooms.storeroomYay(), Obstacle.RIDDLE)
        RoomKind.POND -> Room5(rooms.pond(), ChallengeFactory.skipCount(lv(Skill.SKIP_COUNTING), seed), rooms.pondOops(), rooms.pondYay(), Obstacle.CROSS)
        RoomKind.WORKSHOP -> Room5(
            rooms.workshop(),
            ChallengeFactory.recipe(lv(Skill.RECIPES), seed, com.littledungeon.engine.rpg.learn.PotionKind.entries[(seed and 0x7fffffff).toInt() % com.littledungeon.engine.rpg.learn.PotionKind.entries.size]),
            rooms.workshopOops(), rooms.workshopYay(), Obstacle.RIDDLE,
        )
        RoomKind.BELFRY -> Room5(rooms.belfry(), ChallengeFactory.bells(lv(Skill.LISTENING), seed), rooms.belfryOops(), rooms.belfryYay(), Obstacle.RIDDLE)
        else -> Room5(rooms.mosaic(), ChallengeFactory.puzzle(lv(Skill.PUZZLES), seed), rooms.mosaicOops(), rooms.mosaicYay(), Obstacle.RIDDLE)
    }
    return listOf(tell(s, intro), askOnce(s, c, oops, yay, obstacle, onWin = { solved() }, onFail = { listOf(tell(s, say.failedFor(obstacle))) + failed() }))
}

/** The words of the vault's sum (kept apart so the voice catalog can list every one). */
internal fun vaultStory(have: Int, more: Int, missing: Boolean): String =
    if (missing) {
        "The magic purse holds ${Words.number(have + more)} coins. You have ${Words.number(have)}. How many more do you need to fill it?"
    } else {
        "${if (have == 1) "There is one coin" else "There are ${Words.number(have)} coins"} in the chest, and ${Words.number(more)} more on the floor. How many coins is that altogether?"
    }

private data class Room5(val intro: String, val c: com.littledungeon.engine.rpg.learn.Challenge, val oops: String, val yay: String, val obstacle: Obstacle)

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
    // A boss made a friend in an earlier adventure asks for help instead of a fight; one who was beaten remembers it.
    if (hasFlag("friend:${boss.id}") && arc.friendMeeting != null) {
        steps += tell(bossScene, arc.friendMeeting)
        steps += peace(l, 0, friend = true)
        return steps
    }
    steps += tell(bossScene, if (hasFlag("rival:${boss.id}")) arc.rivalMeeting ?: variant.meeting else variant.meeting)
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
private fun Journey.peace(l: Location, i: Int, friend: Boolean = false): List<JStep> {
    val steps = if (friend && arc.friendSteps.isNotEmpty()) arc.friendSteps else arc.peaceSteps
    if (i >= steps.size) return ending(l, fought = false, friend = friend)
    val step = steps[i]
    val boss = Content.monster(arc.bossId)!!
    val s = sceneAt(l).copy(npc = NpcView(boss.id, boss.name, boss.art, boss.who))
    // Something learned along the road can spare the hero a puzzle: the boss sees they already understand.
    if (step.skippedBy != null && hasFlag(step.skippedBy)) {
        return listOfNotNull(step.skipNote?.let { tell(s, it) }) + peace(l, i + 1, friend)
    }
    val c = peaceChallenge(step, boss)
    return listOf(
        JStep(Beat.Ask(s, c, Speech.of(say.peaceOops()), Speech.of(step.yay))) { reply ->
            val r = reply as? Reply.Solved ?: Reply.Solved(1, 0, 0)
            record(c, r.tries, r.hints, r.millis)
            peace(l, i + 1, friend)
        },
    )
}

/** The puzzle a boss sets on the peaceful way, in the boss's own voice. */
internal fun Journey.peaceChallenge(step: PeaceStep, boss: Monster): Challenge = when (step.kind) {
    "letters" -> ChallengeFactory.letter(level(Skill.LETTERS), nextSeed(), step.intro)
    "pattern" -> ChallengeFactory.pattern(level(Skill.PATTERNS), nextSeed(), step.intro)
    "colors" -> ChallengeFactory.color(level(Skill.COLORS), nextSeed(), step.intro, "gem", speaker = boss.who)
    "numbers" -> ChallengeFactory.numeral(level(Skill.NUMBERS), nextSeed(), step.intro)
    "rhyme" -> PictureFactory.rhyme(level(Skill.RHYMES), nextSeed(), step.intro)
    "money" -> PictureFactory.money(level(Skill.MONEY), nextSeed(), step.intro)
    "share" -> PictureFactory.share(level(Skill.SHARING), nextSeed(), step.intro, PictureFactory.ShareTheme.PLATES)
    "bats" -> PictureFactory.share(level(Skill.SHARING), nextSeed(), step.intro, PictureFactory.ShareTheme.BATS)
    "map" -> PictureFactory.map(level(Skill.MAPS), nextSeed(), step.intro)
    "bells" -> ChallengeFactory.bells(level(Skill.LISTENING), nextSeed(), step.intro)
    "trace" -> tunnelTrace(level(Skill.TRACING), nextSeed(), step.intro, "to light the lanterns.").let { c ->
        // The words of a line or a shape have no room for the boss's own, so theirs go first; a letter's already begin with them.
        if (c.glyph == null) c.copy(prompt = Speech.of("${step.intro} ${com.littledungeon.engine.model.Voice.caption(c.prompt)}")) else c
    }
    else -> ChallengeFactory.count(level(Skill.COUNTING), nextSeed(), Thing.GEM, step.intro + " How many gems?")
}

/** The story ends: a page of the Storybook comes home, and what the child chose is remembered. */
private fun Journey.ending(l: Location, fought: Boolean, friend: Boolean = false): List<JStep> {
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
        // Made peace: a friend, and no longer a rival. Fought: remembered as the one who was beaten, unless already a friend.
        flags = if (fought) world.flags + "rival:${boss.id}" else world.flags - "rival:${boss.id}" + "friend:${boss.id}",
    )
    gain(Attribute.COURAGE, 20)
    lastEnding = id
    finishing = true
    val said = when {
        fought -> variant.fightEnd
        friend && arc.friendEnd != null -> arc.friendEnd
        else -> variant.peaceEnd
    }
    val steps = mutableListOf(tell(s, said))
    steps += item(s, "storybook_page").take(1)
    steps += tell(s, say.pageFound(), say.pagesLine(pagesNow))
    if (pagesNow % BOOK_PAGES == 0) steps += tell(s, say.bookWhole(), say.newBook())
    return steps
}

/**
 * Learning to write starts before letters: a line, a curve, a zigzag, a loop, a circle and a triangle (levels 1 and 2), then letters from
 * straight ones to twisty ones (levels 3 to 5). Writing letters from the first level skipped the strokes they are made of.
 */
internal fun tunnelTrace(level: Int, seed: Long, purpose: String, goal: String = "to light up the tunnel.") = when (level) {
    1 -> ChallengeFactory.trace(1 + (seed and 1L).toInt(), seed, goal)
    2 -> ChallengeFactory.trace(3 + ((seed and 0xffffL) % 3).toInt(), seed, goal)
    3 -> ChallengeFactory.write(1, seed, purpose)
    4 -> ChallengeFactory.write(2 + (seed and 1L).toInt(), seed, purpose)
    else -> ChallengeFactory.write(4 + (seed and 1L).toInt(), seed, purpose)
}
