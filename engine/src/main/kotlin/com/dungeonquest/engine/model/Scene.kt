package com.dungeonquest.engine.model

/** Everything the UI needs to draw a question. Positions are 0..1 relative to the play area. */
data class Scene(val objects: List<PlacedObject>)

data class PlacedObject(
    val sprite: SpriteId,
    val x: Float,
    val y: Float,
    val scale: Float = 1f,
    val rotationDeg: Float = 0f,
    val group: Int = 0,
    val countable: Boolean = true,
)

enum class Arrangement { LINE, GRID, SCATTER, CLUSTERS }

enum class ObjectSize(val scale: Float) { LARGE(1f), MEDIUM(0.8f), SMALL(0.62f) }

/** GENTLE asks for an easier question within the same level, e.g. after a miss. */
enum class Ease { NORMAL, GENTLE }
