package com.littledungeon.engine.model

/**
 * Who is talking. The narrator tells the story; the others are characters on screen who answer
 * each other. Each has a voice (a Kokoro speaker, a speed, and a pitch change) so they sound
 * different, and only the character who is actually speaking moves their mouth.
 *
 * [voiceId] names the exact voice settings and is part of every recording's fingerprint, so
 * changing a voice records its lines again. The narrator's must never change by accident.
 */
enum class Who(val tag: String, val sid: Int, val speed: Float, val pitch: Float, val voiceId: String) {
    // The story and the companion.
    NARRATOR("narrator", 3, 0.9f, 1f, Voice.VOICE_ID),
    PET("pet", 7, 0.92f, 1f, v(7, 0.92f, 1f)),

    // Named people of the kingdom: each has a Kokoro v1.0 speaker of their own (a test keeps it so), and no pitch change.
    HOOT("hoot", 25, 0.88f, 1f, v(25, 0.88f, 1f)),
    BARON("baron", 26, 0.85f, 1f, v(26, 0.85f, 1f)),
    MERLO("merlo", 19, 0.9f, 1f, v(19, 0.9f, 1f)),
    HAZEL("hazel", 21, 0.9f, 1f, v(21, 0.9f, 1f)),
    WILLOW("willow", 20, 0.88f, 1f, v(20, 0.88f, 1f)),
    TILLY("tilly", 0, 0.95f, 1f, v(0, 0.95f, 1f)),
    BUN("bun", 2, 1.0f, 1f, v(2, 1.0f, 1f)),
    BROGAN("brogan", 11, 0.95f, 1f, v(11, 0.95f, 1f)),
    ZIG("zig", 16, 1.0f, 1f, v(16, 1.0f, 1f)),
    RASCAL("rascal", 15, 1.05f, 1f, v(15, 1.05f, 1f)),
    BESS("bess", 5, 0.95f, 1f, v(5, 0.95f, 1f)),
    LUMI("lumi", 4, 0.95f, 1f, v(4, 0.95f, 1f)),
    RIBBIT("ribbit", 18, 1.0f, 1f, v(18, 1.0f, 1f)),
    GRUMBLE("grumble", 17, 0.85f, 1f, v(17, 0.85f, 1f)),
    HOB("hob", 13, 0.95f, 1f, v(13, 0.95f, 1f)),
    FINN("finn", 12, 0.95f, 1f, v(12, 0.95f, 1f)),
    FERN("fern", 1, 1.0f, 1f, v(1, 1.0f, 1f)),
    HENRIETTA("henrietta", 6, 0.95f, 1f, v(6, 0.95f, 1f)),
    OTTO("otto", 27, 1.0f, 1f, v(27, 1.0f, 1f)),

    // Big characters and kinds of creature. These share a speaker with someone else but differ in pitch and pace.
    DRAGON("dragon", 24, 0.85f, 0.8f, v(24, 0.85f, 0.8f)),
    SHADOW("shadow", 22, 0.85f, 0.8f, v(22, 0.85f, 0.8f)),
    SPOOK("spook", 23, 0.85f, 0.9f, v(23, 0.85f, 0.9f)),
    CRITTER("critter", 9, 1.0f, 1.1f, v(9, 1.0f, 1.1f)),
    GROWLER("growler", 14, 0.9f, 0.85f, v(14, 0.9f, 0.85f)),

    // Kept for stories to come and the first dungeon's cast; nobody in a journey speaks with these yet.
    WIZARD("wizard", 24, 0.9f, 1f, v(24, 0.9f, 1f)),
    GOBLIN("goblin", 10, 1.0f, 1.15f, v(10, 1.0f, 1.15f)),
    RUBY("ruby", 8, 0.95f, 1f, v(8, 0.95f, 1f)),
    SNEAK("sneak", 8, 1.0f, 1.1f, v(8, 1.0f, 1.1f)),
    ELDER("elder", 25, 0.9f, 1.08f, v(25, 0.9f, 1.08f)),
    GRANNY("granny", 20, 0.9f, 0.95f, v(20, 0.9f, 0.95f)),
    MERCHANT("merchant", 16, 1.0f, 1.08f, v(16, 1.0f, 1.08f)),
    TOWNSWOMAN("townswoman", 0, 0.95f, 1.08f, v(0, 0.95f, 1.08f)),
    TOWNSMAN("townsman", 11, 0.95f, 1.08f, v(11, 0.95f, 1.08f)),
    GUARD("guard", 13, 0.95f, 0.92f, v(13, 0.95f, 0.92f)),
    CHILD("child", 4, 1.0f, 1.1f, v(4, 1.0f, 1.1f)),
    ;

    companion object {
        /** The people who each have a speaker to themselves. */
        val NAMED = listOf(
            NARRATOR, PET, HOOT, BARON, MERLO, HAZEL, WILLOW, TILLY, BUN, BROGAN, ZIG, RASCAL, BESS, LUMI, RIBBIT, GRUMBLE, HOB, FINN, FERN, HENRIETTA, OTTO,
        )

        fun ofTag(tag: String): Who = entries.first { it.tag == tag }
    }
}

/**
 * A piece of narration: spoken words, the baby dragon's name, a sound effect played in place of
 * words like "blub, blub" or "creak", or a change of speaker.
 */
sealed interface Speech {
    data class Words(val text: String) : Speech
    data object Name : Speech
    data class Sound(val id: String) : Speech

    /** The words after this are said by [who], until the next change. */
    data class As(val who: Who) : Speech

    companion object {
        /**
         * "Thank you, {name}!" becomes words, the name, then words again. "[creak]" is the
         * creak sound effect. "<pet>Hi!<narrator>He waves." changes who speaks; a line starts
         * with the narrator.
         */
        fun of(template: String): List<Speech> {
            val out = mutableListOf<Speech>()
            var who = Who.NARRATOR
            var at = 0
            for (m in TOKEN.findAll(template)) {
                template.substring(at, m.range.first).takeIf { it.isNotBlank() }?.let { out += Words(it.trim()) }
                out += when {
                    m.value == NAME -> Name
                    m.value.startsWith("<") -> As(Who.ofTag(m.groupValues[2]).also { who = it })
                    else -> Sound(m.groupValues[1])
                }
                at = m.range.last + 1
            }
            template.substring(at).takeIf { it.isNotBlank() }?.let { out += Words(it.trim()) }
            // A line never leaves its speaker talking into the next one.
            if (who != Who.NARRATOR) out += As(Who.NARRATOR)
            return out
        }

        private const val NAME = "{name}"
        private val TOKEN = Regex("\\{name\\}|\\[([a-z]+)\\]|<([a-z]+)>")
    }
}

/** The id of a Kokoro v1.0 voice setting: part of every recording's fingerprint. */
private fun v(sid: Int, speed: Float, pitch: Float) = "kokoro-multi-lang-v1_0/sid$sid/speed$speed/pitch$pitch"
