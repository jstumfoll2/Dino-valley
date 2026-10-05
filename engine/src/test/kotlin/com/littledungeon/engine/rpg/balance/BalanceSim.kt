package com.littledungeon.engine.rpg.balance

import com.littledungeon.engine.model.Voice
import com.littledungeon.engine.rpg.hero.Hero
import com.littledungeon.engine.rpg.learn.ChallengeFactory
import com.littledungeon.engine.rpg.learn.ChallengeRecord
import com.littledungeon.engine.rpg.learn.PickOne
import com.littledungeon.engine.rpg.learn.Skill
import com.littledungeon.engine.rpg.learn.SkillBook
import com.littledungeon.engine.rpg.learn.Thing
import com.littledungeon.engine.rpg.run.Beat
import com.littledungeon.engine.rpg.run.Journey
import com.littledungeon.engine.rpg.run.Reply
import com.littledungeon.engine.rpg.run.effortSeconds
import com.littledungeon.engine.rpg.run.speech
import com.littledungeon.engine.rpg.world.WorldMemory
import com.littledungeon.engine.util.Clock
import java.io.File
import kotlin.random.Random

/**
 * The balance harness: plays simulated children through many journeys and measures what the
 * game is like to play (how long it takes, which skills come up, whether levels drift, whether the
 * answer can be guessed from where it sits). `docs/review/2026-10-04-review-and-roadmap.md` is where
 * the first numbers came from; the tests in this package keep them from slipping back.
 *
 * Run the full report with `./gradlew :engine:balanceReport`; CI prints it in the run summary.
 */
object BalanceSim {
    /** What one batch of simulated play looked like. */
    class Batch(val accuracy: Double, val children: Int, val journeysEach: Int) {
        var journeys = 0
        var finished = 0
        var asks = 0
        var asksFailed = 0
        var travels = 0
        var menus = 0
        var battles = 0
        var faints = 0
        var forcedFights = 0
        var namedAmbushes = 0
        var tellsWithRetryWords = 0
        val beats = mutableListOf<Int>()
        val minutes = mutableListOf<Double>()

        /** Minutes of each sitting: a journey is played in days, and the child can stop at every night. */
        val sessionMinutes = mutableListOf<Double>()
        var nights = 0
        val skills: MutableMap<Skill, Int> = sortedMapOf()
        val monsters: MutableMap<String, Int> = sortedMapOf()
        val finalLevels = mutableListOf<Map<Skill, Int>>()

        /** Estimated seconds and count by kind of beat: what a journey is made of. */
        val secondsByKind: MutableMap<String, Double> = sortedMapOf()
        val beatsByKind: MutableMap<String, Int> = sortedMapOf()
        var clockStart = 1_000_000L

        fun median(xs: List<Double>) = xs.sorted()[(xs.size - 1) / 2]
        fun percentile(xs: List<Double>, q: Double) = xs.sorted()[((xs.size - 1) * q).toInt()]
        val medianMinutes get() = median(minutes)
        val medianSession get() = median(sessionMinutes)
        val medianBeats get() = median(beats.map { it.toDouble() })
        val puzzlesPerJourney get() = asks.toDouble() / journeys
        fun share(skill: Skill) = (skills[skill] ?: 0).toDouble() / asks.coerceAtLeast(1)
        fun avgLevel(skill: Skill) = finalLevels.map { it.getValue(skill) }.average()
    }

    /** Monsters that are also people with their own story: they should never turn up as a random encounter. */
    val NAMED_FOES = setOf("bandit_bess", "sneaky_fox", "grumble_troll")

