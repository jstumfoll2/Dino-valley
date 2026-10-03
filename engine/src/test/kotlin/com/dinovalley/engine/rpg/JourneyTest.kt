package com.dinovalley.engine.rpg

import com.dinovalley.engine.rpg.content.Content
import com.dinovalley.engine.rpg.hero.Attribute
import com.dinovalley.engine.rpg.hero.Hero
import com.dinovalley.engine.rpg.hero.HeroClass
import com.dinovalley.engine.rpg.items.Obstacle
import com.dinovalley.engine.rpg.learn.PickOne
import com.dinovalley.engine.rpg.learn.SkillBook
import com.dinovalley.engine.rpg.run.Beat
import com.dinovalley.engine.rpg.run.Journey
import com.dinovalley.engine.rpg.run.Place
import com.dinovalley.engine.rpg.run.Reply
import com.dinovalley.engine.rpg.run.Scene
import com.dinovalley.engine.rpg.run.battle
import com.dinovalley.engine.rpg.run.nearestSafe
import com.dinovalley.engine.rpg.run.obstacle
import com.dinovalley.engine.rpg.run.testShop
import com.dinovalley.engine.rpg.story.Cond
import com.dinovalley.engine.rpg.story.Effect
import com.dinovalley.engine.rpg.story.Icons
import com.dinovalley.engine.rpg.world.WorldMemory
import com.dinovalley.engine.util.Clock
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** Plays whole journeys with different kinds of players, and checks the people, places and rules. */
class JourneyTest {
    /** How a pretend player behaves. */
    private class Player(val rightRate: Int, val goal: Int, val spend: Boolean, val peace: Boolean?) {
        val r = Random(5)
    }

    private fun journey(seed: Long, hero: Hero = Hero(HeroClass.KNIGHT), world: WorldMemory = WorldMemory(), skills: SkillBook = SkillBook()) =
        Journey(seed, hero, skills, world, Clock { 0L })

    private fun answerFor(b: Beat.Ask, p: Player): Reply.Solved {
        val c = b.challenge
        val right = p.r.nextInt(100) < p.rightRate
        if (right || !b.oneTry) return Reply.Solved(1, 0, 1000)
        val wrong = if (c is PickOne) listOf((c.answer + 1) % c.optionCount) else emptyList()
        return Reply.Solved(2, 0, 1000, failed = true, wrong = wrong)
    }

    private fun play(j: Journey, p: Player, limit: Int = 4000): List<Beat> {
        val seen = mutableListOf<Beat>()
        var n = 0
        while (!j.finished) {
            val b = j.beat
            seen += b
            assertTrue(++n < limit, "journey never ended; last beats: ${seen.takeLast(6).map { it::class.simpleName }}")
            assertTrue(j.hp in 0..j.hero.maxHp, "health out of range: ${j.hp}")
            assertTrue(j.hero.coins >= 0)
            j.reply(
                when (b) {
                    is Beat.Travel -> {
                        val open = b.routes.withIndex().filter { !it.value.blocked }.ifEmpty { b.routes.withIndex().toList() }
                        val marked = open.firstOrNull { it.value.marked }
                        Reply.Picked(if (marked != null && p.r.nextInt(100) < p.goal) marked.index else open.random(p.r).index)
                    }
                    is Beat.Choose -> {
                        // Fight the boss or make peace as the player prefers; otherwise any answer.
                        val peaceIdx = b.options.indexOfFirst { it.icon == "hub_peace" }
                        if (peaceIdx >= 0 && p.peace != null) Reply.Picked(if (p.peace) peaceIdx else b.options.indexOfFirst { it.icon == "hub_fight" })
                        else Reply.Picked(p.r.nextInt(b.options.size))
                    }
                    is Beat.Ask -> answerFor(b, p)
                    is Beat.Shop -> {
                        val buyable = b.stock.filter { it.canAfford }
                        if (p.spend && buyable.isNotEmpty() && p.r.nextInt(100) < 60) Reply.Bought(buyable.random(p.r).itemId) else Reply.Next
                    }
                    else -> Reply.Next
                },
            )
        }
        seen += j.beat
        return seen
    }

