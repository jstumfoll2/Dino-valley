package com.littledungeon.engine.rpg

import com.littledungeon.engine.model.Speech
import com.littledungeon.engine.model.Voice
import com.littledungeon.engine.model.Who
import com.littledungeon.engine.rpg.hero.Attribute
import com.littledungeon.engine.rpg.hero.Hero
import com.littledungeon.engine.rpg.hero.HeroClass
import com.littledungeon.engine.rpg.hero.Progression
import com.littledungeon.engine.rpg.learn.MemoryChallenge
import com.littledungeon.engine.rpg.learn.RecipeChallenge
import com.littledungeon.engine.rpg.learn.Skill
import com.littledungeon.engine.rpg.learn.SkillBook
import com.littledungeon.engine.rpg.content.Content
import com.littledungeon.engine.rpg.run.Journey
import com.littledungeon.engine.rpg.run.JourneyLines
import com.littledungeon.engine.rpg.run.Beat
import com.littledungeon.engine.rpg.run.Reply
import com.littledungeon.engine.rpg.run.Say
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
        JourneyLines.numbered().forEach { hear(Speech.of(it)) }
        Say.all(choices.toList()).forEach { hear(Speech.of(it)) }
        return sentences.sortedWith(compareBy({ it.who }, { it.text })).toCollection(linkedSetOf()) to sounds
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