    /** The time model is the engine's own ([effortSeconds]), which is also what decides when the party camps. */
    /**
     * One simulated journey. [accuracy] is the chance of getting a puzzle right; the child follows
     * the baby dragon's marked road 60% of the time and otherwise picks any open road; dialog
     * choices are random.
     */
    fun playJourney(accuracy: Double, seed: Long, hero: Hero, skills: SkillBook, world: WorldMemory, batch: Batch, r: Random): Journey {
        // A clock that moves with the child's time, so "practiced longest ago" means something.
        var now = batch.clockStart
        val j = Journey(seed, hero, skills, world, Clock { now })
        var guard = 0
        var seconds = 0.0
        var sitting = 0.0
        var foe: String? = null
        var ambush = false
        while (!j.finished && guard++ < 4000) {
            val b = j.beat
            // An encounter is announced ("A fox blocks the way!") and the fight starts on the very next beat.
            val ambushNow = ambush
            ambush = false
            val effort = b.effortSeconds()
            seconds += effort
            sitting += effort
            now += (effort * 1000).toLong()
            val battle = b.scene.battle
            if (battle == null) foe = null
            else if (battle.foe.id != foe) {
                foe = battle.foe.id
                batch.battles++
                batch.monsters.merge(battle.foe.id, 1, Int::plus)
                // A road or wild encounter is announced ("A fox blocks the way!"); a fight picked in a conversation is not.
                if (ambushNow && battle.foe.id in NAMED_FOES) batch.namedAmbushes++
            }
            val reply: Reply = when (b) {
                is Beat.Tell, is Beat.Found -> {
                    if (b is Beat.Tell && Voice.caption(b.lines).let { "blocks the way" in it || "Here comes a" in it }) ambush = true
                    Reply.Next
                }
                is Beat.Ask -> {
                    batch.asks++
                    batch.skills.merge(b.challenge.skill, 1, Int::plus)
                    val ok = r.nextDouble() < accuracy
                    if (b.oneTry && !ok) {
                        batch.asksFailed++
                        if (Voice.caption(b.oops).contains(Regex("(?i)try (again|another)|count again"))) batch.tellsWithRetryWords++
                        val wrong = ((b.challenge as? PickOne)?.answer ?: 0).let { if (it == 0) 1 else 0 }
                        Reply.Solved(1, 0, 5000, failed = true, wrong = listOf(wrong))
                    } else {
                        Reply.Solved(if (ok) 1 else 2, 0, 5000)
                    }
                }
                is Beat.Choose -> {
                    // The same announcement as a story beat: a monster turns up, and the fight starts if the child chooses it.
                    if (Voice.caption(b.prompt).let { "blocks the way" in it || "Here comes a" in it }) ambush = true
                    batch.menus++
                    if (b.options.isNotEmpty() && b.options.all { it.icon == "talk_fight" }) batch.forcedFights++
                    Reply.Picked(r.nextInt(b.options.size))
                }
                is Beat.Travel -> {
                    batch.travels++
                    val open = b.routes.indices.filter { !b.routes[it].blocked }
                    val marked = b.routes.indexOfFirst { it.marked }
                    Reply.Picked(if (marked >= 0 && r.nextDouble() < 0.6) marked else open.random(r))
                }
                is Beat.Shop -> {
                    val can = b.stock.filter { it.canAfford }
                    if (r.nextInt(4) == 0 && can.isNotEmpty()) Reply.Bought(can.random(r).itemId) else Reply.Next
                }
                is Beat.Roll -> Reply.Rolled(false, 1)
                is Beat.Night -> {
                    // Stopping at the fire: the sitting ends here and the next begins in the morning.
                    batch.nights++
                    batch.sessionMinutes += sitting / 60.0
                    sitting = 0.0
                    Reply.Next
                }
                is Beat.Finale -> Reply.Next
            }
            val kind = b::class.simpleName.orEmpty()
            batch.secondsByKind.merge(kind, effort, Double::plus)
            batch.beatsByKind.merge(kind, 1, Int::plus)
            j.reply(reply)
        }
        batch.faints += j.faints
        batch.clockStart += 600_000L
        batch.sessionMinutes += sitting / 60.0
        batch.journeys++
        if (j.finished) batch.finished++
        batch.beats += guard
        batch.minutes += seconds / 60.0
        return j
    }

    /** [children] simulated children, each playing [journeysEach] journeys in a row (hero, skills and world carry forward). */
    fun run(accuracy: Double, children: Int = 10, journeysEach: Int = 10, seed: Int = 42): Batch {
        val batch = Batch(accuracy, children, journeysEach)
        val r = Random(seed)
        repeat(children) {
            var hero = Hero()
            var skills = SkillBook()
            var world = WorldMemory()
            repeat(journeysEach) {
                val j = playJourney(accuracy, r.nextLong(), hero, skills, world, batch, r)
                hero = j.hero
                skills = j.skills
                world = j.world
            }
            batch.finalLevels += Skill.entries.associateWith { skills.level(it) }
        }
        return batch
    }

    // ------------------------------------------------------------- answer position

    /** How often the right answer is in [position] of a pick-one puzzle (0 first), over [n] generated puzzles. */
    fun positionShare(level: Int, make: (Int, Long) -> PickOne, n: Int = 3000): List<Double> {
        val size = make(level, 1L).optionCount
        val wins = IntArray(size)
        for (s in 0 until n) wins[make(level, s.toLong()).answer]++
        return wins.map { it.toDouble() / n }
    }

    val countMaker: (Int, Long) -> PickOne = { lv, s -> ChallengeFactory.count(lv, s, Thing.STONE, "") }
    val addMaker: (Int, Long) -> PickOne = { lv, s -> ChallengeFactory.add(lv, s, Thing.COIN) { _, _, _ -> "" } }
    val skipMaker: (Int, Long) -> PickOne = { lv, s -> ChallengeFactory.skipCount(lv, s) }

