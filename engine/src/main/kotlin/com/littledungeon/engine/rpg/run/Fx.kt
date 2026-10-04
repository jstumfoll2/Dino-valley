package com.littledungeon.engine.rpg.run

/**
 * Magic you can see. When the story says something magical happens, the screen shows it:
 * the app plays an effect when the sentence (or story sound) starts. Kept here, in the engine,
 * so what counts as magic is tested and the words and the pictures can't drift apart.
 */
enum class Fx {
    /** A glittering burst. */
    SPARKLE,

    /** A bolt of light from the hero's wand, ending in a burst. */
    SPELL,

    /** A small electric zap. */
    ZAP,

    /** A rainbow arcs across the scene. */
    RAINBOW,

    /** A puff of smoke. */
    POOF,

    /** Sputtering smoke and a few sad sparks. */
    FIZZ,

    /** Bubbles float up. */
    BUBBLES,

    /** A warm glow around everyone. */
    GLOW,

    /** The wizard's hat turns into a bunny. */
    BUNNY,

    /** A frog hops out. */
    FROG,
    ;

    companion object {
        /** The effect to show while [sentence] is said, if it describes something magical. */
        fun forSentence(sentence: String): Fx? {
            val t = sentence.lowercase()
            return when {
                "bunny" in t -> BUNNY
                "frog" in t && "jumps out" in t -> FROG
                "rainbow" in t -> RAINBOW
                Regex("\\bcast\\b|\\byour spell\\b|\\bsparkly spell\\b").containsMatchIn(t) -> SPELL
                "fizz" in t -> FIZZ
                "poof" in t -> POOF
                "bubbl" in t -> BUBBLES
                "glow" in t || "shine like a star" in t -> GLOW
                "sparkl" in t || "glitter" in t || "twinkl" in t -> SPARKLE
                else -> null
            }
        }

        /** The effect that goes with a story sound, if it has one. */
        fun forSound(id: String): Fx? = when (id) {
            "poof", "sneeze" -> POOF
            "zap" -> ZAP
            "fizz" -> FIZZ
            "bubble", "splash" -> BUBBLES
            "unlock" -> SPARKLE
            else -> null
        }
    }
}
