package com.littledungeon.engine.rpg.run

import com.littledungeon.engine.model.Speech
import com.littledungeon.engine.rpg.content.Content
import com.littledungeon.engine.rpg.hero.Attribute
import com.littledungeon.engine.rpg.hero.Hero
import com.littledungeon.engine.rpg.hero.Progression
import com.littledungeon.engine.rpg.items.Item
import com.littledungeon.engine.rpg.items.Slot
import com.littledungeon.engine.rpg.learn.Challenge
import com.littledungeon.engine.rpg.learn.ChallengeRecord
import com.littledungeon.engine.rpg.learn.Skill
import com.littledungeon.engine.rpg.learn.SkillBook
import com.littledungeon.engine.rpg.story.Arc
import com.littledungeon.engine.rpg.story.ArcPicker
import com.littledungeon.engine.rpg.story.Cond
import com.littledungeon.engine.rpg.story.Npc
import com.littledungeon.engine.rpg.story.Variant
import com.littledungeon.engine.rpg.world.Kingdom
import com.littledungeon.engine.rpg.world.Location
import com.littledungeon.engine.rpg.world.Road
import com.littledungeon.engine.rpg.world.Terrain
import com.littledungeon.engine.rpg.world.WorldMemory
import com.littledungeon.engine.util.Clock
import kotlin.random.Random

/** One moment lined up to show, and what to line up after the child answers it. */
internal class JStep(val beat: Beat, val then: (Reply) -> List<JStep> = { emptyList() })

/**
 * One adventure across the kingdom of Whisperwood, from the camp to the lair. It hands out [beat]s
 * one at a time; the app shows each and calls [reply] with what the child did.
 *
 * The child picks the road at every turn, so the story is theirs: towns to shop and talk in, roads
 * with their own dangers, dungeons to explore for loot (one of them required), fights with real
 * rounds, and a boss at the end who can be beaten or befriended. Each person in the kingdom has a
 * life of their own, and what the hero does for them is remembered and can open roads, give tools,
 * or change how the story ends. See `docs/design/story-engine-plan.md`.
 *
 * The parts live in extension files (travel, towns, battles, puzzles, the lair) that share this state.
 */
