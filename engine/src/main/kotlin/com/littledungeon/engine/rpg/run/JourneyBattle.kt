package com.littledungeon.engine.rpg.run

import com.littledungeon.engine.model.Speech
import com.littledungeon.engine.rpg.battle.Monster
import com.littledungeon.engine.rpg.battle.Tier
import com.littledungeon.engine.rpg.content.Content
import com.littledungeon.engine.rpg.hero.Attribute
import com.littledungeon.engine.rpg.hero.Power
import com.littledungeon.engine.rpg.items.BattleUse
import com.littledungeon.engine.rpg.items.Item
import com.littledungeon.engine.rpg.items.ItemKind
import com.littledungeon.engine.rpg.world.LocationKind

/**
 * How a fight stands. Fights have as many rounds as the monster is strong: a little one falls in a
 * few hits, a boss takes many, and the hero's health, gear and items decide how it goes.
 */
internal class Fight(val monster: Monster, val maxHp: Int, val attack: Int) {
    var hp: Int = maxHp
    var round = 0
    var stunned = 0
    var shield = false
    var streak = 0
}

private fun Journey.fightScene(place: Place, f: Fight): Scene =
    scene(
        place, mood = Mood.CALM,
        battle = BattleView(FoeView(f.monster.id, f.monster.name, f.monster.art, f.hp.coerceAtLeast(0), f.maxHp, f.monster.tier == Tier.BOSS, f.monster.who), hp.coerceAtLeast(0), hero.maxHp, f.round),
    )

/** Stronger heroes meet stronger monsters; a hero who keeps fainting meets gentler ones. */
private fun Journey.scaled(m: Monster): Fight {
    val grow = 100 + 12 * (hero.level - 1) - 15 * faints.coerceAtMost(4)
    val maxHp = (m.hp * grow / 100).coerceAtLeast(m.hp / 2).coerceAtLeast(6)
    val attack = (m.attack + (hero.level - 1) / 3 - faints.coerceAtMost(4)).coerceAtLeast(1)
    return Fight(m, maxHp, attack)
}

/**
 * A monster turns up on the way and the child decides: fight it (two pictures, the swords and the way out), or slip away, which is
 * [retreat]. Asking "what will you do?" and then fighting whatever was answered was a question with no choice in it.
 */
internal fun Journey.encounter(m: Monster, s: Scene, retreat: () -> List<JStep>, onWin: () -> List<JStep>): List<JStep> {
    val choices = listOf(Choice("talk_fight", "Fight"), Choice("hub_leave", "Go away"))
    return listOf(
        JStep(Beat.Choose(s, Speech.of(say.fightAsk(m.name.lowercase())), choices)) { reply ->
            if ((reply as? Reply.Picked)?.index == 1) retreat() else battle(m, s.place, onWin)
        },
    )
}

/**
 * A fight. A win goes on to [onWin]; being knocked out carries the hero to the nearest safe place
 * (the camp or a town they have been to) with a quarter of their coins lost; the smoke pearl lets
 * them slip away to [onEscape].
 */
internal fun Journey.battle(m: Monster, place: Place, onWin: () -> List<JStep>, onEscape: () -> List<JStep> = { emptyList() }): List<JStep> {
    val f = scaled(m)
    val intro = tell(fightScene(place, f), m.taunt)
    return listOf(intro) + turn(f, place, onWin, onEscape)
}

// ------------------------------------------------------------- the hero's turn

/** What the hero can use right now, most useful first, at most three. */
private fun Journey.usable(f: Fight): List<Item> {
    val owned = hero.bag.keys.mapNotNull { Content.item(it) }
    val weakness = owned.filter { it.id == f.monster.weakness }
    val rest = owned.filter { it.kind == ItemKind.CONSUMABLE && it.id != f.monster.weakness }.filter {
        when (it.use) {
            BattleUse.HEAL -> hp < hero.maxHp
            BattleUse.BEFRIEND -> f.monster.befriendable
            null -> false
            else -> true
        }
    }.sortedBy {
        when (it.use) {
            BattleUse.DAMAGE -> 0
            BattleUse.HEAL -> if (hp * 100 / hero.maxHp <= 60) 0 else 3
            BattleUse.STUN -> 1
            BattleUse.SHIELD -> 2
            BattleUse.BEFRIEND -> 2
            else -> 4
        }
    }
    return (weakness + rest).distinctBy { it.id }.take(3)
}

