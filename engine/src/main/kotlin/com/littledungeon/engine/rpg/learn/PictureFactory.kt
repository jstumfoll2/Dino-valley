package com.littledungeon.engine.rpg.learn

import com.littledungeon.engine.model.Speech
import kotlin.random.Random

/**
 * Makes the picture challenges (see [PictureChallenge]). Every sentence is built from small sets (seventeen words, two foods, numbers to
 * twelve) and never mixes a long list of names with numbers, so the voice catalog can list them all.
 */
object PictureFactory {
    /** Words with a picture (`art_mini_rhyme_<word>`), grouped by how they end. */
    val RHYMES: List<List<String>> = listOf(
        listOf("cat", "hat", "bat", "mat"), listOf("dog", "log", "frog"), listOf("pig", "wig"), listOf("sun", "bun"),
        listOf("bee", "tree"), listOf("fox", "box"), listOf("car", "jar"),
    )
    val WORDS: List<String> = RHYMES.flatten()

    /** The foods of the market stall, and their pictures. */
    val FOODS = listOf("apple" to "mini_apple", "pie" to "mini_pie")

    private fun word(w: String) = Card(listOf(Shown("mini_rhyme_$w")), w)
    private fun numeral(n: Int) = Card(emptyList(), n.toString())

    private fun rhymeOf(w: String) = RHYMES.first { w in it }
    private fun firstLetter(w: String) = w.first()

    fun rhyme(level: Int, seed: Long, intro: String = ""): PictureChallenge {
        val r = Random(seed)
        val mode = when (level) {
            1, 2 -> 0
            3 -> 1
            4 -> 2
            else -> r.nextInt(3)
        }
        val count = when (level) { 1 -> 3; 2, 3 -> 4; else -> 5 }.let { if (mode == 1) minOf(it, 4) else it }
        val lead = if (intro.isBlank()) "" else "$intro "
        return when (mode) {
            0 -> {
                val family = RHYMES.filter { it.size >= 2 }.random(r)
                val target = family.random(r)
                val answer = (family - target).random(r)
                val others = WORDS.filter { it !in family }.shuffled(r).take(count - 1)
                val options = (listOf(answer) + others).shuffled(r)
                PictureChallenge(
                    Skill.RHYMES, level, seed, Speech.of("${lead}Which one rhymes with $target?"), "rhyme", listOf(Shown("mini_rhyme_$target")),
                    options.map(::word), options.indexOf(answer), Speech.of("${target.cap()} and $answer rhyme. They end with the same sound."),
                )
            }
            1 -> {
                val family = RHYMES.filter { it.size >= 2 }.random(r)
                val pair = family.shuffled(r).take(2)
                val odd = WORDS.filter { it !in family }.random(r)
                val options = (pair + odd + WORDS.filter { it !in family && it != odd }.shuffled(r).take(0)).shuffled(r)
                PictureChallenge(
                    Skill.RHYMES, level, seed, Speech.of("${lead}Two of these rhyme. Which one does not?"), "rhyme", emptyList(),
                    options.map(::word), options.indexOf(odd),
                    Speech.of("${pair[0].cap()} and ${pair[1]} rhyme. The odd one is $odd."),
                )
            }
            else -> {
                val groups = WORDS.groupBy(::firstLetter).values.filter { it.size >= 2 }
                val group = groups.random(r)
                val target = group.random(r)
                val answer = (group - target).random(r)
                val others = WORDS.filter { firstLetter(it) != firstLetter(target) }.shuffled(r).take(count - 1)
                val options = (listOf(answer) + others).shuffled(r)
                PictureChallenge(
                    Skill.RHYMES, level, seed, Speech.of("${lead}Which one starts like $target?"), "firstSound", listOf(Shown("mini_rhyme_$target")),
                    options.map(::word), options.indexOf(answer),
                    Speech.of("${target.cap()} and $answer both start with the letter ${firstLetter(target).uppercaseChar()}."),
                )
            }
        }
    }

    private fun String.cap() = replaceFirstChar { it.uppercase() }

    private val COINS = listOf(5, 2, 1)