class Journey(
    val seed: Long,
    startHero: Hero,
    startSkills: SkillBook,
    startWorld: WorldMemory,
    internal val clock: Clock = Clock.System,
) {
    internal val random = Random(seed)
    internal val say = JourneyLines(Random(random.nextLong()))
    internal val rooms = RoomLines(Random(random.nextLong()))

    val kingdom: Kingdom = Content.kingdom
    val arc: Arc = ArcPicker.pick(startWorld, random)
    val variant: Variant = arc.variants.random(random)

    var hero: Hero = startHero
        internal set
    var skills: SkillBook = startSkills
        internal set
    var world: WorldMemory = startWorld
        internal set
    val records = mutableListOf<ChallengeRecord>()

    internal val levelBefore = startHero.level
    internal val coinsBefore = startHero.coins
    internal val stars = mutableMapOf<Attribute, Int>()

    // ------------------------------------------------------------- where we are, and what has happened

    /** The id of the place the hero is at. */
    var here: String = kingdom.camp.id
        internal set

    val visited = mutableSetOf(kingdom.camp.id)

    /** Health now; the most is [Hero.maxHp]. */
    var hp: Int = startHero.maxHp
        internal set

    internal val runFlags = mutableSetOf<String>()
    internal val opened = mutableSetOf<String>()
    internal val closed = mutableMapOf<String, Int>()
    internal var moves = 0
    internal val done = mutableSetOf<String>()
    internal val dungeonRooms = mutableMapOf<String, Int>()
    internal var monstersBeaten = 0
    internal var faints = 0
    internal var slips = 0
    internal var finishing = false
    internal var lastEnding = ""

    /** What the hero is carrying and how they are doing, for the corner of the screen. */
    data class Bag(val hp: Int, val maxHp: Int, val coins: Int, val hasKey: Boolean, val pages: Int)

    val bag: Bag get() = Bag(hp, hero.maxHp, hero.coins, hero.has(arc.keyItemId), world.pages)

    /** The shape of the Storybook so far: how many pages are home. */
    val pages: Int get() = world.pages

    internal val queue = ArrayDeque<JStep>()

    var beat: Beat
        private set

    val finished: Boolean get() = beat is Beat.Finale

    /** The beats already lined up after this one, so the app can get their words ready early. */
    val upcoming: List<Beat> get() = queue.drop(1).map { it.beat }

    init {
        // Friends made in earlier adventures keep their roads open.
        Content.flagRoads.forEach { (flag, roads) -> if (flag in world.flags) opened += roads }
        queue += camp()
        beat = queue.first().beat
    }

    /** Replaces what is lined up (for tests and tools that set up a particular moment). */
    internal fun load(steps: List<JStep>) {
        queue.clear()
        queue += steps
        beat = queue.first().beat
    }

    fun reply(reply: Reply) {
        if (finished) return
        val step = queue.removeFirst()
        val follow = step.then(reply)
        for (s in follow.asReversed()) queue.addFirst(s)
        if (queue.isEmpty()) queue += nextAction()
        beat = queue.first().beat
    }

    // ------------------------------------------------------------- what the hero can do any time

    /** Puts on gear from the bag; health never goes above the new most. */
    fun equip(itemId: String) {
        val item = Content.item(itemId) ?: return
        hero = hero.wear(item)
        hp = hp.coerceAtMost(hero.maxHp)
    }

    fun unequip(slot: Slot) {
        hero = hero.unwear(slot)
        hp = hp.coerceAtMost(hero.maxHp)
    }

    /** Uses a healing item out of a fight; false if it can't be used now. */
    fun eat(itemId: String): Boolean {
        val item = Content.item(itemId) ?: return false
        if (item.use != com.littledungeon.engine.rpg.items.BattleUse.HEAL || !hero.has(itemId) || hp >= hero.maxHp) return false
        hero = hero.take(itemId)
        hp = (hp + item.power + hero.healBonus).coerceAtMost(hero.maxHp)
        return true
    }

    // ------------------------------------------------------------- helpers shared by the parts

    internal val cast: Set<Actor> get() = setOf(Actor.HERO, Actor.COMPANION)

    internal fun scene(
        place: Place, vararg extra: Actor, mood: Mood = Mood.CALM, cleared: Boolean = false,
        npc: NpcView? = null, battle: BattleView? = null,
    ) = Scene(place, cast + extra, mood, cleared, npc, battle)

    /** The backdrop for where the hero is. */
    internal fun placeOf(l: Location): Place = Place(l.theme)

    internal fun view(npc: Npc) = NpcView(npc.id, npc.name, npc.art, npc.who)

    internal fun tell(scene: Scene, vararg text: String?): JStep =
        JStep(Beat.Tell(scene, text.filterNotNull().filter { it.isNotBlank() }.flatMap { Speech.of(it) }))

    internal fun gain(attribute: Attribute, amount: Int) {
        hero = hero.gain(attribute, amount)
        stars[attribute] = (stars[attribute] ?: 0) + amount
    }

    /** The puzzle level for a skill: what the child has reached, pushed up as the hero grows. */
    internal fun level(skill: Skill): Int = (skills.level(skill) + hero.puzzleBoost).coerceAtMost(5)

    internal fun nextSeed() = random.nextLong()

    /** Notes a finished puzzle. A failed one counts as a miss, never as a drop in level. */
    internal fun record(c: Challenge, tries: Int, hints: Int, millis: Long, failed: Boolean = false) {
        val r = ChallengeRecord(c.skill, c.kind, c.level, if (failed) 2 else tries.coerceAtLeast(1), hints, millis, clock.nowMillis(), c.seed)
        records += r
        skills = skills.record(r)
        gain(c.skill.attribute, if (r.firstTry) 15 else 10)
    }

    internal fun found(scene: Scene, loot: Loot, text: String): JStep = JStep(Beat.Found(scene, loot, Speech.of(text)))

    /** Coins found, with a little more for a wise hero. */
    internal fun coins(scene: Scene, base: Int, text: ((Int) -> String)? = null): List<JStep> {
        val n = (base * (100 + hero.lootBonusPercent) / 100).coerceAtLeast(if (base > 0) 1 else 0)
        if (n <= 0) return emptyList()
        hero = hero.earn(n)
        return listOf(found(scene.copy(mood = Mood.HAPPY), Loot(LootKind.COINS, n, say.coinsFound(n)), text?.invoke(n) ?: say.coinsFound(n)))
    }

    /** An item added to the bag, shown as found. */
    internal fun item(scene: Scene, itemId: String, n: Int = 1): List<JStep> {
        val item = Content.item(itemId) ?: return emptyList()
        hero = hero.give(itemId, n)
        return listOf(found(scene.copy(mood = Mood.HAPPY), Loot(LootKind.ITEM, n, item.name, itemId = itemId), say.itemFound(item.name)))
    }

    internal fun heal(n: Int) {
        hp = (hp + n).coerceAtMost(hero.maxHp)
    }

    // ------------------------------------------------------------- flags and conditions

    internal fun hasFlag(name: String): Boolean = if (name.startsWith("run:")) name in runFlags else name in world.flags

    internal fun setFlag(name: String) {
        if (name.startsWith("run:")) runFlags += name else world = world.copy(flags = world.flags + name)
        Content.flagRoads[name]?.let { opened += it }
    }

    internal fun clearFlag(name: String) {
        if (name.startsWith("run:")) runFlags -= name else world = world.copy(flags = world.flags - name)
    }

    internal fun relation(npcId: String): Int = world.relations[npcId] ?: 0

    internal fun befriend(npcId: String, delta: Int) {
        world = world.copy(relations = world.relations + (npcId to relation(npcId) + delta))
    }

    internal fun holds(c: Cond): Boolean = when (c) {
        is Cond.HasItem -> hero.count(c.itemId) >= c.n
        is Cond.Coins -> hero.coins >= c.atLeast
        is Cond.Stat -> hero.statLevel(c.attribute) >= c.atLeast
        is Cond.Flag -> hasFlag(c.name)
        is Cond.NoFlag -> !hasFlag(c.name)
        is Cond.Friend -> relation(c.npcId) >= c.atLeast
        is Cond.Chapter -> ArcPicker.chapter(world) >= c.atLeast
        is Cond.ArcIs -> arc.id == c.arcId
        is Cond.Not -> !holds(c.cond)
    }

    internal fun holds(all: List<Cond>): Boolean = all.all { holds(it) }

    // ------------------------------------------------------------- roads

    /** What a road is like right now: opened roads are easy, others as built. */
    internal fun terrainOf(road: Road): Terrain = if (road.id in opened) Terrain.ROAD else road.terrain

    internal fun isClosed(road: Road): Boolean = (closed[road.id] ?: 0) > moves

    // ------------------------------------------------------------- the story, start to end

    private fun camp(): List<JStep> {
        val s = scene(Place.CAMP)
        val chapter = ArcPicker.chapter(world)
        val steps = mutableListOf<JStep>()
        // Before each adventure: which chapter of the Storybook this is, and how many pages are home.
        steps += tell(s, say.chapter(chapter, arc.title))
        if (world.adventures > 0 && world.pages > 0) steps += tell(s, say.pagesLine(world.pages))
        arc.setup.forEach { steps += tell(s, it) }
        steps += tell(JourneyStory.mapScene(this), JourneyStory.toTheMap(this, chapter))
        return steps
    }

    private fun nextAction(): List<JStep> = if (finishing) finale() else travel()
}