private fun Journey.turn(f: Fight, place: Place, onWin: () -> List<JStep>, onEscape: () -> List<JStep>): List<JStep> {
    val items = usable(f)
    if (items.isEmpty()) return attack(f, place, onWin, onEscape)
    val s = fightScene(place, f)
    val choices = listOf(Choice("hub_attack", "Attack!")) + items.map { Choice("item_${it.id}", "Use the ${it.name}") }
    return listOf(
        JStep(Beat.Choose(s, Speech.of(say.yourTurn()), choices)) { reply ->
            val i = ((reply as? Reply.Picked)?.index ?: 0).coerceIn(choices.indices)
            if (i == 0) attack(f, place, onWin, onEscape) else useItem(f, items[i - 1], place, onWin, onEscape)
        },
    )
}

private fun Journey.attack(f: Fight, place: Place, onWin: () -> List<JStep>, onEscape: () -> List<JStep>): List<JStep> {
    f.round++
    val c = battlePuzzle(f.monster)
    val skill = c.skill
    val s = fightScene(place, f)
    return listOf(
        askOnce(
            s, c, say.attackOops(f.monster.name), say.attackYay(), null,
            onWin = { r ->
                val first = r.tries == 1
                f.streak = if (first) f.streak + 1 else 0
                // A hero who keeps fainting gets a little stronger each time, so nobody is stuck for good.
                val base = hero.attackWith(skill) + (faints * 2).coerceAtMost(30)
                val big = first && f.streak >= 3
                val dmg = if (big) base + base / 2 else base
                f.hp -= dmg
                val said = if (big) say.heroHitsBig(f.monster.name.lowercase(), dmg) else say.heroHits(f.monster.name.lowercase(), dmg)
                listOf(tell(fightScene(place, f), said)) + afterHero(f, place, onWin, onEscape, foeMayAttack = f.round % 3 == 0)
            },
            onFail = {
                f.streak = 0
                foeAttacks(f, place, onWin, onEscape)
            },
        ),
    )
}

/** After the hero acts: the monster is beaten, or the fight goes on (the monster hits back every third turn). */
private fun Journey.afterHero(f: Fight, place: Place, onWin: () -> List<JStep>, onEscape: () -> List<JStep>, foeMayAttack: Boolean): List<JStep> {
    if (f.hp <= 0) return victory(f, place, onWin, befriended = false)
    return if (foeMayAttack) foeAttacks(f, place, onWin, onEscape) else turn(f, place, onWin, onEscape)
}

private fun Journey.useItem(f: Fight, item: Item, place: Place, onWin: () -> List<JStep>, onEscape: () -> List<JStep>): List<JStep> {
    hero = hero.take(item.id)
    val name = f.monster.name.lowercase()
    val weak = item.id == f.monster.weakness
    return when {
        item.use == BattleUse.BEFRIEND && f.monster.befriendable -> victory(f, place, onWin, befriended = true)
        item.use == BattleUse.HEAL -> {
            val before = hp
            heal(item.power + hero.healBonus)
            listOf(tell(fightScene(place, f), say.healed(hp - before))) + foeAttacks(f, place, onWin, onEscape)
        }
        item.use == BattleUse.STUN -> {
            f.stunned = item.power.coerceAtLeast(1)
            listOf(tell(fightScene(place, f), say.foeStunned(name))) + turn(f, place, onWin, onEscape)
        }
        item.use == BattleUse.SHIELD -> {
            f.shield = true
            listOf(tell(fightScene(place, f), say.shieldUp())) + turn(f, place, onWin, onEscape)
        }
        item.use == BattleUse.ESCAPE -> listOf(tell(fightScene(place, f), say.escaped())) + onEscape()
        else -> {
            // Damage items, and the one thing a boss is weak to.
            val dmg = if (weak) (f.maxHp * 40 / 100).coerceAtLeast(item.power) else item.power
            f.hp -= dmg
            val said = if (weak) say.weaknessHit(name) else say.heroHits(name, dmg)
            listOf(tell(fightScene(place, f), said)) + afterHero(f, place, onWin, onEscape, foeMayAttack = true)
        }
    }
}

