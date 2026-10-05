package com.littledungeon.engine.rpg.run

import com.littledungeon.engine.model.Speech
import com.littledungeon.engine.rpg.content.Content
import com.littledungeon.engine.rpg.items.Obstacle
import com.littledungeon.engine.rpg.learn.Thing
import com.littledungeon.engine.rpg.story.Effect
import com.littledungeon.engine.rpg.story.Npc
import com.littledungeon.engine.rpg.story.ShopDef
import com.littledungeon.engine.rpg.world.Location
import com.littledungeon.engine.rpg.world.LocationKind
import com.littledungeon.engine.rpg.world.RoomKind
import com.littledungeon.engine.rpg.world.Terrain

/** What a night at an inn costs. */
internal const val INN_COST = 3

/** The backdrop at a place: the camp has its own painting, everywhere else is named by its theme. */
internal fun Journey.sceneAt(l: Location, npc: Npc? = null): Scene =
    scene(if (l.kind == LocationKind.CAMP) Place.CAMP else placeOf(l), npc = npc?.let { view(it) })

// ------------------------------------------------------------- towns

internal fun Journey.townHub(l: Location, first: Boolean, woke: Boolean = false): List<JStep> {
    val s = sceneAt(l)
    val steps = mutableListOf<JStep>()
    if (l.kind == LocationKind.CAMP) {
        // Coming back to camp is a free rest, but a hero carried here after fainting only wakes up.
        if (!first && !woke) {
            heal(hero.maxHp)
            steps += tell(s, say.backAtCamp())
        }
    } else if (first) {
        steps += tell(s, say.arriveTown(l.name), l.blurb)
    }
    return steps + hub(l)
}

/** The things to do in a town: talk to the people who live there, rest, or move on. */
internal fun Journey.hub(l: Location): List<JStep> {
    val s = sceneAt(l)
    val people = l.residents.mapNotNull { Content.npc(it) }
    val canRest = l.shops.contains("healer") || l.kind == LocationKind.CAMP
    val choices = people.map { Choice("${it.art}_body", "Talk to ${it.name}") } +
        (if (canRest) listOf(Choice("hub_inn", if (l.kind == LocationKind.CAMP) "Rest by the fire" else "Rest at the inn")) else emptyList()) +
        Choice("hub_leave", "Move on")
    return listOf(
        JStep(Beat.Choose(s, Speech.of(say.hubAsk(l.name)), choices)) { reply ->
            val i = (reply as? Reply.Picked)?.index ?: choices.lastIndex
            when {
                i in people.indices -> talk(people[i]) { hub(l) }
                canRest && i == people.size -> rest(l, if (l.kind == LocationKind.CAMP) 0 else INN_COST) + hub(l)
                else -> emptyList()
            }
        },
    )
}

internal fun Journey.rest(l: Location, cost: Int): List<JStep> {
    val s = sceneAt(l)
    if (hero.coins < cost) return listOf(tell(s, say.cannotRest()))
    hero = hero.earn(-cost)
    heal(hero.maxHp)
    return listOf(tell(s.copy(mood = Mood.HAPPY), say.rest(cost)))
}

// ------------------------------------------------------------- wild places

internal fun Journey.wild(l: Location, first: Boolean): List<JStep> {
    val s = sceneAt(l)
    val steps = mutableListOf<JStep>()
    if (first) steps += tell(s, say.arriveWild(l.name), l.blurb)
    val people = l.residents.mapNotNull { Content.npc(it) }
    if (people.isNotEmpty()) {
        val someone = people.firstOrNull { p -> p.starts.any { holds(it.needs) } }
        return steps + if (someone != null) talk(someone) { tollCheck(l, someone) } else listOf(tell(s, say.quiet(l.name)))
    }
    return steps + if (first) wildEvent(l, s) else listOf(tell(s, say.quiet(l.name)))
}

/**
 * Someone who holds the way lets the hero by once they have paid, made friends or been beaten. Otherwise the hero is sent
 * back the way they came and that road is closed for a few moves, and can go around, or come back with what is wanted.
 */
internal fun Journey.tollCheck(l: Location, npc: Npc): List<JStep> {
    if (npc.passFlags.isEmpty() || npc.passFlags.any { hasFlag(it) }) return emptyList()
    val back = kingdom.location(cameFrom)
    kingdom.roadBetween(l.id, back.id)?.let { closed[it.id] = moves + 3 }
    here = back.id
    return listOf(tell(sceneAt(l, npc), say.tollBlocked(npc.name)))
}

/** Something happens at a place nobody lives: a chest, a shrine, a fight, a traveler or a hidden room. */
internal fun Journey.wildEvent(l: Location, s: Scene): List<JStep> = when (random.nextInt(100)) {
    in 0 until 25 -> chestEvent(s)
    in 25 until 40 -> shrineEvent(s)
    in 40 until 65 -> {
        val m = roadMonster(Terrain.FOREST, 1 + random.nextInt(2))
        encounter(m, s, retreat = { listOf(tell(s, say.sneakAway())) }, onWin = { emptyList() })
    }
    in 65 until 80 -> wandererEvent(s)
    else -> hiddenRoom(s)
}