    // ------------------------------------------------------------- the content hangs together

    @Test
    fun `every person, shop, road and story points at things that exist`() {
        val npcIds = Content.npcs.map { it.id }.toSet()
        assertEquals(Content.npcs.size, npcIds.size, "duplicate people")
        for (l in Content.locations) for (r in l.residents) assertTrue(r in npcIds, "${l.id} lists unknown person $r")
        for (s in Content.shops) {
            assertTrue(s.keeper in npcIds, s.id)
            for (i in s.stock) assertNotNull(Content.item(i), "${s.id} sells unknown $i")
        }
        for (l in Content.locations) for (s in l.shops) assertNotNull(Content.shop(s), "${l.id} has unknown shop $s")
        val roadIds = Content.roads.map { it.id }.toSet()
        for ((flag, roads) in Content.flagRoads) for (r in roads) assertTrue(r in roadIds, "$flag opens unknown road $r")
        for (a in Content.arcs) {
            assertNotNull(Content.kingdom.locationOrNull(a.lairId)); assertNotNull(Content.kingdom.locationOrNull(a.keyDungeonId))
            assertNotNull(Content.item(a.keyItemId), a.keyItemId); assertNotNull(Content.monster(a.bossId), a.bossId)
            assertTrue(a.variants.isNotEmpty() && a.peaceSteps.isNotEmpty())
            for (m in a.moments) assertNotNull(Content.kingdom.locationOrNull(m.at), "${a.id}: moment at unknown ${m.at}")
            assertTrue(a.variants.size >= 3, "${a.id}: a story can be told more than one way")
        }
        for (npc in Content.npcs) {
            val ids = npc.nodes.map { it.id }
            assertEquals(ids.size, ids.toSet().size, "${npc.id}: duplicate nodes")
            assertTrue(npc.starts.isNotEmpty() && npc.starts.last().needs.isEmpty(), "${npc.id}: needs a start that always works")
            for (st in npc.starts) assertTrue(st.node in ids, "${npc.id}: start ${st.node}")
            for (n in npc.nodes) {
                assertTrue(n.options.size <= 4, "${npc.id}/${n.id}: too many replies")
                assertTrue(n.says.isNotBlank())
                val effects = n.effects + n.options.flatMap { it.effects }
                for (o in n.options) {
                    assertTrue(o.icon in Icons.all, "${npc.id}/${n.id}: unknown icon ${o.icon}")
                    assertTrue(o.next == null || o.next in ids, "${npc.id}/${n.id}: goes to missing ${o.next}")
                }
                for (e in effects) when (e) {
                    is Effect.Give -> assertNotNull(Content.item(e.itemId), "${npc.id}: gives unknown ${e.itemId}")
                    is Effect.Take -> assertNotNull(Content.item(e.itemId))
                    is Effect.Fight -> { assertNotNull(Content.monster(e.monsterId), e.monsterId); assertTrue(e.win == null || e.win in ids) }
                    is Effect.Puzzle -> { Obstacle.valueOf(e.kind); assertTrue((e.win == null || e.win in ids) && (e.lose == null || e.lose in ids)) }
                    is Effect.Shop -> assertNotNull(Content.shop(e.shopId))
                    is Effect.OpenRoad -> assertTrue(e.roadId in roadIds, e.roadId)
                    else -> Unit
                }
                for (c in n.options.flatMap { it.needs }) if (c is Cond.HasItem) assertNotNull(Content.item(c.itemId))
            }
            // Every node can be reached from a start.
            val reach = mutableSetOf<String>()
            fun walk(id: String) {
                if (!reach.add(id)) return
                val n = npc.node(id)
                n.options.forEach { o -> o.next?.let(::walk); o.effects.forEach { walkEffect(it, ::walk) } }
                n.effects.forEach { walkEffect(it, ::walk) }
            }
            npc.starts.forEach { walk(it.node) }
            assertEquals(emptyList(), ids - reach, "${npc.id}: nodes nobody can reach")
        }
        // Every line that can be spoken parses (no unknown speaker tags).
        val texts = Content.npcs.flatMap { n -> listOf(n.intro) + n.nodes.map { it.says } } +
            Content.monsters.flatMap { listOf(it.taunt, it.beaten, it.wins) } +
            Content.arcs.flatMap { a -> a.setup + a.sealed + a.keyFound + a.gateOpens + a.ask + a.peaceSteps.flatMap { listOf(it.intro, it.yay) } + a.variants.flatMap { listOf(it.meeting, it.fightEnd, it.peaceEnd) } + a.moments.map { it.says } }
        for (t in texts) com.dinovalley.engine.model.Speech.of(t)
        // Every gift and quest item is somewhere a player can get it.
        val given = Content.npcs.flatMap { n -> n.nodes.flatMap { it.effects + it.options.flatMap { o -> o.effects } } }
            .filterIsInstance<Effect.Give>().map { it.itemId }.toSet()
        val sold = Content.shops.flatMap { it.stock }.toSet()
        val dropped = Content.monsters.flatMap { it.drops }.map { it.itemId }.toSet()
        for (a in Content.arcs) assertTrue(a.keyItemId in given + sold + dropped, "${a.keyItemId} can never be found")
        assertTrue("ink_cleaner" in given && "recipe_page" in given && "magic_beans" in given)
    }

