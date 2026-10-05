package com.littledungeon.engine.rpg

import com.littledungeon.engine.rpg.hero.Hero
import com.littledungeon.engine.rpg.hero.HeroClass
import com.littledungeon.engine.rpg.learn.SkillBook
import com.littledungeon.engine.rpg.run.Beat
import com.littledungeon.engine.rpg.run.Command
import com.littledungeon.engine.rpg.run.CommandCodec
import com.littledungeon.engine.rpg.run.Journey
import com.littledungeon.engine.rpg.run.Reply
import com.littledungeon.engine.rpg.world.WorldMemory
import com.littledungeon.engine.util.Clock
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * A journey is its seed plus what the child did, so the app can save after every tap and carry on from a
 * phone that was put down. These tests hold that: the log survives being written and read, and a replay
 * lands on exactly the same journey, beat for beat.
 */
class CommandLogTest {
    private val start = Hero(HeroClass.KNIGHT)

    private fun fresh(seed: Long, clock: Clock = Clock { 0L }) = Journey(seed, start, SkillBook(), WorldMemory(), clock)

    /** A pretend child: gets most puzzles right, mostly follows the marked road, buys now and then. */
    private fun answer(b: Beat, r: Random): Reply = when (b) {
        is Beat.Tell, is Beat.Found, is Beat.Night, is Beat.Finale -> Reply.Next
        is Beat.Ask -> if (b.oneTry && r.nextInt(5) == 0) Reply.Solved(1, 0, 4000, failed = true, wrong = listOf(0, 2)) else Reply.Solved(r.nextInt(1, 3), r.nextInt(2), 4000)
        is Beat.Choose -> Reply.Picked(r.nextInt(b.options.size))
        // Follows the baby dragon's marked road most of the time, like a child does; a pure random walk can take forever.
        is Beat.Travel -> Reply.Picked(b.routes.indexOfFirst { it.marked && !it.blocked }.takeIf { it >= 0 && r.nextInt(10) < 6 } ?: b.routes.indices.filter { !b.routes[it].blocked }.random(r))
        is Beat.Shop -> b.stock.filter { it.canAfford }.takeIf { it.isNotEmpty() && r.nextInt(3) == 0 }?.random(r)?.let { Reply.Bought(it.itemId) } ?: Reply.Next
        is Beat.Roll -> Reply.Rolled(r.nextBoolean(), r.nextInt(1, 3))
    }

    private fun playOut(j: Journey, r: Random, stopAfter: Int = Int.MAX_VALUE) {
        var n = 0
        while (!j.finished && n++ < stopAfter) j.reply(answer(j.beat, r))
        assertTrue(n < 5000, "journey never ended")
    }

    @Test
    fun `every kind of reply survives being written and read`() {
        val all = listOf(
            Reply.Next, Reply.Solved(2, 1, 4321), Reply.Solved(1, 0, 9, failed = true, wrong = listOf(0, 2)),
            Reply.Bought("cookie"), Reply.Rolled(true, 2), Reply.Picked(3), Reply.Picked(1, 4),
        ).mapIndexed { i, r -> Command(r, 1_000_000L + i) }
        assertEquals(all, CommandCodec.decodeAll(CommandCodec.encodeAll(all)))
    }

    @Test
    fun `a half-written last line is dropped and nothing after a broken line is trusted`() {
        val good = listOf(Command(Reply.Next, 5), Command(Reply.Picked(2), 6))
        val text = CommandCodec.encodeAll(good)
        assertEquals(good, CommandCodec.decodeAll(text + "7|S|2|"), "a line cut short by the phone dying is ignored")
        assertEquals(good, CommandCodec.decodeAll(text + "garbage\n8|N\n"), "after a broken line the rest is not replayed")
        assertNull(CommandCodec.decode(""))
        assertNull(CommandCodec.decode("1|X|2"))
    }

    @Test
    fun `replaying the commands rebuilds the journey beat for beat`() {
        for (seed in listOf(11L, 2024L, 987654321L)) {
            var now = 1_000L
            val live = fresh(seed) { now += 7_000; now }
            val r = Random(seed)
            val beats = mutableListOf(live.beat)
            while (!live.finished) {
                live.reply(answer(live.beat, r))
                beats += live.beat
            }
            val again = Journey.replay(seed, start, SkillBook(), WorldMemory(), emptyList())
            assertEquals(beats.first(), again.beat)
            for ((i, c) in live.commands.withIndex()) {
                again.play(c)
                assertEquals(beats[i + 1], again.beat, "seed $seed: the beat after command $i differs")
            }
            assertTrue(again.finished)
            assertEquals(live.hero, again.hero)
            assertEquals(live.skills, again.skills)
            assertEquals(live.world, again.world)
            assertEquals(live.records, again.records)
            assertEquals(live.hp, again.hp)
            assertEquals(live.day, again.day)
        }
    }

    @Test
    fun `a journey put down in the middle carries on to the same ending`() {
        val seed = 31L
        fun clock(): Clock { var now = 5_000L; return Clock { now += 9_000; now } }
        val whole = fresh(seed, clock())
        playOut(whole, Random(1))

        // The first half is played and saved as text; a fresh start reads it back, and the same child goes on.
        val r = Random(1)
        val theClock = clock()
        val firstHalf = fresh(seed, theClock)
        repeat(whole.commands.size / 2) { firstHalf.reply(answer(firstHalf.beat, r)) }
        val resumed = Journey.replay(seed, start, SkillBook(), WorldMemory(), CommandCodec.decodeAll(CommandCodec.encodeAll(firstHalf.commands)), theClock)
        assertEquals(firstHalf.beat, resumed.beat)
        while (!resumed.finished) resumed.reply(answer(resumed.beat, r))
        assertEquals(whole.world, resumed.world)
        assertEquals(whole.hero, resumed.hero)
        assertEquals(whole.skills, resumed.skills)
    }

    @Test
    fun `the party camps for the night, rested, and a journey takes more than one day`() {
        var nights = 0
        for (seed in 1L..8L) {
            val j = fresh(seed)
            val r = Random(seed)
            var lastDay = j.day
            var guard = 0
            while (!j.finished && guard++ < 5000) {
                val b = j.beat
                if (b is Beat.Night) {
                    nights++
                    assertEquals(lastDay, b.day, "the night names the day that is ending")
                    j.reply(Reply.Next)
                    assertEquals(j.hero.maxHp, j.hp, "a night's rest heals")
                    assertEquals(lastDay + 1, j.day)
                    lastDay = j.day
                } else {
                    j.reply(answer(b, r))
                }
            }
            assertTrue(j.finished)
        }
        assertTrue(nights >= 8, "a journey of ordinary length has at least a night in it ($nights over 8 journeys)")
    }

    @Test
    fun `night never falls on the way into the last fight`() {
        for (seed in 1L..8L) {
            val j = fresh(seed)
            val r = Random(seed)
            var previous: Beat? = null
            while (!j.finished) {
                val b = j.beat
                if (previous is Beat.Night) assertTrue(b !is Beat.Finale, "the finale does not follow a night")
                previous = b
                j.reply(answer(b, r))
            }
        }
    }
}
