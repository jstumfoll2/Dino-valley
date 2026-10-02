package com.dinovalley.engine.model

/**
 * A piece of narration: spoken words, the baby dragon's name, or a sound effect played in
 * place of words like "blub, blub" or "creak".
 */
sealed interface Speech {
    data class Words(val text: String) : Speech
    data object Name : Speech
    data class Sound(val id: String) : Speech

    companion object {
        /**
         * "Thank you, {name}!" becomes words, the name, then words again. "[creak]" is the
         * creak sound effect.
         */
        fun of(template: String): List<Speech> {
            val out = mutableListOf<Speech>()
            var at = 0
            for (m in TOKEN.findAll(template)) {
                template.substring(at, m.range.first).takeIf { it.isNotBlank() }?.let { out += Words(it.trim()) }
                out += if (m.value == NAME) Name else Sound(m.groupValues[1])
                at = m.range.last + 1
            }
            template.substring(at).takeIf { it.isNotBlank() }?.let { out += Words(it.trim()) }
            return out
        }

        private const val NAME = "{name}"
        private val TOKEN = Regex("\\{name\\}|\\[([a-z]+)\\]")
    }
}