    private fun walkEffect(e: Effect, walk: (String) -> Unit) {
        when (e) {
            is Effect.Fight -> e.win?.let(walk)
            is Effect.Puzzle -> { e.win?.let(walk); e.lose?.let(walk) }
            else -> Unit
        }
    }

    // ------------------------------------------------------------- whole journeys

    @Test
    fun `players of every kind reach the end`() {
        val players = listOf(
            Player(100, 90, spend = false, peace = true),
            Player(100, 90, spend = true, peace = false),
            Player(60, 80, spend = true, peace = null),
            Player(40, 70, spend = true, peace = null),
            Player(30, 60, spend = false, peace = false),
            Player(70, 20, spend = true, peace = null),
        )
        for (seed in 1L..24L) for ((i, p) in players.withIndex()) {
            val j = journey(seed)
            val beats = try {
                play(j, p)
            } catch (e: AssertionError) {
                throw AssertionError("seed $seed player $i at ${j.here} hp ${j.hp} faints ${j.faints} slips ${j.slips} key ${j.hero.has(j.arc.keyItemId)} arc ${j.arc.id}: ${e.message}")
            }
            assertTrue(j.finished, "seed $seed player $i")
            assertEquals(1, j.world.pages, "a page comes home")
            assertEquals(1, j.world.adventures)
            assertTrue(j.records.size >= 3, "some learning happened: ${j.records.size}")
            assertTrue(j.hero.has(j.arc.keyItemId), "the key was found before the lair")
            assertTrue(beats.any { it is Beat.Travel } && beats.any { it is Beat.Ask })
            assertTrue(j.visited.contains(j.arc.lairId) && j.visited.contains(j.arc.keyDungeonId))
        }
    }

    @Test
    fun `the same seed tells the same journey`() {
        fun shape(seed: Long) = play(journey(seed), Player(80, 80, true, null)).map { it::class.simpleName + ":" + it.scene.place.id }
        assertEquals(shape(7), shape(7))
    }

    @Test
    fun `friends keep their roads open in later adventures, and the world remembers`() {
        val world = WorldMemory(flags = setOf("grumble_befriended"), pages = 1, adventures = 1)
        val j = journey(3, world = world)
        assertTrue("pennywhistle~whispering_falls" in j.opened, "Finn's ferry stays open")
        val done = play(j, Player(100, 90, true, true))
        assertTrue(done.isNotEmpty())
        assertEquals(2, j.world.pages)
        assertTrue("friend:${j.arc.bossId}" in j.world.flags, "peace makes a friend")
    }

    @Test
    fun `after the fifth page the book is whole`() {
        val j = journey(11, world = WorldMemory(pages = 4, adventures = 4))
        val beats = play(j, Player(100, 90, false, false))
        assertEquals(5, j.world.pages)
        val said = beats.filterIsInstance<Beat.Tell>().joinToString(" ") { com.dinovalley.engine.model.Voice.caption(it.lines) }
        assertTrue("Great Storybook is whole" in said, "the finished book is celebrated")
    }

