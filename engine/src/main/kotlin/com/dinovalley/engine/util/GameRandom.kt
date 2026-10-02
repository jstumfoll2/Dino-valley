package com.dinovalley.engine.util

import kotlin.random.Random

/**
 * All game randomness goes through here so any question can be replayed from its seed.
 * [nextSeed] hands out a fresh seed per question; each question then gets its own [Random].
 */
class GameRandom(seed: Long) {
    private val seeds = Random(seed)

    fun nextSeed(): Long = seeds.nextLong()

    companion object {
        fun forQuestion(seed: Long): Random = Random(seed)
    }
}
