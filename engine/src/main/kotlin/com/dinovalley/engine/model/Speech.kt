package com.dinovalley.engine.model

/** A piece of narration: spoken words, or the dino's name (the child's own recording, or "Rex"). */
sealed interface Speech {
    data class Words(val text: String) : Speech
    data object Name : Speech

    companion object {
        /** "Thank you, {name}!" becomes words, the name, then words again. */
        fun of(template: String): List<Speech> =
            template.split(NAME).flatMapIndexed { i, part ->
                listOfNotNull(if (i > 0) Name else null, part.takeIf { it.isNotBlank() }?.let { Words(it.trim()) })
            }

        private const val NAME = "{name}"
    }
}