    // ------------------------------------------------------------- the rules

    @Test
    fun `one try, then help from the bag, never a free second go`() {
        // With nothing in the bag, a wrong answer fails and the hero is turned back.
        val bare = journey(1)
        val scene = Scene(Place.CAMP, bare.cast)
        var turned = 0
        bare.load(bare.obstacle(Obstacle.CLIMB, scene, next = { emptyList() }, turnBack = { turned++; emptyList() }))
        assertTrue(bare.beat is Beat.Tell)
        bare.reply(Reply.Next)
        val ask = bare.beat as Beat.Ask
        assertTrue(ask.oneTry && ask.tried.isEmpty())
        val wrong = (ask.challenge as PickOne).let { listOf((it.answer + 1) % it.optionCount) }
        bare.reply(Reply.Solved(2, 0, 0, failed = true, wrong = wrong))
        assertEquals(1, turned.coerceAtLeast(0).let { if (bare.beat is Beat.Tell) { bare.reply(Reply.Next); turned } else turned }, "turned back once")

        // With a Wise Owl Feather: one more guess, with a wrong answer taken away, and only once.
        val sharp = SkillBook(levels = mapOf(com.dinovalley.engine.rpg.learn.Skill.COUNTING to 4))
        val charmed = journey(1, Hero().give("owl_feather"), skills = sharp)
        var through = 0
        var back = 0
        charmed.load(charmed.obstacle(Obstacle.CLIMB, scene, next = { through++; emptyList() }, turnBack = { back++; emptyList() }))
        charmed.reply(Reply.Next)
        val first = charmed.beat as Beat.Ask
        val firstWrong = (first.challenge as PickOne).let { listOf((it.answer + 1) % it.optionCount) }
        charmed.reply(Reply.Solved(2, 0, 0, failed = true, wrong = firstWrong))
        val offer = charmed.beat as Beat.Choose
        assertTrue(offer.options.any { it.icon == "item_owl_feather" } && offer.options.last().icon == "hub_leave")
        charmed.reply(Reply.Picked(0))
        assertTrue(!charmed.hero.has("owl_feather"), "the feather is used up")
        charmed.reply(Reply.Next) // "the feather glows"
        val second = charmed.beat as Beat.Ask
        assertTrue(second.tried.size > firstWrong.size, "the feather took a wrong answer away")
        assertTrue(second.challenge === first.challenge && (second.challenge as PickOne).answer !in second.tried)
        assertTrue((second.challenge as PickOne).optionCount - second.tried.size >= 2, "still a real choice")
        charmed.reply(Reply.Solved(2, 0, 0, failed = true, wrong = second.tried))
        // Failing again, with no more charms, turns back.
        assertEquals(0, through)
    }

    @Test
    fun `a tool gets past the obstacle it is made for`() {
        val j = journey(2, Hero().give("rope"))
        val scene = Scene(Place.CAMP, j.cast)
        var through = 0
        j.load(j.obstacle(Obstacle.CLIMB, scene, next = { through++; emptyList() }, turnBack = { emptyList() }))
        j.reply(Reply.Next)
        j.reply(Reply.Solved(2, 0, 0, failed = true, wrong = listOf(0)))
        val offer = j.beat as Beat.Choose
        assertTrue(offer.options.first().icon == "item_rope")
        j.reply(Reply.Picked(0))
        assertTrue(!j.hero.has("rope"))
        assertTrue(through == 0 || through == 1) // the tell comes first
        while (!j.finished && j.beat is Beat.Tell) j.reply(Reply.Next)
    }

