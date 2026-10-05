package com.littledungeon.data

import android.content.Context
import com.littledungeon.engine.rpg.hero.Attribute
import com.littledungeon.engine.rpg.hero.Hero
import com.littledungeon.engine.rpg.hero.HeroClass
import com.littledungeon.engine.rpg.items.Slot
import com.littledungeon.engine.rpg.run.Command
import com.littledungeon.engine.rpg.run.CommandCodec
import com.littledungeon.engine.rpg.learn.ChallengeRecord
import com.littledungeon.engine.rpg.learn.Skill
import com.littledungeon.engine.rpg.learn.SkillBook
import com.littledungeon.engine.rpg.world.WorldMemory
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Everything the game keeps, stored only on this phone (no network permission exists).
 *
 * Between adventures: the hero, what they have learned and what the world remembers (`save.json`). During
 * one: where it started (`journey.json`: its seed and the starting state) and every tap since, one line each
 * (`journey.log`). The seed and the taps rebuild the adventure exactly, so a tap is saved the moment it is made
 * and nothing is lost if the phone is put down, or dies. The challenge log is one JSON object per line, ready
 * for a future on-device model to learn from; each puzzle is added the moment it is answered.
 */
class Save(context: Context) {
    private val file = File(context.filesDir, "save.json")
    private val log = File(context.filesDir, "challenges.jsonl")
    private val journeyFile = File(context.filesDir, "journey.json")
    private val commandFile = File(context.filesDir, "journey.log")

    data class State(val hero: Hero, val skills: SkillBook, val world: WorldMemory)

    /** An adventure that was put down: its seed, the state it started from, and what the child did in it. */
    class Saved(val seed: Long, val start: State, val commands: List<Command>)

    fun load(): State = runCatching { stateFrom(JSONObject(file.readText())) }.getOrDefault(State(Hero(), SkillBook(), WorldMemory()))

    fun store(state: State) = write(file, stateJson(state))

    // ------------------------------------------------------------- the adventure in progress

    /** Starts keeping an adventure: its seed and where the hero stands as it begins. Any earlier one is gone. */
    fun begin(seed: Long, start: State) {
        end()
        write(journeyFile, stateJson(start).put("seed", seed))
    }

    /** Keeps one tap. A line cut short by the phone dying is ignored when the adventure is read back. */
    fun append(command: Command) {
        commandFile.appendText(CommandCodec.encode(command) + "\n")
    }

    fun hasSaved(): Boolean = journeyFile.exists()

    fun saved(): Saved? {
        val json = runCatching { JSONObject(journeyFile.readText()) }.getOrNull() ?: return null
        return runCatching {
            val text = if (commandFile.exists()) commandFile.readText() else ""
            val commands = CommandCodec.decodeAll(text)
            // A line cut short by the phone dying is cut off the file, so the next tap starts a line of its own instead of
            // running on from the broken one (which would make every tap after it unreadable).
            val whole = CommandCodec.encodeAll(commands)
            if (whole != text) commandFile.writeText(whole)
            Saved(json.getLong("seed"), stateFrom(json), commands)
        }.getOrNull()
    }

    /** The adventure is over (or let go): there is nothing to carry on from. */
    fun end() {
        journeyFile.delete()
        commandFile.delete()
    }

    /** Adds puzzles to the challenge log as they are answered. */
    fun logRecords(records: List<ChallengeRecord>) {
        if (records.isNotEmpty()) log.appendText(records.joinToString("") { record(it).toString() + "\n" })
    }

    // ------------------------------------------------------------- the whole state

    private fun stateJson(s: State) = JSONObject()
        .put("version", 1)
        .put("hero", hero(s.hero))
        .put("skills", skills(s.skills))
        .put("world", world(s.world))

    private fun stateFrom(json: JSONObject) = State(hero(json.getJSONObject("hero")), skills(json.getJSONObject("skills")), world(json.getJSONObject("world")))

    /** Written beside the file and moved into place, so a crash never leaves half a file. */
    private fun write(target: File, json: JSONObject) {
        val tmp = File(target.path + ".tmp")
        tmp.writeText(json.toString())
        tmp.renameTo(target)
    }

    // ------------------------------------------------------------- hero

    private fun hero(h: Hero) = JSONObject()
        .put("class", h.heroClass.name)
        .put("xp", JSONObject().also { o -> h.xp.forEach { (k, v) -> o.put(k.name, v) } })
        .put("coins", h.coins)
        .put("bag", JSONObject().also { o -> h.bag.forEach { (k, v) -> o.put(k, v) } })
        .put("worn", JSONObject().also { o -> h.worn.forEach { (k, v) -> o.put(k.name, v) } })

