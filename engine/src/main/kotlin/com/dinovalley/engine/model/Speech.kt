package com.dinovalley.engine.model

/**
 * Who is talking. The narrator tells the story; the others are characters on screen who answer
 * each other. Each has a voice (a Kokoro speaker, a speed, and a pitch change) so they sound
 * different, and only the character who is actually speaking moves their mouth.
 *
 * [voiceId] names the exact voice settings and is part of every recording's fingerprint, so
 * changing a voice records its lines again. The narrator's must never change by accident.
 */
enum class Who(val tag: String, val sid: Int, val speed: Float, val pitch: Float, val voiceId: String) {
    NARRATOR("narrator", 1, 0.9f, 1f, Voice.VOICE_ID),
    PET("pet", 8, 0.88f, 1.0f, "kokoro-en-v0_19/sid8/speed0.88/pitch1.0"),
    WIZARD("wizard", 3, 0.9f, 0.92f, "kokoro-en-v0_19/sid3/speed0.9/pitch0.92"),
    GOBLIN("goblin", 5, 1.0f, 1.15f, "kokoro-en-v0_19/sid5/speed1.0/pitch1.15"),
    RUBY("ruby", 7, 0.95f, 1f, "kokoro-en-v0_19/sid7/speed0.95/pitch1.0"),
    DRAGON("dragon", 10, 0.9f, 0.85f, "kokoro-en-v0_19/sid10/speed0.9/pitch0.85"),
    SHADOW("shadow", 6, 0.9f, 0.8f, "kokoro-en-v0_19/sid6/speed0.9/pitch0.8"),
    ;

    companion object {
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
