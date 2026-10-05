package com.littledungeon.engine.rpg.run

/**
 * One thing the child did, and when. A journey is decided by its seed and the hero it starts with, so the
 * seed plus these commands, in order, rebuild it exactly (see [Journey.replay]). That is what lets the app
 * save after every tap and carry on after the phone was put down, closed or restarted.
 *
 * [at] is the clock's time of the tap: it is kept because "which skill was practiced longest ago" depends on it.
 */
data class Command(val reply: Reply, val at: Long)

/** One command as one line of text (`at|kind|fields`), for a log that is only ever added to. */
object CommandCodec {
    private const val SEP = '|'

    fun encode(c: Command): String = when (val r = c.reply) {
        Reply.Next -> "${c.at}${SEP}N"
        is Reply.Solved -> "${c.at}${SEP}S$SEP${r.tries}$SEP${r.hints}$SEP${r.millis}$SEP${if (r.failed) 1 else 0}$SEP${r.wrong.joinToString(",")}"
        is Reply.Bought -> "${c.at}${SEP}B$SEP${r.itemId}"
        is Reply.Rolled -> "${c.at}${SEP}R$SEP${if (r.usedReroll) 1 else 0}$SEP${r.sumTries}"
        is Reply.Picked -> "${c.at}${SEP}P$SEP${r.index}$SEP${r.tries}"
    }

    /** The command a line stands for, or null if it is not one (a half-written last line after the phone died, say). */
    fun decode(line: String): Command? = runCatching {
        val p = line.trim().split(SEP)
        val at = p[0].toLong()
        val reply = when (p[1]) {
            "N" -> Reply.Next
            "S" -> Reply.Solved(p[2].toInt(), p[3].toInt(), p[4].toLong(), p[5] == "1", p[6].split(",").filter { it.isNotEmpty() }.map { it.toInt() })
            "B" -> Reply.Bought(p[2])
            "R" -> Reply.Rolled(p[2] == "1", p[3].toInt())
            "P" -> Reply.Picked(p[2].toInt(), p[3].toInt())
            else -> return null
        }
        Command(reply, at)
    }.getOrNull()

    fun encodeAll(commands: List<Command>): String = commands.joinToString("") { encode(it) + "\n" }

    /** Every line that is a whole command, in order. A broken line ends the log: nothing after it can be trusted. */
    fun decodeAll(text: String): List<Command> {
        val out = mutableListOf<Command>()
        for (line in text.lineSequence()) {
            if (line.isBlank()) continue
            out += decode(line) ?: break
        }
        return out
    }
}
