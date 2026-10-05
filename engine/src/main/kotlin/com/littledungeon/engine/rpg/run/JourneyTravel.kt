package com.littledungeon.engine.rpg.run

import com.littledungeon.engine.model.Speech
import com.littledungeon.engine.rpg.battle.Monster
import com.littledungeon.engine.rpg.battle.Tier
import com.littledungeon.engine.rpg.content.Content
import com.littledungeon.engine.rpg.hero.Power
import com.littledungeon.engine.rpg.world.Location
import com.littledungeon.engine.rpg.world.LocationKind
import com.littledungeon.engine.rpg.world.Road
import com.littledungeon.engine.rpg.world.Terrain

/** The chance, on the first walk of a road with no monster on it, that something is found along the way. */
internal const val ROAD_EVENT_PERCENT = 30

/** The backdrop for a road. */
internal fun roadPlace(t: Terrain) = Place("road_${t.name.lowercase()}")

/** The roads out of where the hero is. Lairs that belong to other stories are not on this map. */
internal fun Journey.roadsHere(): List<Road> = kingdom.roadsFrom(here).filter { r ->
    val to = kingdom.location(r.other(here))
    to.kind != LocationKind.LAIR || to.id == arc.lairId
}

/** Where the story wants the hero next: the dungeon with the key, then the lair. */
internal fun Journey.goal(): Location {
    val key = hero.has(arc.keyItemId)
    return kingdom.location(if (key) arc.lairId else arc.keyDungeonId)
}

/** The road that leads fastest toward [goal], for the baby dragon to point at. */
internal fun Journey.markedRoad(roads: List<Road>): Road? {
    val target = goal().id
    val others = kingdom.lairs.map { it.id }.toSet() - arc.lairId
    return roads.filter { !isClosed(it) }.minByOrNull { r ->
        val next = r.other(here)
        if (next == target) 0 else (kingdom.hops(next, target, avoid = others) ?: 99) + 1
    }
}

internal fun Journey.travel(): List<JStep> {
    val roads = roadsHere()
    val marked = markedRoad(roads)
    val routes = roads.map { r ->
        val to = kingdom.location(r.other(here))
        val terrain = terrainOf(r)
        val blocked = isClosed(r)
        val danger = if (hero.seesDangers) r.danger.takeIf { terrain == r.terrain } ?: 0 else null
        Route(
            to.id, to.name, to.kind, terrain, danger, to.id in visited, r === marked, blocked,
            Speech.of(if (blocked) say.blockedRoad(to.name) else say.routeSaid(to.name, terrain, danger, to.id in visited, r === marked)),
        )
    }
    val prompt = Speech.of(say.travelPrompt(kingdom.location(here).name))
    return listOf(
        JStep(Beat.Travel(Scene(Place.WORLD_MAP, cast), prompt, here, routes)) { reply ->
            val index = ((reply as? Reply.Picked)?.index ?: 0).coerceIn(routes.indices)
            // A closed road can't be taken; the first open one is taken instead.
            val road = roads[index].takeUnless { isClosed(it) } ?: roads.firstOrNull { !isClosed(it) } ?: roads[index]
            go(road)
        },
    )
}

/** Walks a road: what happens on the way, then arriving, unless something turns the hero back. */
internal fun Journey.go(road: Road): List<JStep> {
    moves++
    val from = here
    cameFrom = from
    val dest = kingdom.location(road.other(from))
    val terrain = terrainOf(road)
    val s = scene(roadPlace(terrain))
    val arrive = { arrive(dest) }
    val afterObstacle: () -> List<JStep> = {
        val chance = when {
            !road.terrain.monsters -> 0
            road.danger >= 2 -> 70
            else -> 50
        } / (if (hero.heroClass.power == Power.KEEN_EYES) 2 else 1) // a Ranger spots trouble early
        if (terrain.monsters && road.id !in opened && random.nextInt(100) < chance) {
            encounter(roadMonster(terrain, road.danger), s, retreat = { listOf(tell(scene(placeOf(kingdom.location(from))), say.backAway(kingdom.location(from).name))) }, onWin = arrive)
        } else if (walked.add(road.id) && random.nextInt(100) < ROAD_EVENT_PERCENT) {
            roadEvent(s).andThen(arrive)
        } else {
            arrive()
        }
    }
    val obstacle = terrain.obstacle
    return if (obstacle != null) {
        obstacle(obstacle, s, next = afterObstacle, turnBack = { turnedBack(road, from) })
    } else {
        afterObstacle()
    }
}

/** A road monster fitted to the road: little ones on easy roads, a chance of a tough one on dangerous ones. */
internal fun Journey.roadMonster(terrain: Terrain, danger: Int): Monster {
    val tier = if (danger >= 2 && random.nextInt(100) < 40) Tier.ELITE else Tier.MINION
    // Kinds the hero has made friends with (a cookie for a wolf pup) do not ambush them again.
    fun fair(m: Monster) = m.roams && m.tier == tier && !hasFlag("friend:${m.id}")
    val pool = Content.monsters.filter { fair(it) && (it.habitat.isEmpty() || terrain in it.habitat) }
        .ifEmpty { Content.monsters.filter { fair(it) } }
        .ifEmpty { Content.monsters.filter { it.roams && it.tier == tier } }
    return pool.random(random)
}

internal fun Journey.turnedBack(road: Road, from: String): List<JStep> {
    closed[road.id] = moves + 3
    return listOf(tell(scene(placeOf(kingdom.location(from))), say.turnedBack(kingdom.location(from).name)))
}

/** Arriving somewhere: what the place does depends on what it is. */
internal fun Journey.arrive(l: Location): List<JStep> {
    here = l.id
    val first = visited.add(l.id)
    val story = if (first) moments(l) else emptyList()
    return story + when (l.kind) {
        LocationKind.CAMP, LocationKind.TOWN -> townHub(l, first)
        LocationKind.DUNGEON -> dungeon(l, first)
        LocationKind.WILD -> wild(l, first)
        LocationKind.LAIR -> lairArrival(l)
    }
}

/** What the story shows when the hero first reaches a place. */
internal fun Journey.moments(l: Location): List<JStep> {
    val s = sceneAt(l)
    return arc.moments.filter { it.at == l.id && holds(it.needs) }.flatMap { m ->
        listOf(tell(s, m.says)) + runEffects(m.effects, null, { emptyList() }, { emptyList() })
    }
}