/** Something found along a road that is walked for the first time and has no monster: a chest, a shrine, a traveler, a hidden room. */
internal fun Journey.roadEvent(s: Scene): List<JStep> = when (random.nextInt(100)) {
    in 0 until 20 -> chestEvent(s)
    in 20 until 35 -> shrineEvent(s)
    in 35 until 50 -> wandererEvent(s)
    in 50 until 70 -> luckyRoll(s)
    else -> hiddenRoom(s)
}

internal fun Journey.shrineEvent(s: Scene): List<JStep> {
    heal(hero.maxHp)
    gain(com.littledungeon.engine.rpg.hero.Attribute.COURAGE, 5)
    return listOf(tell(s.copy(mood = Mood.HAPPY), say.shrine()))
}

internal fun Journey.wandererEvent(s: Scene): List<JStep> = listOf(tell(s, say.wanderer())) + coins(s, 2 + random.nextInt(4))

/**
 * A little door hidden by the road, into one of the puzzle rooms a dungeon can hold: the kind of puzzle the
 * child has practiced least. Solving it earns coins; missing it just closes the door.
 */
internal fun Journey.hiddenRoom(s: Scene): List<JStep> {
    val skill = pickSkill(RoomKind.learningRooms.map { it.skill!! })
    val kind = RoomKind.learningRooms.first { it.skill == skill }
    val room = roomScene(kind)
    return listOf(tell(s, say.hiddenDoor())) + roomPuzzle(
        kind, room,
        solved = { coins(room, 3 + random.nextInt(5)) + tell(room.copy(mood = Mood.HAPPY), say.roomCleared()) },
        failed = { listOf(tell(s, say.hiddenDoorShut())) },
    )
}

/** A locked chest: one try at the lock, or a key from the bag. */
internal fun Journey.chestEvent(s: Scene): List<JStep> {
    val loot = { loot(s) }
    return listOf(tell(s, say.chest())) + obstacle(Obstacle.LOCK, s, next = loot, turnBack = { emptyList() })
}

/** What is in a chest: coins, and now and then something useful. */
internal fun Journey.loot(s: Scene): List<JStep> {
    val pool = listOf("berry", "honey_cake", "rope", "lantern", "lucky_clover", "spark_bomb", "bubble_shield", "rusty_key", "friendship_cookie")
    val steps = coins(s, 3 + random.nextInt(6)).toMutableList()
    if (random.nextInt(100) < 55) steps += item(s, pool.random(random))
    return listOf(tell(s, say.chestOpens())) + steps
}

// ------------------------------------------------------------- people

/** Starts a conversation: the first time with an introduction, always at the first start that fits. */
internal fun Journey.talk(npc: Npc, back: () -> List<JStep>): List<JStep> {
    val start = npc.starts.firstOrNull { holds(it.needs) }?.node ?: return back()
    val met = "met:${npc.id}"
    val s = sceneAt(kingdom.location(here), npc)
    val steps = mutableListOf<JStep>()
    if (!hasFlag(met)) {
        setFlag(met)
        steps += tell(s, "<${npc.who.tag}>${npc.intro}")
    }
    return steps + node(npc, start, back)
}

/** One moment of a conversation: what they say, and what the hero can reply. */
internal fun Journey.node(npc: Npc, id: String, back: () -> List<JStep>): List<JStep> {
    val n = npc.node(id)
    val s = sceneAt(kingdom.location(here), npc)
    val said = Speech.of("<${npc.who.tag}>${n.says}")
    val options = n.options.filter { holds(it.needs) }.take(3)
    if (options.isEmpty()) {
        return listOf(JStep(Beat.Tell(s, said)) { runEffects(n.effects, npc, back, back) })
    }
    return listOf(
        JStep(Beat.Choose(s, said, options.map { Choice(it.icon, it.said) })) { reply ->
            val o = options[((reply as? Reply.Picked)?.index ?: 0).coerceIn(options.indices)]
            val onward = { o.next?.let { node(npc, it, back) } ?: back() }
            runEffects(n.effects + o.effects, npc, back, onward)
        },
    )
}

