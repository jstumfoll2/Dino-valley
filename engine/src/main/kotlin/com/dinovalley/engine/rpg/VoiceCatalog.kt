package com.dinovalley.engine.rpg

import com.dinovalley.engine.model.Speech
import com.dinovalley.engine.model.Voice
import com.dinovalley.engine.model.Who
import com.dinovalley.engine.rpg.hero.Attribute
import com.dinovalley.engine.rpg.hero.Hero
import com.dinovalley.engine.rpg.hero.HeroClass
import com.dinovalley.engine.rpg.hero.Progression
import com.dinovalley.engine.rpg.learn.MemoryChallenge
import com.dinovalley.engine.rpg.learn.RecipeChallenge
import com.dinovalley.engine.rpg.learn.Skill
import com.dinovalley.engine.rpg.learn.SkillBook
import com.dinovalley.engine.rpg.run.Adventure
import com.dinovalley.engine.rpg.run.Beat
import com.dinovalley.engine.rpg.run.Reply
import com.dinovalley.engine.rpg.run.Say
import com.dinovalley.engine.rpg.world.QuestKind
import com.dinovalley.engine.rpg.world.WorldMemory
import com.dinovalley.engine.util.Clock
import java.io.File
import kotlin.random.Random

/**
 * Every sentence the narrator can say. Adventures are played thousands of times with every
 * kind of hero, skill level, world and answer, and every line they speak is collected, along
 * with the screens' own lines ([Say]). The build records each one (scripts/render-voice.py),
 * so the phone plays sound files instead of making speech.
 */
object VoiceCatalog {
    fun speech(runs: Int = 6000, seed: Int = 7): Pair<Set<Voice.Piece.Say>, Set<String>> {
        val said = mutableListOf<List<Speech>>()
        val choices = mutableSetOf<String>()
        val r = Random(seed)
        repeat(runs) { run ->
            val hero = Hero(HeroClass.entries[run % HeroClass.entries.size], mapOf(Attribute.entries.random(r) to Progression.xpFor(r.nextInt(1, 13)) + 1))
            val skills = SkillBook(levels = Skill.entries.associateWith { r.nextInt(1, 6) })
            val world = if (run % 4 == 0) {
                WorldMemory()
            } else {
                WorldMemory(
                    adventures = r.nextInt(1, 20),
                    friends = if (r.nextBoolean()) setOf(listOf("Pip", "Nib", "Tock", "Moss", "Bindle", "Wobble").random(r)) else emptySet(),
                    dragonFriend = if (r.nextInt(3) == 0) listOf("Ember", "Cinder", "Bramble", "Smolder", "Puddle", "Glim").random(r) else null,
                    lastQuest = QuestKind.entries.random(r),
                )
            }
            val a = Adventure(r.nextLong(), hero, skills, world, Clock { 0L })
            var guard = 0
            while (!a.finished && guard++ < 400) {
                val b = a.beat
                val reply = when (b) {
                    is Beat.Tell -> {
                        said += b.lines
                        Reply.Next
                    }
                    is Beat.Found -> {
                        said += b.lines
                        Reply.Next
                    }
                    is Beat.Ask -> {
                        said += b.challenge.prompt
                        said += b.oops
                        said += b.yay
                        when (val c = b.challenge) {
                            is MemoryChallenge -> said += c.remember
                            is RecipeChallenge -> c.riddle?.let { said += it }
                            else -> Unit
                        }
                        val tries = r.nextInt(1, 4)
                        Reply.Solved(tries, tries - 1, 1000)
                    }
                    is Beat.Roll -> {
                        said += b.why
                        Reply.Rolled(r.nextBoolean(), r.nextInt(1, 3))
                    }
                    is Beat.Choose -> {
                        said += b.prompt
                        b.options.forEach { choices += it.said }
                        Reply.Picked(r.nextInt(b.options.size))
                    }
                    is Beat.Doors -> {
                        said += b.prompt
                        b.offers.forEach { said += it }
                        Reply.Picked(r.nextInt(b.fork.doors.size))
                    }
                    is Beat.Travel -> {
                        said += b.prompt
                        b.routes.forEach { said += it.said }
                        Reply.Picked(r.nextInt(b.routes.size))
                    }
                    is Beat.Shop -> {
                        said += b.prompt
                        Reply.Next
                    }
                    is Beat.Finale -> Reply.Next
                }
                a.reply(reply)
            }
            (a.beat as? Beat.Finale)?.let { said += it.summary.lines }
        }
        Say.all(choices.toList()).forEach { said += Speech.of(it) }
        val sentences = linkedSetOf<Voice.Piece.Say>()
        val sounds = sortedSetOf<String>()
        for (s in said) {
            for (p in Voice.pieces(s)) {
                when (p) {
                    is Voice.Piece.Say -> sentences += p
                    is Voice.Piece.Sound -> sounds += p.id
                }
            }
        }
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