    /** [total] in coins of the given values, by chance: a mixed handful. */
    private fun handful(total: Int, values: List<Int>, r: Random): List<Shown> {
        var left = total
        val out = mutableListOf<Shown>()
        for (v in values.sortedDescending()) {
            val most = left / v
            val n = if (v == values.min()) most else r.nextInt(0, most + 1)
            if (n > 0) out += Shown("mini_coin_$v", n)
            left -= n * v
        }
        return out
    }

    fun money(level: Int, seed: Long, intro: String = ""): PictureChallenge {
        val r = Random(seed)
        val lead = if (intro.isBlank()) "" else "$intro "
        val (food, art) = FOODS.random(r)
        if (level >= 4) {
            val n = r.nextInt(4, 10)
            val m = r.nextInt(1, n)
            val answer = n - m
            val near = (0..9).filter { it != answer }.sortedBy { Math.abs(it - answer) * 10 + r.nextInt(10) }
            val options = (listOf(answer) + near.take(2)).shuffled(r)
            if (level == 4) {
                return PictureChallenge(
                    Skill.MONEY, level, seed,
                    Speech.of("${lead}You have ${Words.number(n)} coins. The $food costs ${Words.number(m)} coins. How many coins are left?"), "change",
                    listOf(Shown("mini_coin_1", n)), options.map(::numeral), options.indexOf(answer),
                    Speech.of("${Words.capital(n)} take away ${Words.number(m)} leaves ${Words.number(answer)}."),
                )
            }
            val (other, _) = FOODS.first { it.first != food }
            val a = r.nextInt(1, 5)
            val b = r.nextInt(1, 5)
            val sum = a + b
            val opts = (listOf(sum) + (1..9).filter { it != sum }.sortedBy { Math.abs(it - sum) * 10 + r.nextInt(10) }.take(2)).shuffled(r)
            return PictureChallenge(
                Skill.MONEY, level, seed,
                Speech.of("${lead}The $food costs ${Words.number(a)} coins. The $other costs ${Words.number(b)} coins. How many coins for both?"), "bothPrices",
                listOf(Shown(art), Shown(FOODS.first { it.first == other }.second)), opts.map(::numeral), opts.indexOf(sum),
                Speech.of("${Words.capital(a)} and ${Words.number(b)} make ${Words.number(sum)}."),
            )
        }
        val values = when (level) { 1 -> listOf(1); 2 -> listOf(2, 1); else -> COINS }
        val price = when (level) { 1 -> r.nextInt(1, 4); 2 -> r.nextInt(2, 6); else -> r.nextInt(3, 10) }
        val totals = (listOf(price) + listOf(price - 1, price + 1, price + 2, price - 2).filter { it >= 1 }.shuffled(r).take(2)).shuffled(r)
        val options = totals.map { t -> Card(handful(t, values, r), t.toString()) }
        return PictureChallenge(
            Skill.MONEY, level, seed, Speech.of("${lead}The $food costs ${Words.number(price)} coins. Which coins pay for it exactly?"), "pay",
            listOf(Shown(art)), options, totals.indexOf(price),
            Speech.of("The $food costs ${Words.number(price)}, so these coins make exactly ${Words.number(price)}."),
        )
    }

    fun share(level: Int, seed: Long, intro: String = ""): PictureChallenge {
        val r = Random(seed)
        val lead = if (intro.isBlank()) "" else "$intro "
        val bats = when (level) { 1, 2 -> 2; 3 -> 3; else -> r.nextInt(2, 5) }
        val each = when (level) { 1 -> r.nextInt(1, 3); 2 -> r.nextInt(2, 4); 3 -> r.nextInt(1, 4); else -> r.nextInt(2, 5) }.coerceAtMost(12 / bats)
        val total = bats * each
        val near = (1..6).filter { it != each }.sortedBy { Math.abs(it - each) * 10 + r.nextInt(10) }
        val options = (listOf(each) + near.take(if (level <= 2) 2 else 3)).shuffled(r)
        return PictureChallenge(
            Skill.SHARING, level, seed,
            Speech.of("${lead}${Words.capital(bats)} hungry bats share ${Words.number(total)} berries fairly. How many berries does each bat get?"), "share",
            listOf(Shown("mini_bat_small", bats), Shown("item_berry", total)), options.map(::numeral), options.indexOf(each),
            Speech.of("${Words.capital(total)} berries shared by ${Words.number(bats)} bats is ${Words.number(each)} each."),
        )
    }

