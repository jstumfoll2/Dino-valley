package com.dinovalley.data

import android.content.Context
import com.dinovalley.engine.rpg.hero.Attribute
import com.dinovalley.engine.rpg.hero.Hero
import com.dinovalley.engine.rpg.hero.HeroClass
import com.dinovalley.engine.rpg.items.Slot
import com.dinovalley.engine.rpg.learn.ChallengeRecord
import com.dinovalley.engine.rpg.learn.Skill
import com.dinovalley.engine.rpg.learn.SkillBook
import com.dinovalley.engine.rpg.world.QuestKind
import com.dinovalley.engine.rpg.world.WorldMemory
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Everything the game keeps between adventures, stored only on this phone (no network
 * permission exists). The challenge log is one JSON object per line, ready for a future
 * on-device model to learn from.
 */
class Save(context: Context) {
    private val file = File(context.filesDir, "save.json")
    private val log = File(context.filesDir, "challenges.jsonl")

    data class State(val hero: Hero, val skills: SkillBook, val world: WorldMemory)

    fun load(): State {
        val fresh = State(Hero(), SkillBook(), WorldMemory())
        val json = runCatching { JSONObject(file.readText()) }.getOrNull() ?: return fresh
        return runCatching { State(hero(json.getJSONObject("hero")), skills(json.getJSONObject("skills")), world(json.getJSONObject("world"))) }
            .getOrDefault(fresh)
    }

    fun store(state: State, records: List<ChallengeRecord> = emptyList()) {
        val json = JSONObject()
            .put("version", 1)
            .put("hero", hero(state.hero))
            .put("skills", skills(state.skills))
            .put("world", world(state.world))
        val tmp = File(file.path + ".tmp")
        tmp.writeText(json.toString())
        tmp.renameTo(file)
        if (records.isNotEmpty()) {
            log.appendText(records.joinToString("") { record(it).toString() + "\n" })
        }
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

    private fun skills(o: JSONObject) = SkillBook(
        levels = ints<Skill>(o.optJSONObject("levels")),
        streaks = ints<Skill>(o.optJSONObject("streaks")),
        lastPracticed = o.optJSONObject("last")?.let { x ->
            x.keys().asSequence().mapNotNull { k -> enumOrNull<Skill>(k)?.let { it to x.getLong(k) } }.toMap()
        } ?: emptyMap(),
    )

    // ------------------------------------------------------------- world

    private fun world(w: WorldMemory) = JSONObject()
        .put("adventures", w.adventures)
        .put("friends", JSONArray(w.friends.toList()))
        .put("dragonFriend", w.dragonFriend ?: "")
        .put("endings", JSONArray(w.endings.toList()))
        .put("treasures", JSONArray(w.treasures))
        .put("questsDone", JSONObject().also { o -> w.questsDone.forEach { (k, v) -> o.put(k.name, v) } })
        .put("lastQuest", w.lastQuest?.name ?: "")
        .put("flags", JSONArray(w.flags.toList()))
        .put("relations", JSONObject().also { o -> w.relations.forEach { (k, v) -> o.put(k, v) } })
        .put("pages", w.pages)
        .put("arcsDone", JSONObject().also { o -> w.arcsDone.forEach { (k, v) -> o.put(k, v) } })
        .put("lastArc", w.lastArc ?: "")

    private fun world(o: JSONObject) = WorldMemory(
        adventures = o.optInt("adventures"),
        friends = strings(o.optJSONArray("friends")).toSet(),
        dragonFriend = o.optString("dragonFriend").ifEmpty { null },
        endings = strings(o.optJSONArray("endings")).toSet(),
        treasures = strings(o.optJSONArray("treasures")),
        questsDone = ints<QuestKind>(o.optJSONObject("questsDone")),
        lastQuest = enumOrNull<QuestKind>(o.optString("lastQuest")),
        flags = strings(o.optJSONArray("flags")).toSet(),
        relations = o.optJSONObject("relations")?.let { x -> x.keys().asSequence().associateWith { x.getInt(it) } } ?: emptyMap(),
        pages = o.optInt("pages"),
        arcsDone = o.optJSONObject("arcsDone")?.let { x -> x.keys().asSequence().associateWith { x.getInt(it) } } ?: emptyMap(),
        lastArc = o.optString("lastArc").ifEmpty { null },
    )

    private fun strings(a: JSONArray?): List<String> = a?.let { (0 until it.length()).map { i -> it.getString(i) } } ?: emptyList()

    private fun record(r: ChallengeRecord) = JSONObject()
        .put("skill", r.skill.name).put("kind", r.kind).put("level", r.level).put("tries", r.tries)
        .put("hints", r.hintsUsed).put("millis", r.millis).put("at", r.atMillis).put("seed", r.seed)

    private inline fun <reified E : Enum<E>> ints(o: JSONObject?): Map<E, Int> =
        o?.let { x -> x.keys().asSequence().mapNotNull { k -> enumOrNull<E>(k)?.let { it to x.getInt(k) } }.toMap() } ?: emptyMap()

    private inline fun <reified E : Enum<E>> enumOrNull(name: String): E? = enumValues<E>().firstOrNull { it.name == name }
}
