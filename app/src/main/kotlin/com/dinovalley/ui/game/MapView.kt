package com.dinovalley.ui.game

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dinovalley.engine.rpg.run.Adventure
import com.dinovalley.engine.rpg.run.Place
import com.dinovalley.engine.rpg.run.placeOf
import com.dinovalley.engine.rpg.world.DungeonMap
import com.dinovalley.engine.rpg.world.Room
import com.dinovalley.engine.rpg.world.RoomKind
import com.dinovalley.engine.rpg.world.Stop
import com.dinovalley.ui.art.Art
import com.dinovalley.ui.art.Character
import com.dinovalley.ui.art.Mood
import com.dinovalley.ui.art.Rigs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

private enum class Kind { CAMP, LANDMARK, JUNCTION, DOOR }

/** One place on the map. Forks have a junction, then one room per door side by side. */
private class MapNode(val kind: Kind, val stop: Int, val door: Int, val room: Room?, val col: Int, val row: Int, val rows: Int)

/** The map as columns left to right: the camp, then every landmark, and for each fork a junction and its doors. */
private fun columnsOf(map: DungeonMap): List<List<MapNode>> {
    val cols = mutableListOf<List<MapNode>>()
    cols += listOf(MapNode(Kind.CAMP, -1, -1, null, 0, 0, 1))
    map.stops.forEachIndexed { i, stop ->
        when (stop) {
            is Stop.Landmark -> cols += listOf(MapNode(Kind.LANDMARK, i, -1, stop.room, cols.size, 0, 1))
            is Stop.Fork -> {
                cols += listOf(MapNode(Kind.JUNCTION, i, -1, null, cols.size, 0, 1))
                val c = cols.size
                cols += stop.doors.mapIndexed { j, r -> MapNode(Kind.DOOR, i, j, r, c, j, stop.doors.size) }
            }
        }
    }
    return cols
}

/**
 * The adventure's map: a path from the camp to the dragon's lair that splits into a branch for
 * every door at a fork and comes back together after. The doors taken are walked in gold; a
 * door that was not the clue's winds back round to the same doors (a looping arrow), so the child
 * sees the way they went. The baby dragon marks where the hero is. [atFork] means the hero is
 * choosing at a fork's doors now; [windowed] zooms in on the part of the map around the hero.
 * Drawn in the box from ([left], [top]) of size [width] x [height].
 */