// ------------------------------------------------------------- the monster's turn

private fun Journey.foeAttacks(f: Fight, place: Place, onWin: () -> List<JStep>, onEscape: () -> List<JStep>): List<JStep> {
    val name = f.monster.name.lowercase()
    val s = fightScene(place, f)
    if (f.stunned > 0) {
        f.stunned--
        return listOf(tell(s, say.foeStunned(name))) + turn(f, place, onWin, onEscape)
    }
    if (f.shield) {
        f.shield = false
        return listOf(tell(s, say.shielded())) + turn(f, place, onWin, onEscape)
    }
    // Funny gear makes monsters giggle, and now and then they miss.
    if (hero.gear.any { it.funny } && random.nextInt(100) < 25) {
        return listOf(tell(s, say.foeMisses())) + turn(f, place, onWin, onEscape)
    }
    val dmg = (f.attack - hero.defense).coerceAtLeast(1)
    hp -= dmg
    val hit = tell(fightScene(place, f), say.foeHits(name, dmg), if (hp > 0) say.healthLine(hp, hero.maxHp) else null)
    return listOf(hit) + when {
        hp > 0 -> turn(f, place, onWin, onEscape)
        braveHeart() -> listOf(tell(fightScene(place, f), say.braveHeart())) + turn(f, place, onWin, onEscape)
        else -> faint(f, place)
    }
}

/** The Knight's power: once a journey, a knock-out is not the end. They stand back up with a quarter of their health. */
private fun Journey.braveHeart(): Boolean {
    if (hero.heroClass.power != Power.BRAVE_HEART || "run:brave_heart" in runFlags) return false
    runFlags += "run:brave_heart"
    hp = (hero.maxHp / 4).coerceAtLeast(1)
    return true
}

// ------------------------------------------------------------- the end of a fight

private fun Journey.victory(f: Fight, place: Place, onWin: () -> List<JStep>, befriended: Boolean): List<JStep> {
    val m = f.monster
    val s = fightScene(place, f).copy(mood = Mood.HAPPY)
    monstersBeaten++
    // A win lifts the spirits: a fifth of the way back to full health.
    heal(hero.maxHp / 5)
    gain(Attribute.COURAGE, when (m.tier) { Tier.MINION -> 5; Tier.ELITE -> 10; Tier.MINI_BOSS -> 15; Tier.BOSS -> 20 })
    if (befriended) {
        setFlag("friend:${m.id}")
        befriend("monster:${m.id}", 1)
    }
    val steps = mutableListOf<JStep>()
    steps += tell(s, if (befriended) say.befriended(m.name.lowercase()) else say.victory(m.name.lowercase()), if (befriended) null else m.beaten)
    val base = m.coins.random(random) / if (befriended) 2 else 1
    steps += coins(s, base)
    // Loot: what it drops, and for a guardian the key it was guarding.
    if (!befriended) m.drops.forEach { d -> if (random.nextInt(100) < d.percent) steps += item(s, d.itemId) }
    return steps + onWin()
}

/** Knocked out: carried to the nearest safe place, a quarter of the coins lost. */
private fun Journey.faint(f: Fight, place: Place): List<JStep> {
    faints++
    val lost = hero.coins / 4
    hero = hero.earn(-lost)
    val safe = nearestSafe()
    val s = fightScene(place, f).copy(battle = null)
    hp = (hero.maxHp / 2).coerceAtLeast(1)
    here = safe.id
    return listOf(
        tell(s, f.monster.wins, say.faint(lost)),
        tell(sceneAt(safe), say.wakeUp(safe.name)),
    ) + townHub(safe, first = false, woke = true)
}

/** The closest town (or the camp) the hero has been to. */
internal fun Journey.nearestSafe() = kingdom.locations
    .filter { (it.kind == LocationKind.TOWN || it.kind == LocationKind.CAMP) && it.id in visited }
    .minByOrNull { kingdom.hops(here, it.id) ?: 99 } ?: kingdom.camp