    private fun said(m: Move, compass: Boolean) = "Go ${if (compass) m.way.compass else m.way.plain} ${Words.number(m.steps)} ${if (m.steps == 1) "step" else "steps"}."

    /** Hoot's treasure map: one to three moves on a grid; compass words from level 4. */
    fun map(level: Int, seed: Long, intro: String = ""): GridChallenge {
        val r = Random(seed)
        val (rows, cols) = when (level) { 1 -> 3 to 3; 2 -> 3 to 4; 3 -> 4 to 4; 4 -> 4 to 5; else -> 5 to 5 }
        val count = when (level) { 1, 2 -> 1; 3, 4 -> 2; else -> 3 }
        val compass = level >= 4
        while (true) {
            val start = r.nextInt(rows) to r.nextInt(cols)
            var row = start.first
            var col = start.second
            val moves = mutableListOf<Move>()
            repeat(count) {
                val options = Way.entries.flatMap { w -> (1..(if (level <= 2) 2 else 3)).map { Move(w, it) } }.filter { m ->
                    val nr = row + m.way.dRow * m.steps
                    val nc = col + m.way.dCol * m.steps
                    nr in 0 until rows && nc in 0 until cols && (moves.isEmpty() || moves.last().way != m.way)
                }
                if (options.isEmpty()) return@repeat
                val m = options.random(r)
                moves += m
                row += m.way.dRow * m.steps
                col += m.way.dCol * m.steps
            }
            if (moves.size != count || (row == start.first && col == start.second)) continue
            val lead = if (intro.isBlank()) "" else "$intro "
            val route = moves.map { said(it, compass) }
            return GridChallenge(
                level, seed, Speech.of("${lead}You start at the circle. ${route.joinToString(" ")} Where is the treasure?"),
                rows, cols, start.first, start.second, moves, compass,
                Speech.of("Start at the circle. ${route.joinToString(" ")} That is where the treasure is."),
            )
        }
    }

    /** A place for the recap: its name and the name of its picture (`art_scene_<art>`). */
    data class Spot(val name: String, val art: String)

    /**
     * Tell it back: remembering the trip, in order. [trail] is the places the hero went to, in the order they got there (camp first);
     * [all] is every place, for wrong answers. Level 1 asks for the first place after camp; 2 and 3 for the one after a place, 4 for the
     * one before a place, 5 either.
     */
    fun recall(level: Int, seed: Long, trail: List<Spot>, all: List<Spot>): PictureChallenge? {
        if (trail.size < 3) return null
        val r = Random(seed)
        val count = when (level) { 1, 2 -> 3; 3, 4 -> 4; else -> 5 }
        fun card(p: Spot) = Card(listOf(Shown("scene_${p.art}")), p.name)
        fun build(prompt: String, anchor: Spot?, answer: Spot, because: String): PictureChallenge {
            val others = (all - trail.toSet() + trail.filter { it != answer && it != anchor }).filter { it != answer && it != anchor }.shuffled(r).take(count - 1)
            val options = (listOf(answer) + others).shuffled(r)
            return PictureChallenge(
                Skill.STORY, level, seed, Speech.of(prompt), "recall", listOfNotNull(anchor?.let { Shown("scene_${it.art}") }),
                options.map(::card), options.indexOf(answer), Speech.of(because),
            )
        }
        if (level == 1) {
            val first = trail[1]
            return build("Do you remember your trip? Where did you go first, after the camp?", null, first, "${first.name} was the first place you went to.")
        }
        val before = level == 4 || (level == 5 && r.nextBoolean())
        return if (before) {
            val i = r.nextInt(2, trail.size)
            build("Where did you go before ${trail[i].name}?", trail[i], trail[i - 1], "You went to ${trail[i - 1].name}, and then to ${trail[i].name}.")
        } else {
            val i = r.nextInt(1, trail.size - 1)
            build("After ${trail[i].name}, where did you go next?", trail[i], trail[i + 1], "You went from ${trail[i].name} to ${trail[i + 1].name}.")
        }
    }
}