/** Does what a choice or a moment does, one effect after another, then goes on to [then]. */
internal fun Journey.runEffects(list: List<Effect>, npc: Npc?, back: () -> List<JStep>, then: () -> List<JStep>): List<JStep> {
    if (list.isEmpty()) return then()
    val e = list.first()
    val cont = { runEffects(list.drop(1), npc, back, then) }
    val s = sceneAt(kingdom.location(here), npc)
    return when (e) {
        is Effect.Give -> item(s, e.itemId, e.n) + cont()
        is Effect.Take -> {
            hero = hero.take(e.itemId, e.n)
            cont()
        }
        is Effect.Pay -> {
            hero = hero.earn(-e.coins)
            cont()
        }
        is Effect.Earn -> {
            hero = hero.earn(e.coins)
            listOf(found(s.copy(mood = Mood.HAPPY), Loot(LootKind.COINS, e.coins, say.coinsFound(e.coins)), say.coinsFound(e.coins))) + cont()
        }
        is Effect.SetFlag -> {
            setFlag(e.name)
            cont()
        }
        is Effect.ClearFlag -> {
            clearFlag(e.name)
            cont()
        }
        is Effect.Relation -> {
            befriend(e.npcId, e.delta)
            cont()
        }
        is Effect.Heal -> {
            heal(e.hp)
            cont()
        }
        is Effect.Stars -> {
            gain(e.attribute, e.n)
            cont()
        }
        is Effect.OpenRoad -> {
            opened += e.roadId
            closed -= e.roadId
            cont()
        }
        is Effect.CloseRoad -> {
            closed[e.roadId] = moves + 99
            cont()
        }
        is Effect.Fight -> {
            val m = Content.monster(e.monsterId) ?: return cont()
            battle(m, s.place, onWin = { e.win?.let { node(npc!!, it, back) } ?: cont() })
        }
        is Effect.Puzzle -> {
            val o = Obstacle.valueOf(e.kind)
            val own = e.skill?.let { Costume(it, e.ask.orEmpty(), e.thing ?: Thing.STONE) }
            obstacle(
                o, s,
                next = { e.win?.let { node(npc!!, it, back) } ?: cont() },
                turnBack = { e.lose?.let { node(npc!!, it, back) } ?: cont() },
                own = own,
            )
        }
        is Effect.Shop -> {
            val def = Content.shop(e.shopId) ?: return cont()
            shop(def, npc) { cont() }
        }
        is Effect.Rest -> rest(kingdom.location(here), e.cost) + cont()
        Effect.Page -> {
            world = world.copy(pages = world.pages + 1)
            item(s, "storybook_page") + cont()
        }
    }
}

// ------------------------------------------------------------- shops

/** What an item costs here: kindness already lowers it, and a shopkeeper who likes the hero lowers it more (5% for each point, up to 20%). */
internal fun Journey.priceAt(npc: Npc?, item: com.littledungeon.engine.rpg.items.Item): Int {
    val friendly = npc?.let { (relation(it.id) * 5).coerceIn(0, 20) } ?: 0
    return (hero.priceOf(item) * (100 - friendly) / 100).coerceAtLeast(1)
}

internal fun Journey.shop(def: ShopDef, npc: Npc?, done: () -> List<JStep>): List<JStep> {
    val s = sceneAt(kingdom.location(here), npc)
    val stock = def.stock.mapNotNull { Content.item(it) }.map {
        val price = priceAt(npc, it)
        ShopItem(it.id, it.name, price, hero.count(it.id) + if (hero.worn.values.contains(it.id)) 1 else 0, hero.coins >= price)
    }
    val prompt = Speech.of(say.shopWelcome(npc?.name ?: def.name) + " " + say.shopAsk())
    return listOf(
        JStep(Beat.Shop(s, prompt, def.name, hero.coins, stock)) { reply ->
            val bought = (reply as? Reply.Bought)?.itemId?.let { id -> Content.item(id)?.takeIf { it.id in def.stock } }
            if (reply !is Reply.Bought) return@JStep listOf(tell(s, say.shopBye())) + done()
            if (bought == null) return@JStep shop(def, npc, done)
            val price = priceAt(npc, bought)
            if (hero.coins < price) return@JStep listOf(tell(s, say.cannotAfford())) + shop(def, npc, done)
            hero = hero.earn(-price).give(bought.id)
            // New gear goes straight on if that spot was empty, so the hero's look changes at once.
            val wear = bought.slot != null && hero.worn[bought.slot] == null
            if (wear) equip(bought.id)
            listOf(tell(s.copy(mood = Mood.HAPPY), say.bought(bought.name, price), if (wear) say.foundGear(bought.name) else null)) + shop(def, npc, done)
        },
    )
}


/**
 * Lucky dice on the road: roll two, add them up (adding, and seeing a die's dots at a glance), and the coins are the total. A low
 * total (four or less) is silly rather than bad: a gnome says sorry with a coin.
 */
internal fun Journey.luckyRoll(s: Scene): List<JStep> {
    val dice = listOf(random.nextInt(1, 7), random.nextInt(1, 7))
    val total = dice.sum()
    return listOf(
        JStep(Beat.Roll(s, Speech.of(say.luckyRollAsk()), dice, null)) { reply ->
            val r = reply as? Reply.Rolled
            // Adding them up is practice for addition: a first-time answer counts as a first try.
            skills = skills.record(
                com.littledungeon.engine.rpg.learn.ChallengeRecord(
                    com.littledungeon.engine.rpg.learn.Skill.ADDITION, "dice", level(com.littledungeon.engine.rpg.learn.Skill.ADDITION),
                    (r?.sumTries ?: 1).coerceAtLeast(1), 0, 0, now, nextSeed(), false,
                ),
            )
            val silly = total <= 4
            val found = if (silly) 1 else total
            listOf(tell(s.copy(mood = if (silly) Mood.SILLY else Mood.HAPPY), if (silly) say.luckyRollSilly() else say.luckyRollBig())) + coins(s, found)
        },
    )
}
