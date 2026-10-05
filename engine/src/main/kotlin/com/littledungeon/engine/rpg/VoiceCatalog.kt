package com.littledungeon.engine.rpg

import com.littledungeon.engine.model.Speech
import com.littledungeon.engine.model.Voice
import com.littledungeon.engine.model.Who
import com.littledungeon.engine.rpg.hero.Attribute
import com.littledungeon.engine.rpg.hero.Hero
import com.littledungeon.engine.rpg.hero.HeroClass
import com.littledungeon.engine.rpg.hero.Progression
import com.littledungeon.engine.rpg.learn.ChallengeFactory
import com.littledungeon.engine.rpg.learn.PictureFactory
import com.littledungeon.engine.rpg.learn.Coach
import com.littledungeon.engine.rpg.learn.BellChallenge
import com.littledungeon.engine.rpg.learn.MemoryChallenge
import com.littledungeon.engine.rpg.learn.RecipeChallenge
import com.littledungeon.engine.rpg.learn.Skill
import com.littledungeon.engine.rpg.learn.SkillBook
import com.littledungeon.engine.rpg.learn.Thing
import com.littledungeon.engine.rpg.items.Obstacle
import com.littledungeon.engine.rpg.story.Effect
import com.littledungeon.engine.rpg.content.Content
import com.littledungeon.engine.rpg.run.Costume
import com.littledungeon.engine.rpg.run.Journey
import com.littledungeon.engine.rpg.run.JourneyLines
import com.littledungeon.engine.rpg.run.tunnelTrace
import com.littledungeon.engine.rpg.run.Beat
import com.littledungeon.engine.rpg.run.Reply
import com.littledungeon.engine.rpg.run.RoomLines
import com.littledungeon.engine.rpg.run.BATTLE_SKILLS
import com.littledungeon.engine.rpg.run.PICK_ONE_SKILLS
import com.littledungeon.engine.rpg.run.Say
import com.littledungeon.engine.rpg.run.addStory
import com.littledungeon.engine.rpg.run.peaceChallenge
import com.littledungeon.engine.rpg.run.battleCostume
import com.littledungeon.engine.rpg.run.costumesFor
import com.littledungeon.engine.rpg.run.puzzleFor
import com.littledungeon.engine.rpg.run.vaultStory
import com.littledungeon.engine.rpg.world.Terrain
import com.littledungeon.engine.rpg.world.WorldMemory
import com.littledungeon.engine.util.Clock
import java.io.File
import kotlin.random.Random

/**
 * Every sentence the narrator can say. Adventures are played thousands of times with every
 * kind of hero, skill level, world and answer, and every line they speak is collected, along
 * with the screens' own lines ([Say]). The build records each one (scripts/render-voice.py),
 * so the phone plays sound files instead of making speech.
 */
