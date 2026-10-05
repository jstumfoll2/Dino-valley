package com.littledungeon.engine.rpg

import com.littledungeon.engine.model.Speech
import com.littledungeon.engine.model.Voice
import com.littledungeon.engine.model.Who
import com.littledungeon.engine.rpg.hero.Attribute
import com.littledungeon.engine.rpg.hero.Hero
import com.littledungeon.engine.rpg.hero.HeroClass
import com.littledungeon.engine.rpg.hero.Progression
import com.littledungeon.engine.rpg.learn.ChallengeFactory
import com.littledungeon.engine.rpg.learn.Coach
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
import com.littledungeon.engine.rpg.run.Beat
import com.littledungeon.engine.rpg.run.Reply
import com.littledungeon.engine.rpg.run.PICK_ONE_SKILLS
import com.littledungeon.engine.rpg.run.Say
import com.littledungeon.engine.rpg.run.addStory
import com.littledungeon.engine.rpg.run.peaceChallenge
import com.littledungeon.engine.rpg.run.battleCostume
import com.littledungeon.engine.rpg.run.costumesFor
import com.littledungeon.engine.rpg.run.puzzleFor
import com.littledungeon.engine.rpg.run.vaultStory
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
                    is Beat.Finale -> Reply.Next
                }
                a.reply(reply)
            }
            (a.beat as? Beat.Finale)?.let { hear(it.summary.lines) }
        }
        enumerateDomains(::hear)
        JourneyLines.numbered().forEach { hear(Speech.of(it)) }
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
    val own = Content.npcs.flatMap { n -> n.nodes.flatMap { it.effects + it.options.flatMap { o -> o.effects } } }
        .filterIsInstance<Effect.Puzzle>().mapNotNull { e -> e.skill?.let { Costume(it, e.ask.orEmpty(), e.thing ?: Thing.STONE) } }
    val costumes = Obstacle.entries.flatMap { costumesFor(it) } + own +
        Content.monsters.flatMap { m -> PICK_ONE_SKILLS.map { battleCostume(m, it) } }
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
            for (step in arc.peaceSteps) repeat(60) { hear(j.peaceChallenge(step, boss).prompt) }
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