@Composable
fun DungeonMapView(
    map: DungeonMap, position: Int, visits: List<Adventure.Visit>, atFork: Boolean, windowed: Boolean,
    left: Dp, top: Dp, width: Dp, height: Dp,
) {
    val columns = remember(map) { columnsOf(map) }
    val all = columns.flatten()
    val taken = visits.map { it.stop to it.door }.toSet()
    val looped = visits.filter { it.looped }.map { it.stop to it.door }.toSet()
    val resolved = visits.filter { !it.looped }.map { it.stop }.toSet()

    fun visited(n: MapNode) = when (n.kind) {
        Kind.CAMP -> true
        Kind.LANDMARK, Kind.JUNCTION -> n.stop <= position
        Kind.DOOR -> (n.stop to n.door) in taken
    }

    // Where the hero stands: the camp, a landmark, the junction while choosing, or the door's room.
    val hero = when {
        position < 0 -> all.first()
        else -> {
            val here = all.filter { it.stop == position }
            val fork = here.any { it.kind == Kind.JUNCTION }
            if (!fork) {
                here.first()
            } else if (atFork) {
                here.first { it.kind == Kind.JUNCTION }
            } else {
                val last = visits.lastOrNull { it.stop == position }
                here.firstOrNull { it.kind == Kind.DOOR && it.door == last?.door } ?: here.first { it.kind == Kind.JUNCTION }
            }
        }
    }

    // Zoomed in while choosing a door; the whole map while the story shows it.
    val span = 7
    val c0 = if (windowed) (hero.col - 2).coerceIn(0, max(0, columns.size - span)) else 0
    val c1 = if (windowed) min(columns.size - 1, c0 + span - 1) else columns.size - 1
    val cc = c1 - c0 + 1
    val maxRows = columns.maxOf { it.size }
    val node = minOf(width / (cc * 1.28f), height / (maxRows * 1.3f))
    val rowGap = minOf(node * 1.25f, (height - node) / max(maxRows - 1, 1))

    fun x(n: MapNode): Dp = left + node / 2 + (width - node) * ((n.col - c0) / max(cc - 1, 1).toFloat())
    fun y(n: MapNode): Dp = top + height / 2 + rowGap * (n.row - (n.rows - 1) / 2f)
    fun sizeOf(n: MapNode): Dp = when {
        n.kind == Kind.JUNCTION -> node * 0.55f
        n.room?.kind == RoomKind.LAIR -> node * 1.15f
        else -> node
    }

    val shown = all.filter { it.col in c0..c1 }
    val t = rememberInfiniteTransition(label = "map")
    val bob by t.animateFloat(-1f, 1f, infiniteRepeatable(tween(500), RepeatMode.Reverse), label = "bob")
    val density = LocalDensity.current

    // The paths: walked in gold, the rest faint and dashed; wrong doors loop back to their junction.
    Canvas(Modifier.fillMaxSize()) {
        val stroke = node.toPx() * 0.09f
        fun pt(n: MapNode) = Offset(x(n).toPx(), y(n).toPx())
        for (c in c0 until c1) {
            for (a in columns[c]) for (b in columns[c + 1]) {
                val loopedOut = a.kind == Kind.DOOR && (a.stop to a.door) in looped
                val walked = visited(a) && visited(b) && !loopedOut
                branch(pt(a), pt(b), stroke, walked)
            }
        }
        for (d in shown) {
            if (d.kind != Kind.DOOR || (d.stop to d.door) !in looped) continue
            val j = all.firstOrNull { it.kind == Kind.JUNCTION && it.stop == d.stop } ?: continue
            if (j.col < c0) continue
            val up = d.row < (d.rows - 1) / 2f + 0.01f
            loopBack(pt(d), pt(j), node.toPx() * 1.15f * (if (up) -1f else 1f), stroke)
        }
    }

    for (n in shown) {
        val size = sizeOf(n)
        val isHere = n === hero
        val dim = n.kind == Kind.DOOR && !visited(n) && n.stop in resolved
        val wrong = n.kind == Kind.DOOR && (n.stop to n.door) in looped
        val edge = when {
            isHere -> Palette.Name
            wrong -> Color(0xFFE5484D)
            visited(n) -> Palette.Gold
            else -> Palette.PaperEdge
        }
        Box(
            Modifier.at(x(n), y(n), size, size)
                .shadow(if (n.kind == Kind.JUNCTION) 2.dp else 4.dp, CircleShape)
                .background(Palette.Paper, CircleShape)
                .border(if (isHere) 5.dp else 3.dp, edge, CircleShape)
                .clip(CircleShape)
                .graphicsLayer { alpha = if (dim) 0.4f else 1f },
            contentAlignment = Alignment.Center,
        ) {
            when (n.kind) {
                Kind.CAMP -> Thumb(Place.CAMP)
                Kind.LANDMARK -> Thumb(placeOf(n.room!!.kind))
                Kind.JUNCTION -> {
                    val fs = with(density) { (size * 0.6f).toSp() }
                    Text(if (n.stop >= position) "?" else "•", fontSize = fs, fontWeight = FontWeight.Black, color = Palette.Name, maxLines = 1)
                }
                Kind.DOOR -> if (visited(n)) {
                    Thumb(placeOf(n.room!!.kind))
                } else {
                    Image(painterResource(Art.door(n.room!!.hue)), null, Modifier.size(size * 0.8f))
                }
            }
        }
        if (wrong) {
            val fs = with(density) { (node * 0.34f).toSp() }
            Text("↩", fontSize = fs, fontWeight = FontWeight.Black, color = Color(0xFFE5484D), modifier = Modifier.at(x(n) + size * 0.36f, y(n) - size * 0.36f, node * 0.4f, node * 0.4f))
        }
        if (n.room?.kind == RoomKind.LAIR) {
            val fs = with(density) { (node * 0.32f).toSp() }
            Text("⭐", fontSize = fs, fontWeight = FontWeight.Black, modifier = Modifier.at(x(n) + size * 0.4f, y(n) - size * 0.42f, node * 0.4f, node * 0.4f))
        }
    }
    // The baby dragon: you are here.
    val token = node * 0.85f
    Character(
        Rigs.babyDragon, Mood.HAPPY,
        Modifier.at(x(hero), y(hero) - node * 0.7f, token, token).graphicsLayer { translationY = bob * 6f },
    )
}

/** A branch of the path from [a] to [b], an easy S-curve so splits and joins read as paths. */
private fun DrawScope.branch(a: Offset, b: Offset, stroke: Float, walked: Boolean) {
    val dx = (b.x - a.x) / 2f
    val path = Path().apply {
        moveTo(a.x, a.y)
        cubicTo(a.x + dx, a.y, b.x - dx, b.y, b.x, b.y)
    }
    if (walked) {
        drawPath(path, Color(0x66FFE680), style = Stroke(stroke * 2.2f, cap = StrokeCap.Round))
        drawPath(path, Color(0xFF8A5A2A), style = Stroke(stroke, cap = StrokeCap.Round))
    } else {
        drawPath(
            path, Color(0x998A5A2A),
            style = Stroke(stroke, cap = StrokeCap.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(stroke * 1.6f, stroke * 1.4f))),
        )
    }
}

/** A path from a wrong door round in a loop back to the junction, with an arrow head. */
private fun DrawScope.loopBack(from: Offset, to: Offset, bulge: Float, stroke: Float) {
    val c1 = Offset(from.x, from.y + bulge)
    val c2 = Offset(to.x, to.y + bulge)
    val path = Path().apply {
        moveTo(from.x, from.y)
        cubicTo(c1.x, c1.y, c2.x, c2.y, to.x, to.y)
    }
    val red = Color(0xFFE5484D)
    drawPath(
        path, red,
        style = Stroke(stroke * 0.9f, cap = StrokeCap.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(stroke * 1.3f, stroke * 1.1f))),
    )
    // The arrow head points along the curve's end.
    val angle = atan2(to.y - c2.y, to.x - c2.x)
    val len = stroke * 3.2f
    for (side in listOf(-0.5f, 0.5f)) {
        val a = angle + Math.PI.toFloat() + side
        drawLine(red, to, Offset(to.x + len * cos(a), to.y + len * sin(a)), stroke * 0.9f, StrokeCap.Round)
    }
}

@Composable
private fun Thumb(place: Place) {
    Image(painterResource(Art.place(place)), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
}