    /** A child who only guesses, on one-try counting puzzles: the level they reach after [puzzles] of them. */
    fun guesserLevel(puzzles: Int = 40, seed: Int = 3): Int {
        var book = SkillBook()
        val r = Random(seed)
        repeat(puzzles) { i ->
            val c = ChallengeFactory.count(book.level(Skill.COUNTING), i.toLong(), Thing.STONE, "")
            val ok = r.nextInt(c.optionCount) == c.answer
            book = book.record(ChallengeRecord(Skill.COUNTING, "count", c.level, 1, 0, 1, i.toLong(), 0, failed = !ok))
        }
        return book.level(Skill.COUNTING)
    }

    // ------------------------------------------------------------- the report

    private fun pct(x: Double) = "%.0f%%".format(x * 100)
    private fun f1(x: Double) = "%.1f".format(x)

    fun report(accuracies: List<Double> = listOf(0.6, 0.75, 0.9), children: Int = 10, journeysEach: Int = 10): String {
        val batches = accuracies.map { run(it, children, journeysEach) }
        val sb = StringBuilder()
        sb.appendLine("## Balance report ($children children x $journeysEach journeys per accuracy)\n")
        sb.appendLine("| Accuracy | Median beats | Journey minutes p10 / median / p90 | Sitting minutes p10 / median / p90 | Nights | Puzzles | Menus | Battles | Faints | Forced-fight menus | Named characters ambushing | One-try misses saying try again |")
        sb.appendLine("|---|---|---|---|---|---|---|---|---|---|---|---|")
        for (b in batches) {
            sb.appendLine(
                "| ${pct(b.accuracy)} | ${f1(b.medianBeats)} | ${f1(b.percentile(b.minutes, 0.1))} / ${f1(b.medianMinutes)} / ${f1(b.percentile(b.minutes, 0.9))} | " +
                    "${f1(b.percentile(b.sessionMinutes, 0.1))} / ${f1(b.medianSession)} / ${f1(b.percentile(b.sessionMinutes, 0.9))} | ${f1(b.nights.toDouble() / b.journeys)} | " +
                    "${f1(b.puzzlesPerJourney)} | ${f1(b.menus.toDouble() / b.journeys)} | ${f1(b.battles.toDouble() / b.journeys)} | " +
                    "${"%.2f".format(b.faints.toDouble() / b.journeys)} | ${b.forcedFights} | ${b.namedAmbushes} | ${b.tellsWithRetryWords} of ${b.asksFailed} |",
            )
        }
        sb.appendLine("\n### Where the time goes (first batch, minutes and beats per journey)\n")
        sb.appendLine("| Beat | Minutes | Beats |")
        sb.appendLine("|---|---|---|")
        val first = batches.first()
        for ((kind, secs) in first.secondsByKind) {
            sb.appendLine("| $kind | ${f1(secs / 60 / first.journeys)} | ${f1(first.beatsByKind.getValue(kind).toDouble() / first.journeys)} |")
        }
        sb.appendLine("\n### Share of puzzles by skill\n")
        sb.appendLine("| Skill | " + batches.joinToString(" | ") { pct(it.accuracy) + " accuracy" } + " |")
        sb.appendLine("|---|" + batches.joinToString("") { "---|" })
        for (s in Skill.entries) sb.appendLine("| ${s.name.lowercase()} | " + batches.joinToString(" | ") { "%.1f%%".format(it.share(s) * 100) } + " |")
        sb.appendLine("\n### Average skill level after $journeysEach journeys\n")
        sb.appendLine("| Skill | " + batches.joinToString(" | ") { pct(it.accuracy) + " accuracy" } + " |")
        sb.appendLine("|---|" + batches.joinToString("") { "---|" })
        for (s in Skill.entries) sb.appendLine("| ${s.name.lowercase()} | " + batches.joinToString(" | ") { f1(it.avgLevel(s)) } + " |")
        sb.appendLine("\n### Where the right answer sits (best position's share; fair is 1 divided by the number of answers)\n")
        sb.appendLine("| Puzzle | L1 | L2 | L3 | L4 | L5 |")
        sb.appendLine("|---|---|---|---|---|---|")
        for ((name, make) in listOf("counting" to countMaker, "adding" to addMaker, "skip counting" to skipMaker)) {
            sb.appendLine("| $name | " + (1..5).joinToString(" | ") { lv ->
                val shares = positionShare(lv, make, 1500)
                "${pct(shares.max())} (fair ${pct(1.0 / shares.size)})"
            } + " |")
        }
        sb.appendLine("\nA child who only guesses reaches counting level ${guesserLevel()} after 40 one-try puzzles.")
        return sb.toString()
    }
}

fun main(args: Array<String>) {
    val text = BalanceSim.report()
    println(text)
    args.firstOrNull()?.let { path ->
        val out = File(path)
        out.parentFile?.mkdirs()
        out.writeText(text)
    }
}