object VoiceCatalog {
    fun speech(runs: Int = 3000, seed: Int = 7): Pair<Set<Voice.Piece.Say>, Set<String>> {
        val sentences = linkedSetOf<Voice.Piece.Say>()
        val sounds = sortedSetOf<String>()
        // Sorted into pieces as it goes: thousands of adventures would not fit in memory as whole lines.
        fun hear(lines: List<Speech>) {
            for (p in Voice.pieces(lines)) {
                when (p) {
                    is Voice.Piece.Say -> sentences += p
                    is Voice.Piece.Sound -> sounds += p.id
                }
            }
        }
        val choices = mutableSetOf<String>()
        val r = Random(seed)
        repeat(runs) { run ->
            val hero = Hero(
                HeroClass.entries[run % HeroClass.entries.size], mapOf(Attribute.entries.random(r) to Progression.xpFor(r.nextInt(1, 13)) + 1),
                coins = r.nextInt(0, 400),
                // A bag with a few things in it, so rescues, battles and gifts all come up.
                bag = Content.items.shuffled(r).take(r.nextInt(0, 6)).associate { it.id to 1 },
            )
            val skills = SkillBook(levels = Skill.entries.associateWith { r.nextInt(1, 6) })
            val world = if (run % 4 == 0) {
                WorldMemory()
            } else {
                WorldMemory(adventures = r.nextInt(1, 20), pages = r.nextInt(0, 5))
            }
            val a = Journey(r.nextLong(), hero, skills, world, Clock { 0L })
            var guard = 0
            while (!a.finished && guard++ < 3000) {
                val b = a.beat
                val reply = when (b) {
                    is Beat.Tell -> {
                        hear(b.lines)
                        Reply.Next
                    }
                    is Beat.Found -> {
                        hear(b.lines)
                        Reply.Next
                    }
                    is Beat.Ask -> {
                        hear(b.challenge.prompt)
                        hear(b.oops)
                        hear(b.yay)
                        hear(b.explain)
                        when (val c = b.challenge) {
                            is MemoryChallenge -> hear(c.remember)
                            is BellChallenge -> hear(c.listen)
                            is RecipeChallenge -> c.riddle?.let { hear(it) }
                            else -> Unit
                        }
                        val tries = r.nextInt(1, 4)
                        // One-try puzzles are sometimes failed, so the rescue and turning-back words are heard too.
                        if (b.oneTry && r.nextInt(3) == 0) Reply.Solved(1, 0, 1000, failed = true) else Reply.Solved(tries, tries - 1, 1000)
                    }
                    is Beat.Roll -> {
                        hear(b.why)
                        Reply.Rolled(r.nextBoolean(), r.nextInt(1, 3))
                    }
                    is Beat.Choose -> {
                        hear(b.prompt)
                        b.options.forEach { choices += it.said }
                        Reply.Picked(r.nextInt(b.options.size))
                    }
                    is Beat.Travel -> {
                        hear(b.prompt)
                        b.routes.forEach { hear(it.said) }
                        Reply.Picked(r.nextInt(b.routes.size))
                    }
                    is Beat.Shop -> {
                        hear(b.prompt)
                        if (b.stock.isNotEmpty() && r.nextInt(3) > 0) Reply.Bought(b.stock.random(r).itemId) else Reply.Next
                    }
                    is Beat.Night -> {
                        hear(b.lines)
                        Reply.Next
                    }
                    is Beat.Finale -> Reply.Next
                }
                a.reply(reply)
            }
            (a.beat as? Beat.Finale)?.let { hear(it.summary.lines) }
        }
        enumerateDomains(::hear)
        JourneyLines.numbered().forEach { hear(Speech.of(it)) }
        // What every choice says, whether or not a random adventure met the person who offers it.
        for (npc in Content.npcs) for (n in npc.nodes) for (o in n.options) choices += o.said
        for (arc in Content.arcs) choices += listOf(arc.fightLabel, arc.peaceLabel)
        Say.all(choices.toList()).forEach { hear(Speech.of(it)) }
        return sentences.sortedWith(compareBy({ it.who }, { it.text })).toCollection(linkedSetOf()) to sounds
    }
}

/**
 * Everything whose words depend on a small set of numbers, colors, letters or names, said once each, so no sentence of
 * them is left to the chance of a random adventure reaching it. Random play (above) still finds what depends on the story.
 */