    @Test
    fun `fights take more rounds for stronger monsters, and fainting costs a quarter of the coins`() {
        fun rounds(id: String): Int {
            val j = journey(4, Hero(xp = mapOf(Attribute.COURAGE to 0)).earn(40))
            val m = Content.monster(id)!!
            j.load(j.battle(m, Place.CAMP, onWin = { emptyList() }))
            var asks = 0
            var guard = 0
            while (guard++ < 400 && !(j.beat is Beat.Travel)) {
                val b = j.beat
                if (b is Beat.Ask) asks++
                if (b is Beat.Choose && b.options.firstOrNull()?.icon == "hub_attack") { j.reply(Reply.Picked(0)); continue }
                j.reply(if (b is Beat.Ask) Reply.Solved(1, 0, 0) else Reply.Next)
                if (j.beat is Beat.Ask || j.queue.isNotEmpty()) continue
                break
            }
            return asks
        }
        val minion = rounds("slime")
        val elite = rounds("cave_troll")
        val boss = rounds("baron_grumblewick")
        assertTrue(minion in 2..5, "a little monster falls in a few hits: $minion")
        assertTrue(elite > minion && boss > elite, "tougher means more rounds: $minion, $elite, $boss")

        val j = journey(4, Hero().earn(40))
        j.load(j.battle(Content.monster("cave_troll")!!, Place.CAMP, onWin = { emptyList() }))
        var guard = 0
        while (guard++ < 300 && j.faints == 0) {
            val b = j.beat
            j.reply(
                when (b) {
                    is Beat.Choose -> Reply.Picked(0)
                    is Beat.Ask -> Reply.Solved(2, 0, 0, failed = true, wrong = listOf(0))
                    else -> Reply.Next
                },
            )
        }
        assertEquals(1, j.faints, "the hero faints")
        assertEquals(30, j.hero.coins, "a quarter of 40 coins is lost")
        assertEquals(j.hero.maxHp / 2, j.hp)
        assertEquals(j.kingdom.camp.id, j.here, "carried back to the camp")
        assertEquals(j.kingdom.camp, j.nearestSafe())
    }

    @Test
    fun `gear and stars make fights easier and what a monster is weak to matters`() {
        fun hitsToWin(hero: Hero, id: String): Int {
            val j = journey(9, hero)
            j.load(j.battle(Content.monster(id)!!, Place.CAMP, onWin = { emptyList() }))
            var asks = 0
            var guard = 0
            while (guard++ < 500 && j.monstersBeaten == 0) {
                val b = j.beat
                if (b is Beat.Ask) asks++
                j.reply(
                    when (b) {
                        // Use the monster's weakness when it is on offer.
                        is Beat.Choose -> Reply.Picked(b.options.indexOfFirst { it.icon == "item_ink_cleaner" }.coerceAtLeast(0))
                        is Beat.Ask -> Reply.Solved(1, 0, 0)
                        else -> Reply.Next
                    },
                )
            }
            return asks
        }
        val bare = hitsToWin(Hero(), "cave_troll")
        val armed = hitsToWin(Hero().give("knight_sword").wear(Content.item("knight_sword")!!), "cave_troll")
        assertTrue(armed < bare, "a sword shortens the fight: $armed < $bare")
        // The Ink Cleaner hurts the Baron most of all.
        val plain = hitsToWin(Hero(), "baron_grumblewick")
        val cleaned = hitsToWin(Hero().give("ink_cleaner"), "baron_grumblewick")
        assertTrue(cleaned < plain, "the Baron's weakness shortens the fight: $cleaned < $plain")
    }

    @Test
    fun `shops sell, gear goes on, and coins are checked`() {
        val j = journey(6, Hero().earn(50))
        j.load(j.testShop("smithy"))
        val shop = j.beat as Beat.Shop
        assertEquals(50, shop.coins)
        val sword = shop.stock.first { it.itemId == "wooden_sword" }
        j.reply(Reply.Bought("wooden_sword"))
        assertEquals(50 - sword.price, j.hero.coins)
        assertEquals("wooden_sword", j.hero.worn[com.dinovalley.engine.rpg.items.Slot.HAND], "gear goes on at once")
        // Not enough coins: nothing is sold.
        val poor = journey(6, Hero().earn(1))
        poor.load(poor.testShop("smithy"))
        while (poor.beat !is Beat.Shop) poor.reply(Reply.Next)
        poor.reply(Reply.Bought("chain_mail"))
        assertEquals(1, poor.hero.coins)
        assertTrue(!poor.hero.has("chain_mail"))
    }
}
