package com.dinovalley.engine.rpg.run

import com.dinovalley.engine.model.Speech
import com.dinovalley.engine.model.Who
import com.dinovalley.engine.rpg.hero.Attribute
import com.dinovalley.engine.rpg.hero.Hero
import com.dinovalley.engine.rpg.hero.Power
import com.dinovalley.engine.rpg.hero.Progression
import com.dinovalley.engine.rpg.learn.Challenge
import com.dinovalley.engine.rpg.learn.ChallengeFactory
import com.dinovalley.engine.rpg.learn.ChallengeRecord
import com.dinovalley.engine.rpg.learn.Hue
import com.dinovalley.engine.rpg.learn.MapChallenge
import com.dinovalley.engine.rpg.learn.MapDoor
import com.dinovalley.engine.rpg.learn.PotionKind
import com.dinovalley.engine.rpg.learn.Skill
import com.dinovalley.engine.rpg.learn.SkillBook
import com.dinovalley.engine.rpg.learn.Thing
import com.dinovalley.engine.rpg.learn.Words
import com.dinovalley.engine.rpg.world.Boss
import com.dinovalley.engine.rpg.world.DungeonGenerator
import com.dinovalley.engine.rpg.world.DungeonMap
import com.dinovalley.engine.rpg.world.Quest
import com.dinovalley.engine.rpg.world.QuestKind
import com.dinovalley.engine.rpg.world.QuestWriter
import com.dinovalley.engine.rpg.world.Room
import com.dinovalley.engine.rpg.world.RoomKind
import com.dinovalley.engine.rpg.world.Stop
import com.dinovalley.engine.rpg.world.WorldMemory
import com.dinovalley.engine.util.Clock
import kotlin.random.Random

/**
 * One adventure, from the camp to the treasure. It hands out [beat]s one at a time; the app
 * shows each and calls [reply] with what the child did. Rooms are generated as they're
 * reached, so a skill that levels up mid-adventure gets harder right away.
 *
 * The four parts from the brief meet here: the dungeon and quest (narrative), the challenge
 * factory and skill book (learning), dice and items (mechanics), and the hero's stars and
 * levels (progression).
 */