internal object JourneyStory {
    fun mapScene(j: Journey): Scene = Scene(Place.WORLD_MAP, j.cast)

    fun toTheMap(j: Journey, chapter: Int): String =
        "This is the kingdom of Whisperwood. You are at ${j.kingdom.camp.name}, and the story ends at ${j.kingdom.location(j.arc.lairId).name}. Many roads lead there. Visit the towns, make friends, and find what you need. Every friend you make could help!"
}

/** The XP and level bookkeeping used when an adventure ends. */
internal fun Journey.finale(): List<JStep> {
    val earned = stars.values.sum()
    val levelAfter = hero.level
    val unlocked = Progression.unlocksBetween(levelBefore, levelAfter)
    val coinsHome = (hero.coins - coinsBefore).coerceAtLeast(0)
    val said = buildList {
        add(say.finale(earned))
        add(say.pagesLine(world.pages))
        if (coinsHome > 0) add(say.coinsEarned(coinsHome))
        if (monstersBeaten > 0) add(say.killsLine(monstersBeaten))
        if (levelAfter > levelBefore) add(say.levelUp(levelAfter))
        unlocked.forEach { add(it.announcement) }
    }
    val summary = Summary(
        said.flatMap { Speech.of(it) }, stars.toMap(), levelBefore, levelAfter, unlocked,
        "a page of the Storybook", lastEnding, coinsHome, world.pages, monstersBeaten,
    )
    return listOf(JStep(Beat.Finale(Scene(Place.CAMP, cast, Mood.HAPPY), summary)))
}

/** A short list of the items that can be used now: for menus and the bag. */
val Journey.usableItems: List<Item>
    get() = hero.bag.keys.mapNotNull { Content.item(it) }
