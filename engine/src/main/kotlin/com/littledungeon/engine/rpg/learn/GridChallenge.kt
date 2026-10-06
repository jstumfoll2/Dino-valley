package com.littledungeon.engine.rpg.learn

import com.littledungeon.engine.model.Speech

/** One step of a route on a treasure map. */
enum class Way(val plain: String, val compass: String, val dRow: Int, val dCol: Int) {
    UP("up", "north", -1, 0),
    DOWN("down", "south", 1, 0),
    LEFT("left", "west", 0, -1),
    RIGHT("right", "east", 0, 1),
}

data class Move(val way: Way, val steps: Int)

/**
 * Hoot's treasure map: a grid of squares, a start, and a route to follow ("go right two steps, then up one"). The child taps the square
 * where the route ends. Cards are the squares, numbered row by row; [answer] is the one the route ends on.
 * With [compass] the directions are north, south, east and west (up is north).
 */
data class GridChallenge(
    override val level: Int,
    override val seed: Long,
    override val prompt: List<Speech>,
    val rows: Int,
    val cols: Int,
    val startRow: Int,
    val startCol: Int,
    val moves: List<Move>,
    val compass: Boolean,
    val because: List<Speech>,
) : PickOne {
    override val skill get() = Skill.MAPS
    override val optionCount get() = rows * cols
    val endRow: Int get() = moves.fold(startRow) { r, m -> r + m.way.dRow * m.steps }
    val endCol: Int get() = moves.fold(startCol) { c, m -> c + m.way.dCol * m.steps }
    override val answer get() = endRow * cols + endCol
}