class Adventure(
    val seed: Long,
    startHero: Hero,
    startSkills: SkillBook,
    startWorld: WorldMemory,
    private val clock: Clock = Clock.System,
) {
    private val random = Random(seed)
    private val lines = Lines(Random(random.nextLong()))
    val map: DungeonMap = DungeonGenerator.generate(random.nextLong(), startSkills)
    val quest: Quest = QuestWriter.write(random.nextLong(), map.name, startWorld)

    var hero: Hero = startHero
        private set
    var skills: SkillBook = startSkills
        private set
    var world: WorldMemory = startWorld
        private set
    val records = mutableListOf<ChallengeRecord>()

    private val levelBefore = startHero.level
    private val stars = mutableMapOf<Attribute, Int>()

    // What happened on this adventure.
    private var stopIndex = -1
    private var treasureDoorTaken = false
    private var coins = 0
    private var goblinFriend = false
    private var magicKey = false
    private var potionMade = false
    private var bossStars = 0
    private var powerUsed = false
    private var hearts = MAX_HEARTS
    private var chests = 0
    private val taken = mutableMapOf<Int, Room>()

    /** What the hero is carrying, for the bag in the corner of the screen. */
    data class Bag(val coins: Int, val key: Boolean, val potion: PotionKind?, val gems: List<Hue>, val friends: Int, val hearts: Int)

    val bag: Bag get() = Bag(coins, magicKey, if (potionMade) quest.potion else null, gems.toList(), if (goblinFriend) 1 else 0, hearts)

    /** Where the hero is on the map: the index of the current stop, or -1 at the camp. */
    val position: Int get() = stopIndex

    /** The room picked at each fork so far, by stop index, for drawing the map. */
    val route: Map<Int, Room> get() = taken.toMap()

    val bossStarsLit: Int get() = bossStars

    private val gems = mutableListOf<Hue>()

    /** A door taken at a fork. [looped]: it was not the clue's door, so its path wound back to the same doors. */
    data class Visit(val stop: Int, val door: Int, val looped: Boolean)

    private val trail = mutableListOf<Visit>()

    /** Every door taken so far, in order, for drawing the path on the map (loops included). */
    val visits: List<Visit> get() = trail.toList()

    private class Step(val beat: Beat, val then: (Reply) -> List<Step> = { emptyList() })

    private val queue = ArrayDeque<Step>()

    var beat: Beat
        private set

    val finished: Boolean get() = beat is Beat.Finale

    /** The beats already lined up after this one, so the app can get their words ready early. */
    val upcoming: List<Beat> get() = queue.drop(1).map { it.beat }

    init {
        queue += camp()
        beat = queue.first().beat
    }

    fun reply(reply: Reply) {
        if (finished) return
        val step = queue.removeFirst()
        val follow = step.then(reply)
        for (s in follow.asReversed()) queue.addFirst(s)
        if (queue.isEmpty()) queue += nextStop()
        beat = queue.first().beat
    }

    // ------------------------------------------------------------- helpers

    private val cast: Set<Actor>
        get() = buildSet {
            add(Actor.HERO)
            add(Actor.COMPANION)
            if (quest.kind == QuestKind.RUBYS_QUEST) add(Actor.RUBY)
        }

    private fun scene(place: Place, vararg extra: Actor, mood: Mood = Mood.CALM, cleared: Boolean = false) =
        Scene(place, cast + extra, mood, if (place == Place.LAIR) bossStars else null, cleared)

    private fun tell(scene: Scene, vararg text: String?): Step =
        Step(Beat.Tell(scene, text.filterNotNull().flatMap { Speech.of(it) }))

    private fun gain(attribute: Attribute, amount: Int) {
        hero = hero.gain(attribute, amount)
        stars[attribute] = (stars[attribute] ?: 0) + amount
    }

    private fun level(skill: Skill) = skills.level(skill)

    private fun nextSeed() = random.nextLong()

    private fun record(c: Challenge, tries: Int, hints: Int, millis: Long) {
        val r = ChallengeRecord(c.skill, c.kind, c.level, tries.coerceAtLeast(1), hints, millis, clock.nowMillis(), c.seed)
        records += r
        skills = skills.record(r)
        gain(c.skill.attribute, if (r.firstTry) 15 else 10)
    }

    /** A challenge step; [after] runs once it's solved. */
    private fun ask(scene: Scene, c: Challenge, oops: String, yay: String, prop: Prop = Prop.NONE, after: () -> List<Step> = { emptyList() }): Step =
        Step(Beat.Ask(scene, c, Speech.of(oops), Speech.of(yay), prop)) { reply ->
            val s = reply as? Reply.Solved ?: Reply.Solved(1, 0, 0)
            record(c, s.tries, s.hints, s.millis)
            after()
        }

    /**
     * A dice step: roll two dice and add them up. Courage grows with every roll. Getting the
     * total wrong on the first try costs a heart; [result] tells what the roll did.
     */
    private fun roll(scene: Scene, why: String, result: (Tier, Int) -> List<Step>): Step {
        val first = List(2) { random.nextInt(1, 7) }
        val second = List(2) { random.nextInt(1, 7) }
        val knight = hero.heroClass.power == Power.BRAVE_REROLL && !powerUsed
        val beat = Beat.Roll(scene, Speech.of(why), first, if (knight && first.sum() <= 4) second else null)
        return Step(beat) { reply ->
            val rolled = reply as? Reply.Rolled ?: Reply.Rolled(false, 1)
            val dice = if (rolled.usedReroll && beat.reroll != null) {
                powerUsed = true
                listOf(first, beat.reroll).maxBy { it.sum() }
            } else {
                first
            }
            gain(Attribute.COURAGE, 5)
            // Adding the dice is an addition challenge in its own right.
            val c = ChallengeFactory.add(level(Skill.ADDITION), nextSeed(), Thing.GEM) { _, _, _ -> "What is ${dice[0]} plus ${dice[1]}?" }
            val tries = rolled.sumTries.coerceAtLeast(1)
            record(c, tries, (tries - 1).coerceAtMost(2), 0)
            val revive = if (tries > 1) {
                hearts -= 1
                if (hearts <= 0) {
                    hearts = MAX_HEARTS
                    listOf(tell(scene, lines.heartsBack()))
                } else {
                    emptyList()
                }
            } else {
                emptyList()
            }
            // Higher levels bring a little luck: the hero's bonus nudges the result, not the sum.
            revive + result(Tier.of(dice.sum() + hero.diceBonus - 1), dice.sum())
        }
    }

    private fun found(scene: Scene, loot: Loot, text: String): Step = Step(Beat.Found(scene, loot, Speech.of(text)))

    // ------------------------------------------------------------- the route

    private fun camp(): List<Step> {
        val s = scene(Place.CAMP)
        return listOf(
            tell(s, lines.intro(quest)),
            tell(s, lines.remember(world, hero.strongest)),
            tell(Scene(Place.MAP, cast), lines.toTheMap(quest)),
        )
    }

    private fun nextStop(): List<Step> {
        stopIndex++
        val stop = map.stops.getOrNull(stopIndex) ?: return finale()
        return when (stop) {
            is Stop.Landmark -> room(stop.room)
            is Stop.Fork -> listOf(doors(stop))
        }
    }

    /**
     * Pick a door. With a clue, one door has the treasure. Any other door still has a room to
     * play, but its path winds back round to the same doors, so the child gets another go with
     * that door closed, until only the clue's door is left.
     */
    private fun doors(fork: Stop.Fork, tried: Set<Int> = emptySet()): Step {
        val s = Scene(Place.MAP, cast)
        val clue = fork.treasureDoor?.let { target ->
            ChallengeFactory.map(level(Skill.MAPS), nextSeed(), fork.doors.map { MapDoor(it.hue, it.side) }, target, lines.treasureSniff())
        }
        val prompt = clue?.prompt ?: Speech.of(lines.pickDoor())
        val peek = hero.heroClass.power == Power.KEEN_EYES
        val open = fork.doors.indices.filter { it !in tried }
        val offers = fork.doors.mapIndexed { i, d ->
            if (i in tried) emptyList() else Speech.of(lines.doorOffer(d.hue.word.uppercase(), d.kind.activity, open.indexOf(i), open.lastIndex))
        }
        return Step(Beat.Doors(s, prompt, fork, clue, peek, stopIndex, offers, tried)) { reply ->
            val picked = reply as? Reply.Picked ?: Reply.Picked(open.first())
            val index = picked.index.takeIf { it in open } ?: open.first()
            enter(fork, index, clue, tried, s)
        }
    }

    /** Goes through the door [index]: its room, its chest, and, for a wrong door, back to the doors. */
    private fun enter(fork: Stop.Fork, index: Int, clue: MapChallenge?, tried: Set<Int>, s: Scene): List<Step> {
        val room = fork.doors[index]
        taken[stopIndex] = room
        val right = clue == null || index == clue.answer
        trail += Visit(stopIndex, index, looped = !right)
        if (clue != null && tried.isEmpty()) record(clue, if (right) 1 else 2, 0, 0)
        treasureDoorTaken = clue != null && right
        val said = when {
            clue == null -> emptyList()
            right -> listOf(tell(s, if (tried.isEmpty()) lines.rightWay() else lines.lastWay()))
            else -> listOf(tell(s, lines.wrongWay()))
        }
        val back = if (right) {
            emptyList()
        } else {
            val left = fork.doors.indices.filter { it !in tried && it != index }
            if (left.size == 1) {
                // Only the clue's door is left: no need to ask. It is entered once this has been said.
                listOf(Step(Beat.Tell(s, Speech.of(lines.circleBack()))) { enter(fork, left.single(), clue, tried + index, s) })
            } else {
                listOf(tell(s, lines.circleBack()), doors(fork, tried + index))
            }
        }
        return said + room(room) + chest(room) + back
    }

    /**
     * A small chest with a magic lock after each room behind a door: find a number or a letter
     * (the core learning goals), taking turns, to open it.
     */
    private fun chest(room: Room): List<Step> {
        if (room.kind == RoomKind.VAULT) return emptyList()
        val s = scene(placeOf(room.kind))
        val numbers = chests++ % 2 == 0
        val c = if (numbers) {
            ChallengeFactory.numeral(level(Skill.NUMBERS), nextSeed(), lines.chestLock("number"))
        } else {
            ChallengeFactory.letter(level(Skill.LETTERS), nextSeed(), lines.chestLock("letter"))
        }
        return listOf(
            tell(s, lines.chestAppears()),
            ask(s, c, lines.chestOops(), lines.chestYay(), Prop.CHEST) {
                val n = random.nextInt(1, 4)
                coins += n
                listOf(found(s.copy(mood = Mood.HAPPY), Loot(LootKind.COINS, n, lines.coinWords(n)), lines.chestLoot(n)))
            },
        )
    }

    private fun room(room: Room): List<Step> = when (room.kind) {
        RoomKind.GATE -> gate()
        RoomKind.RUNE_DOOR -> {
            val s = scene(Place.RUNE_HALL)
            listOf(
                tell(s, lines.runeDoor()),
                ask(s, ChallengeFactory.pattern(level(Skill.PATTERNS), nextSeed()), lines.runeOops(), lines.patternAgain()) {
                    listOf(ask(s, ChallengeFactory.pattern(level(Skill.PATTERNS), nextSeed()), lines.runeOops(), lines.runeYay()) { treasure(s) })
                },
            )
        }
        RoomKind.BRIDGE -> {
            val s = scene(Place.BRIDGE)
            val c = ChallengeFactory.count(level(Skill.COUNTING), nextSeed(), Thing.STONE, "How many stones are on the bridge?")
            // Count the stones, then find the number that says how many: quantity, then symbol.
            listOf(
                tell(s, lines.bridge()),
                ask(s, c, lines.bridgeOops(), "Yes, ${Words.number(c.count)}!") {
                    val n = ChallengeFactory.numeral(level(Skill.NUMBERS), nextSeed(), lines.countThenFind(), c.count)
                    listOf(ask(s, n, lines.bridgeOops(), lines.bridgeYay()) { treasure(s) })
                },
            )
        }
        RoomKind.CRYSTAL_CAVE -> {
            val s = scene(Place.CRYSTAL_CAVE, Actor.WIZARD)
            val c = ChallengeFactory.color(level(Skill.COLORS), nextSeed(), "", speaker = Who.WIZARD)
            listOf(
                tell(s, lines.crystalCave()),
                ask(s, c, lines.crystalOops(), lines.crystalYay()) {
                    gems += c.target.hue
                    val again = ChallengeFactory.color(level(Skill.COLORS), nextSeed(), lines.colorAgain(), speaker = Who.WIZARD)
                    listOf(
                        found(s.copy(mood = Mood.HAPPY), Loot(LootKind.GEM, 1, "a ${c.target.hue.word} gem", c.target.hue), "You got a shiny ${c.target.hue.word} gem!"),
                        ask(s, again, lines.crystalOops(), lines.crystalYay()) { treasure(s) },
                    )
                },
            )
        }
        RoomKind.LIBRARY -> {
            val s = scene(Place.LIBRARY)
            val c = ChallengeFactory.letter(level(Skill.LETTERS), nextSeed(), lines.letterPurpose())
            listOf(
                tell(s, lines.library()),
                ask(s, c, lines.libraryOops(), "Yes! That's the letter ${c.letter}.") {
                    val again = ChallengeFactory.letter(level(Skill.LETTERS), nextSeed(), lines.letterAgain())
                    listOf(ask(s, again, lines.libraryOops(), lines.libraryYay()) { treasure(s) })
                },
            )
        }
        RoomKind.TUNNEL -> {
            val s = scene(Place.TUNNEL)
            val c = ChallengeFactory.write(level(Skill.TRACING), nextSeed(), lines.writePurpose())
            listOf(
                tell(s, lines.tunnel()),
                ask(s, c, lines.tunnelOops(), lines.tunnelYay()) {
                    val n = ChallengeFactory.write(level(Skill.TRACING), nextSeed(), lines.writeAgain(), number = true)
                    listOf(ask(s, n, lines.tunnelOops(), lines.tunnelYay()) { treasure(s) })
                },
            )
        }
        RoomKind.MIRROR_HALL -> {
            val s = scene(Place.MIRROR_HALL)
            val c = ChallengeFactory.memory(level(Skill.MEMORY), nextSeed())
            listOf(tell(s, lines.mirrorHall()), ask(s, c, lines.mirrorOops(), lines.mirrorYay()) { treasure(s) })
        }
        RoomKind.STOREROOM -> {
            val s = scene(Place.STOREROOM)
            val c = ChallengeFactory.sort(level(Skill.SORTING), nextSeed())
            listOf(tell(s, lines.storeroom()), ask(s, c, lines.storeroomOops(), lines.storeroomYay()) { treasure(s) })
        }
        RoomKind.POND -> {
            val s = scene(Place.POND)
            val c = ChallengeFactory.skipCount(level(Skill.SKIP_COUNTING), nextSeed())
            listOf(
                tell(s, lines.pond()),
                ask(s, c, lines.pondOops(), lines.pondYay()) {
                    listOf(
                        tell(s, lines.pondAgain()),
                        ask(s, ChallengeFactory.skipCount(level(Skill.SKIP_COUNTING), nextSeed()), lines.pondOops(), lines.pondYay()) { treasure(s) },
                    )
                },
            )
        }
        RoomKind.MOSAIC_HALL -> {
            val s = scene(Place.MOSAIC_HALL)
            val c = ChallengeFactory.puzzle(level(Skill.PUZZLES), nextSeed())
            listOf(tell(s, lines.mosaic()), ask(s, c, lines.mosaicOops(), lines.mosaicYay()) { treasure(s) })
        }
        RoomKind.VAULT -> vault()
        RoomKind.GOBLIN_DEN -> den()
        RoomKind.WORKSHOP -> workshop()
        RoomKind.LAIR -> lair()
    }

    /** The bonus treasure behind the door the clue pointed to. */
    private fun treasure(s: Scene): List<Step> {
        if (!treasureDoorTaken) return emptyList()
        treasureDoorTaken = false
        val n = random.nextInt(2, 5)
        coins += n
        return listOf(found(s.copy(mood = Mood.HAPPY), Loot(LootKind.COINS, n, lines.coinWords(n)), "The clue was right! A hidden treasure: ${lines.coinWords(n)}!"))
    }

    private fun gate(): List<Step> {
        val s = scene(Place.GATE)
        return listOf(
            tell(s, lines.gate(quest)),
            roll(s, lines.gateRoll()) { tier, _ ->
                val bonus = when (tier) {
                    Tier.GREAT -> 1
                    Tier.MAGIC -> 3
                    else -> 0
                }
                coins += bonus
                val after = s.copy(cleared = true, mood = if (tier == Tier.SILLY) Mood.SILLY else Mood.HAPPY)
                listOf(tell(after, lines.gateResult(tier, bonus))) +
                    if (bonus > 0) listOf(found(after, Loot(LootKind.COINS, bonus, lines.coinWords(bonus)), "Into your bag they go!")) else emptyList()
            },
        )
    }

    private fun vault(): List<Step> {
        val s = scene(Place.VAULT)
        return listOf(
            tell(s, lines.vault()),
            roll(s, lines.vaultRoll()) { tier, _ ->
                val opened = s.copy(cleared = true, mood = if (tier == Tier.SILLY) Mood.SILLY else Mood.HAPPY)
                val c = ChallengeFactory.add(level(Skill.ADDITION), nextSeed(), Thing.COIN) { have, more, missing ->
                    if (missing) {
                        "The magic purse holds ${Words.number(have + more)} coins. You have ${Words.number(have)}. How many more do you need to fill it?"
                    } else {
                        "${if (have == 1) "There is one coin" else "There are ${Words.number(have)} coins"} in the chest, and ${Words.number(more)} more on the floor. How many coins is that altogether?"
                    }
                }
                listOf(
                    tell(opened, lines.vaultResult(tier)),
                    ask(opened, c, lines.vaultOops(), lines.vaultYay()) {
                        coins += c.total
                        treasure(opened)
                    },
                )
            },
        )
    }

    private fun den(): List<Step> {
        val s = scene(Place.GOBLIN_DEN, Actor.GOBLIN)
        val goblin = quest.goblin
        if (goblin in world.friends) {
            goblinFriend = true
            return listOf(tell(s.copy(mood = Mood.HAPPY), lines.denFriend(goblin)))
        }
        val options = listOf(
            Choice(ChoicePicture.SHARE_SNACK, "Share your snack"),
            Choice(ChoicePicture.SING_SONG, "Sing a happy song"),
            Choice(ChoicePicture.TIPTOE, "Tiptoe past quietly"),
        )
        val magnet = hero.heroClass.power == Power.FRIEND_MAGNET
        return listOf(
            tell(s, lines.den(goblin)),
            Step(Beat.Choose(s, Speech.of(lines.denAsk(goblin)), options)) { reply ->
                val happy = s.copy(mood = Mood.HAPPY)
                when (options.getOrNull((reply as? Reply.Picked)?.index ?: 0)?.picture) {
                    ChoicePicture.SING_SONG -> {
                        befriend(goblin)
                        listOf(roll(s, "Sing your song! Roll the dice to see how it sounds.") { tier, _ ->
                            listOf(tell(happy, lines.songResult(tier, goblin), "<goblin>I'll come and help you later!"))
                        })
                    }
                    ChoicePicture.TIPTOE -> {
                        gain(Attribute.COURAGE, 10)
                        magicKey = true
                        if (magnet) befriend(goblin)
                        listOf(
                            tell(s, "Tiptoe, tiptoe... ${if (magnet) "$goblin sees you and smiles. Friends!" else "$goblin doesn't even notice you."} Behind the barrel, you find a secret key!"),
                            found(happy, Loot(LootKind.MAGIC_KEY, 1, "a magic key"), "A magic key! It might open something later."),
                        )
                    }
                    else -> {
                        befriend(goblin)
                        listOf(tell(happy, "$goblin nibbles your cookie and smiles. <goblin>Yum! You're so kind. I'll come and help you later!"))
                    }
                }
            },
        )
    }

    private fun befriend(goblin: String) {
        goblinFriend = true
        gain(Attribute.KINDNESS, 10)
        world = world.copy(friends = world.friends + goblin)
    }

    private fun workshop(): List<Step> {
        val s = scene(Place.WORKSHOP)
        val c = ChallengeFactory.recipe(level(Skill.RECIPES), nextSeed(), quest.potion)
        return listOf(
            tell(s, lines.workshop(quest.potion)),
            ask(s, c, lines.potionOops(), lines.potionYay(quest.potion)) {
                potionMade = true
                listOf(found(s.copy(mood = Mood.HAPPY, cleared = true), Loot(LootKind.POTION, 1, quest.potion.title), "Into your bag goes the ${quest.potion.title}!"))
            },
        )
    }

    // ------------------------------------------------------------- the boss

    private fun lair(): List<Step> {
        val bossActor = if (quest.boss == Boss.SHADOW) Actor.SHADOW else Actor.DRAGON
        val s = { mood: Mood -> scene(Place.LAIR, bossActor, mood = mood) }
        val options = if (quest.boss == Boss.SHADOW) {
            listOf(Choice(ChoicePicture.LIGHT_SPELL, "Cast a light spell"), Choice(ChoicePicture.LULLABY, "Sing a lullaby"))
        } else {
            listOf(Choice(ChoicePicture.MAKE_FRIENDS, "Make friends with ${quest.dragon}"), Choice(ChoicePicture.CAST_SPELL, "Cast a sparkle spell"))
        }
        return listOf(
            tell(s(Mood.SURPRISED), lines.lairWay(quest.potion)),
            tell(s(Mood.CALM), lines.bossReveal(quest)),
            Step(Beat.Choose(s(Mood.CALM), Speech.of(lines.bossAsk(quest)), options)) { reply ->
                val picture = options.getOrNull((reply as? Reply.Picked)?.index ?: 0)?.picture
                val friendly = picture == ChoicePicture.MAKE_FRIENDS || picture == ChoicePicture.LULLABY
                if (friendly) gain(Attribute.KINDNESS, 10) else gain(Attribute.MAGIC, 10)
                bossStars(friendly)
            },
        )
    }

    /**
     * Three stars to light. Friends and items from earlier light some; the rest are short
     * challenges and dice rolls, chosen to fit the way the child decided to help.
     */
    private fun bossStars(friendly: Boolean): List<Step> {
        val bossActor = if (quest.boss == Boss.SHADOW) Actor.SHADOW else Actor.DRAGON
        fun s(mood: Mood = Mood.CALM) = scene(Place.LAIR, bossActor, mood = mood)
        val steps = mutableListOf<Step>()
        fun lit(text: String) {
            bossStars++
            steps += tell(s(Mood.HAPPY), text)
        }
        if (goblinFriend) lit(lines.goblinHelps(quest.goblin))
        if (magicKey && bossStars < 3) lit(lines.keyHelps())
        if (friendly && potionMade && quest.potion == PotionKind.FRIENDSHIP && bossStars < 3) lit(lines.friendshipHelps())

        // The rest come from challenges, made when they're reached so the stars light in order.
        val plan = if (friendly) listOf("gift", "joke", "pattern") else listOf("spell", "letters", "pattern")
        val remaining = plan.take(3 - bossStars.coerceAtMost(3))
        fun starStep(kind: String): List<Step> {
            fun lightOne(): List<Step> {
                bossStars++
                return listOf(tell(s(Mood.HAPPY), lines.starLit(3 - bossStars)))
            }
            return when (kind) {
                "gift" -> listOf(
                    ask(
                        s(), ChallengeFactory.count(level(Skill.COUNTING), nextSeed(), Thing.COIN, lines.giftAsk(quest) + " How many coins are in the gift?"),
                        "${quest.dragon} counts them too and gets mixed up! Let's count again.", "${quest.dragon} hugs the gift. <dragon>Thank you!",
                    ) { lightOne() },
                )
                "joke" -> listOf(roll(s(), lines.jokeRoll()) { tier, _ -> listOf(tell(s(Mood.SILLY), lines.jokeResult(tier, quest.dragon))) + lightOne() })
                "spell" -> listOf(roll(s(), lines.spellRoll()) { tier, _ -> listOf(tell(s(Mood.SURPRISED), lines.spellResult(tier))) + lightOne() })
                "letters" -> listOf(
                    ask(s(), ChallengeFactory.letter(level(Skill.LETTERS), nextSeed(), "The spell needs one more magic letter."), lines.libraryOops(), "The letter shines!") { lightOne() },
                )
                else -> listOf(
                    ask(s(), ChallengeFactory.pattern(level(Skill.PATTERNS), nextSeed()), lines.runeOops(), "The magic symbols spin and glow!") { lightOne() },
                )
            }
        }
        // Chain them: each star step leads into the next.
        fun chain(i: Int): List<Step> {
            if (i >= remaining.size) return ending(friendly)
            val stepsHere = starStep(remaining[i])
            val last = stepsHere.last()
            return stepsHere.dropLast(1) + Step(last.beat) { reply -> last.then(reply) + chain(i + 1) }
        }
        return steps + chain(0)
    }

    private fun ending(friendly: Boolean): List<Step> {
        gain(Attribute.COURAGE, 20)
        val bossActor = if (quest.boss == Boss.SHADOW) Actor.SHADOW else Actor.DRAGON
        val s = scene(Place.LAIR, bossActor, mood = Mood.HAPPY, cleared = true)
        val endingId = "${quest.twist.name.lowercase()}_${if (friendly) "friends" else "spell"}"
        world = world.copy(
            endings = world.endings + endingId,
            dragonFriend = if (friendly && quest.boss == Boss.DRAGON) quest.dragon else world.dragonFriend,
            treasures = world.treasures + quest.treasure,
        )
        lastEnding = endingId
        return listOf(
            tell(s, lines.ending(quest, friendly)),
            found(s, Loot(LootKind.TREASURE, 1, quest.treasure), lines.treasureFound(quest.treasure)),
        )
    }

    private var lastEnding = ""

    private companion object {
        const val MAX_HEARTS = 3
    }

    private fun finale(): List<Step> {
        world = world.copy(
            adventures = world.adventures + 1,
            questsDone = world.questsDone + (quest.kind to (world.questsDone[quest.kind] ?: 0) + 1),
            lastQuest = quest.kind,
        )
        gain(Attribute.COURAGE, hearts * 5)
        val earned = stars.values.sum()
        val levelAfter = hero.level
        val unlocked = Progression.unlocksBetween(levelBefore, levelAfter)
        val said = buildList {
            add(lines.heartsKept(hearts))
            add(lines.finale(earned))
            if (levelAfter > levelBefore) add(lines.levelUp(levelAfter))
            unlocked.forEach { add(it.announcement) }
        }
        val summary = Summary(said.flatMap { Speech.of(it) }, stars.toMap(), levelBefore, levelAfter, unlocked, quest.treasure, lastEnding)
        return listOf(Step(Beat.Finale(Scene(Place.CAMP, cast, Mood.HAPPY), summary)))
    }
}