private fun enumerateDomains(hear: (List<Speech>) -> Unit) {
    // Every sum a puzzle can make, in every costume that holds one.
    for (have in 1..9) for (more in 1..(10 - have)) for (missing in listOf(false, true)) {
        for (thing in Thing.entries) hear(Speech.of(addStory("", thing, have, more, missing)))
        hear(Speech.of(vaultStory(have, more, missing)))
    }
    // Tell it back: every place, and every pair of places one after the other.
    for (a in Content.locations) {
        hear(Speech.of("After ${a.name}, where did you go next?"))
        hear(Speech.of("Where did you go before ${a.name}?"))
        hear(Speech.of("${a.name} was the first place you went to."))
        for (b in Content.locations) {
            hear(Speech.of("You went from ${a.name} to ${b.name}."))
            hear(Speech.of("You went to ${a.name}, and then to ${b.name}."))
        }
    }
    // The pre-writing shapes and letters of the tunnel, with each of its purposes.
    for (level in 1..5) for (seed in 0L until 40L) for (purpose in listOf("Let's light the tunnel!", "Draw with your magic finger!", "Make the wall glow!")) {
        hear(tunnelTrace(level, seed, purpose).prompt)
    }
    // Every picture puzzle, at every level: small sets of words, foods and numbers, so a few hundred seeds reach them all.
    for (level in 1..5) for (seed in 0L until 600L) {
        for (c in listOf(PictureFactory.rhyme(level, seed), PictureFactory.money(level, seed)) + PictureFactory.ShareTheme.entries.map { PictureFactory.share(level, seed, theme = it) }) {
            hear(c.prompt)
            hear(c.because)
        }
        PictureFactory.map(level, seed).let { hear(it.prompt); hear(it.because) }

    }
    // Every recipe a workshop can ask for: each step is its own sentence, so a few hundred recipes reach them all.
    for (level in 1..5) for (potion in com.littledungeon.engine.rpg.learn.PotionKind.entries) for (seed in 0L until 150L) {
        val recipe = ChallengeFactory.recipe(level, seed, potion)
        hear(recipe.prompt)
        recipe.riddle?.let { hear(it) }
    }
    // Everything the stories and people say, whether or not random play happened to reach it in this world.
    for (arc in Content.arcs) {
        val said = arc.setup + arc.returnSetup + listOfNotNull(arc.sealed, arc.keyFound, arc.gateOpens, arc.ask, arc.friendMeeting, arc.friendEnd, arc.rivalMeeting) +
            (arc.peaceSteps + arc.friendSteps).flatMap { listOfNotNull(it.intro, it.yay, it.skipNote) } +
            arc.variants.flatMap { listOf(it.meeting, it.fightEnd, it.peaceEnd) } + arc.moments.map { it.says }
        said.forEach { hear(Speech.of(it)) }
    }
    for (npc in Content.npcs) {
        hear(Speech.of("<${npc.who.tag}>${npc.intro}"))
        for (n in npc.nodes) hear(Speech.of("<${npc.who.tag}>${n.says}"))
    }
    for (m in Content.monsters) for (t in listOf(m.taunt, m.beaten, m.wins)) hear(Speech.of(t))
    // Every line that takes nothing in, every way it can be said: a rare variant is drawn by asking many times.
    fun everyPlainLine(from: Any) {
        for (method in from::class.java.declaredMethods) {
            if (method.parameterCount != 0 || method.returnType != String::class.java || method.isSynthetic) continue
            method.isAccessible = true
            repeat(60) { hear(Speech.of(method.invoke(from) as String)) }
        }
    }
    everyPlainLine(JourneyLines(Random(2)))
    everyPlainLine(RoomLines(Random(3)))
    // Lines that name a person, an item, a monster or a place, for every one of them.
    val lines = JourneyLines(Random(1))
    for (arc in Content.arcs) for (chapter in 1..5) hear(Speech.of(lines.chapter(chapter, arc.title)))
    for (item in Content.items) repeat(30) {
        hear(Speech.of(lines.itemFound(item.name)))
        hear(Speech.of(lines.bought(item.name, 7)))
        hear(Speech.of(lines.foundGear(item.name)))
    }
    for (m in Content.monsters) repeat(30) {
        val lower = m.name.lowercase()
        for (line in listOf(lines.fightAsk(lower), lines.attackOops(m.name), lines.foeStunned(lower), lines.weaknessHit(lower), lines.befriended(lower), lines.victory(lower))) hear(Speech.of(line))
    }
    for (l in Content.locations) repeat(30) {
        for (line in listOf(
            lines.arriveTown(l.name), lines.arriveWild(l.name), lines.hubAsk(l.name), lines.quiet(l.name), lines.dungeonAsk(l.name), lines.dungeonEnter(l.name),
            lines.alreadyDone(l.name), lines.dungeonDone(l.name), lines.thrownOut(l.name), lines.turnedBack(l.name), lines.backAway(l.name), lines.blockedRoad(l.name), lines.wakeUp(l.name),
        )) hear(Speech.of(line))
        for (terrain in Terrain.entries) for (danger in listOf(null, 0, 1, 2)) for (visited in listOf(false, true)) for (marked in listOf(false, true)) {
            hear(Speech.of(lines.routeSaid(l.name, terrain, danger, visited, marked)))
        }
    }
    for (npc in Content.npcs) repeat(30) { hear(Speech.of(lines.shopWelcome(npc.name))) }
    for (shop in Content.shops) repeat(30) { hear(Speech.of(lines.shopWelcome(shop.name))) }
    for (npc in Content.npcs.filter { it.passFlags.isNotEmpty() }) repeat(40) { hear(Speech.of(lines.tollBlocked(npc.name))) }
    val own = Content.npcs.flatMap { n -> n.nodes.flatMap { it.effects + it.options.flatMap { o -> o.effects } } }
        .filterIsInstance<Effect.Puzzle>().mapNotNull { e -> e.skill?.let { Costume(it, e.ask.orEmpty(), e.thing ?: Thing.STONE) } }
    val costumes = Obstacle.entries.flatMap { costumesFor(it) } + own +
        Content.monsters.flatMap { m -> BATTLE_SKILLS.map { battleCostume(m, it) } }
    for (level in 1..5) {
        // A journey that has warmed up, with every skill at this level, so puzzles come at exactly this level.
        val j = Journey(level.toLong(), Hero(), SkillBook(levels = PICK_ONE_SKILLS.associateWith { level }), WorldMemory(), Clock { 0L })
        j.warmedUp.addAll(PICK_ONE_SKILLS)
        for (c in costumes) repeat(25) {
            val p = j.puzzleFor(c.skill, c.intro, c.thing)
            hear(p.prompt)
            hear(Coach.explain(p))
        }
        // The puzzles a boss sets on the peaceful way, in the boss's voice.
        for (arc in Content.arcs) {
            val boss = Content.monster(arc.bossId) ?: continue
            for (step in arc.peaceSteps + arc.friendSteps) repeat(60) {
                val c = j.peaceChallenge(step, boss)
                hear(c.prompt)
                (c as? BellChallenge)?.let { b -> hear(b.listen) }
            }
        }
        // The puzzle rooms of a dungeon, as the journey sets them up.
        for (seed in 1L..200L) {
            hear(ChallengeFactory.count(level, seed, Thing.STONE, "How many stones are on the bridge?").prompt)
            hear(ChallengeFactory.color(level, seed, "", speaker = Who.WIZARD).prompt)
            hear(ChallengeFactory.letter(level, seed, "").prompt)
            hear(ChallengeFactory.write(level, seed, "").prompt)
            hear(ChallengeFactory.write(level, seed, "", number = true).prompt)
            hear(ChallengeFactory.memory(level, seed).prompt)
            hear(ChallengeFactory.memory(level, seed).remember)
            ChallengeFactory.bells(level, seed).let { hear(it.prompt); hear(it.listen) }
            hear(ChallengeFactory.sort(level, seed).prompt)
            hear(ChallengeFactory.skipCount(level, seed).prompt)
            hear(ChallengeFactory.puzzle(level, seed).prompt)
            hear(ChallengeFactory.pattern(level, seed).prompt)
        }
    }
}

fun main(args: Array<String>) {
    val (sentences, sounds) = VoiceCatalog.speech()
    val out = File(args.firstOrNull() ?: "lines.txt")
    out.parentFile?.mkdirs()
    // One recording per line: voice id, speaker, speed, pitch, then the sentence, separated by tabs.
    out.writeText(sentences.joinToString("\n", postfix = "\n") { "${Voice.voiceIdOf(it.text, it.who)}\t${it.who.sid}\t${it.who.speed}\t${it.who.pitch}\t${it.text}" })
    println("${sentences.size} sentences, ${sentences.sumOf { it.text.length }} characters -> $out")
    println("sound effects: ${sounds.joinToString(" ")}")
}
