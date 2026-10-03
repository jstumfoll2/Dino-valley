package com.dinovalley.feedback

import android.os.Build
import android.os.SystemClock
import com.dinovalley.engine.model.Voice
import com.dinovalley.engine.rpg.run.Beat

/**
 * What the game remembers so a grown-up's note can be pinned to the exact moment: the screen
 * showing now, the adventure's seed and every answer so far (an adventure can be replayed from
 * those), and the last things said and done. Nothing leaves the phone until someone taps send.
 */
object FeedbackLog {
    private const val KEEP = 120
    private val events = ArrayDeque<String>()
    private val started = SystemClock.elapsedRealtime()

    /** The beat on screen, described for the report. */
    @Volatile
    var screen: String = "title screen"

    /** Seed, hero and skills of the adventure being played, and the replies given so far. */
    @Volatile
    var adventure: String = "no adventure yet"

    private val replies = mutableListOf<String>()

    @Synchronized
    fun note(kind: String, text: String) {
        val t = (SystemClock.elapsedRealtime() - started) / 1000.0
        events += "+%.1fs %s: %s".format(t, kind, text.take(160))
        while (events.size > KEEP) events.removeFirst()
    }

    @Synchronized
    fun newAdventure(description: String) {
        adventure = description
        replies.clear()
        note("adventure", description)
    }

    @Synchronized
    fun reply(text: String) {
        replies += text
        note("reply", text)
    }

    @Synchronized
    private fun recent(n: Int): List<String> = events.toList().takeLast(n)

    @Synchronized
    private fun replyList(): String = replies.joinToString(", ").ifEmpty { "none yet" }

    /** The whole report, to share or paste. */
    fun report(note: String, tags: Collection<String>, version: String): String = buildString {
        appendLine("## Playtest note")
        appendLine(note.ifBlank { "(no words, just the moment below)" })
        if (tags.isNotEmpty()) appendLine("\nWhat kind: ${tags.joinToString(", ")}")
        appendLine("\n## Where it happened")
        appendLine(screen)
        appendLine("\n## Adventure (replayable)")
        appendLine(adventure)
        appendLine("Replies so far: ${replyList()}")
        appendLine("\n## Last things heard and done")
        recent(60).forEach { appendLine("- $it") }
        appendLine("\n## Phone")
        appendLine("App $version, ${Build.MANUFACTURER} ${Build.MODEL}, Android ${Build.VERSION.RELEASE}")
    }

    /** A short version that fits in a web address. */
    fun summary(note: String, tags: Collection<String>): String = buildString {
        appendLine(note.ifBlank { "(no words)" })
        if (tags.isNotEmpty()) appendLine("\nKind: ${tags.joinToString(", ")}")
        appendLine("\nWhere: $screen")
        appendLine("Adventure: $adventure")
        appendLine("\nLast:")
        recent(8).forEach { appendLine("- $it") }
        appendLine("\n(The full report is on the clipboard: paste it below.)")
    }

    /** What a beat looks like in the report. */
    fun describe(beat: Beat): String = when (beat) {
        is Beat.Tell -> "Story at ${beat.scene.place}, cast ${beat.scene.cast}: ${Voice.caption(beat.lines)}"
        is Beat.Found -> "Found ${beat.loot.kind} (${beat.loot.words}, color ${beat.loot.hue}): ${Voice.caption(beat.lines)}"
        is Beat.Ask -> "Challenge ${beat.challenge::class.simpleName} level ${beat.challenge.level} seed ${beat.challenge.seed} at ${beat.scene.place}: ${Voice.caption(beat.challenge.prompt)}"
        is Beat.Roll -> "Dice ${beat.dice}, reroll ${beat.reroll}, at ${beat.scene.place}: ${Voice.caption(beat.why)}"
        is Beat.Choose -> "Choice at ${beat.scene.place}: ${beat.options.map { it.said }}"
        is Beat.Doors -> "Doors at stop ${beat.stopIndex}: ${beat.fork.doors.map { "${it.hue.word} ${it.kind}" }}, closed ${beat.closed}, clue answer ${beat.clue?.answer}"
        is Beat.Travel -> "Map at ${beat.here}: ${beat.routes.map { "${it.name} (${it.terrain.word})" }}"
        is Beat.Shop -> "Shop ${beat.shopName}, ${beat.coins} coins: ${beat.stock.map { it.name }}"
        is Beat.Finale -> "Finale"
    }
}