    private fun hero(o: JSONObject) = Hero(
        heroClass = enumOrNull<HeroClass>(o.optString("class")) ?: HeroClass.KNIGHT,
        xp = ints<Attribute>(o.optJSONObject("xp")),
        coins = o.optInt("coins"),
        bag = o.optJSONObject("bag")?.let { x -> x.keys().asSequence().associateWith { x.getInt(it) } } ?: emptyMap(),
        worn = o.optJSONObject("worn")?.let { x ->
            x.keys().asSequence().mapNotNull { k -> enumOrNull<Slot>(k)?.let { it to x.getString(k) } }.toMap()
        } ?: emptyMap(),
    )

    // ------------------------------------------------------------- skills

    private fun skills(s: SkillBook) = JSONObject()
        .put("levels", JSONObject().also { o -> s.levels.forEach { (k, v) -> o.put(k.name, v) } })
        .put("streaks", JSONObject().also { o -> s.streaks.forEach { (k, v) -> o.put(k.name, v) } })
        .put("last", JSONObject().also { o -> s.lastPracticed.forEach { (k, v) -> o.put(k.name, v) } })
        .put("recent", JSONObject().also { o -> s.recent.forEach { (k, v) -> o.put(k.name, v.joinToString("") { if (it) "1" else "0" }) } })
        .put("misses", JSONObject().also { o -> s.misses.forEach { (k, v) -> o.put(k.name, v) } })

    private fun skills(o: JSONObject) = SkillBook(
        levels = ints<Skill>(o.optJSONObject("levels")),
        streaks = ints<Skill>(o.optJSONObject("streaks")),
        lastPracticed = o.optJSONObject("last")?.let { x ->
            x.keys().asSequence().mapNotNull { k -> enumOrNull<Skill>(k)?.let { it to x.getLong(k) } }.toMap()
        } ?: emptyMap(),
        recent = o.optJSONObject("recent")?.let { x ->
            x.keys().asSequence().mapNotNull { k -> enumOrNull<Skill>(k)?.let { it to x.getString(k).map { c -> c == '1' } } }.toMap()
        } ?: emptyMap(),
        misses = ints<Skill>(o.optJSONObject("misses")),
    )

    // ------------------------------------------------------------- world

    private fun world(w: WorldMemory) = JSONObject()
        .put("adventures", w.adventures)
        .put("endings", JSONArray(w.endings.toList()))
        .put("flags", JSONArray(w.flags.toList()))
        .put("relations", JSONObject().also { o -> w.relations.forEach { (k, v) -> o.put(k, v) } })
        .put("pages", w.pages)
        .put("arcsDone", JSONObject().also { o -> w.arcsDone.forEach { (k, v) -> o.put(k, v) } })
        .put("lastArc", w.lastArc ?: "")

    private fun world(o: JSONObject) = WorldMemory(
        adventures = o.optInt("adventures"),
        endings = strings(o.optJSONArray("endings")).toSet(),
        flags = strings(o.optJSONArray("flags")).toSet(),
        relations = o.optJSONObject("relations")?.let { x -> x.keys().asSequence().associateWith { x.getInt(it) } } ?: emptyMap(),
        pages = o.optInt("pages"),
        arcsDone = o.optJSONObject("arcsDone")?.let { x -> x.keys().asSequence().associateWith { x.getInt(it) } } ?: emptyMap(),
        lastArc = o.optString("lastArc").ifEmpty { null },
    )

    private fun strings(a: JSONArray?): List<String> = a?.let { (0 until it.length()).map { i -> it.getString(i) } } ?: emptyList()

    private fun record(r: ChallengeRecord) = JSONObject()
        .put("skill", r.skill.name).put("kind", r.kind).put("level", r.level).put("tries", r.tries)
        .put("hints", r.hintsUsed).put("millis", r.millis).put("at", r.atMillis).put("seed", r.seed).put("failed", r.failed)

    private inline fun <reified E : Enum<E>> ints(o: JSONObject?): Map<E, Int> =
        o?.let { x -> x.keys().asSequence().mapNotNull { k -> enumOrNull<E>(k)?.let { it to x.getInt(k) } }.toMap() } ?: emptyMap()

    private inline fun <reified E : Enum<E>> enumOrNull(name: String): E? = enumValues<E>().firstOrNull { it.name == name }
}
