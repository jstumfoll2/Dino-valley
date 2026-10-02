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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
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
import kotlin.math.sin

/**
 * The adventure's map: a winding path from the camp to the dragon's lair, with a picture of
 * every stop. Forks show their two doors until one is picked, then the room behind it. The baby
 * dragon marks where the hero is. Drawn in the box from ([left], [top]) of size [width] x [height].
 */
@Composable
fun DungeonMapView(map: DungeonMap, position: Int, route: Map<Int, Room>, left: Dp, top: Dp, width: Dp, height: Dp) {
    val n = map.stops.size + 1 // the camp, then every stop
    val node = minOf(height * 0.42f, width / (n * 1.25f))
    fun x(i: Int): Dp = left + node / 2 + (width - node) * (i / (n - 1f))
    fun y(i: Int): Dp = top + height / 2 + (height - node) * 0.38f * sin(i * 1.3f).toFloat()
    val here = position + 1
    val t = rememberInfiniteTransition(label = "map")
    val bob by t.animateFloat(-1f, 1f, infiniteRepeatable(tween(500), RepeatMode.Reverse), label = "bob")
    val density = LocalDensity.current

    // The path: walked part solid, the rest dashed.
    Canvas(Modifier.fillMaxSize()) {
        val stroke = node.toPx() * 0.08f
        for (i in 0 until n - 1) {
            val a = Offset(x(i).toPx(), y(i).toPx())
            val b = Offset(x(i + 1).toPx(), y(i + 1).toPx())
            val walked = i < here
            drawLine(
                if (walked) Color(0xFF8A5A2A) else Color(0x998A5A2A), a, b, stroke, StrokeCap.Round,
                if (walked) null else PathEffect.dashPathEffect(floatArrayOf(stroke * 1.6f, stroke * 1.4f)),
            )
        }
    }
    for (i in 0 until n) {
        val stop = map.stops.getOrNull(i - 1)
        val visited = i <= here
        val size = if (i == n - 1) node * 1.15f else node
        Box(
            Modifier.at(x(i), y(i), size, size)
                .shadow(4.dp, CircleShape)
                .background(Palette.Paper, CircleShape)
                .border(if (i == here) 5.dp else 3.dp, if (i == here) Palette.Name else if (visited) Palette.Gold else Palette.PaperEdge, CircleShape)
                .clip(CircleShape)
                .graphicsLayer { alpha = if (visited || i == here + 1) 1f else 0.75f },
            contentAlignment = Alignment.Center,
        ) {
            when (stop) {
                null -> Thumb(Place.CAMP)
                is Stop.Landmark -> Thumb(placeOf(stop.room.kind))
                is Stop.Fork -> {
                    val picked = route[i - 1]
                    if (picked != null) {
                        Thumb(placeOf(picked.kind))
                    } else {
                        Row(Modifier.size(size * 0.8f), verticalAlignment = Alignment.CenterVertically) {
                            stop.doors.forEach { d ->
                                Image(painterResource(Art.door(d.hue)), null, Modifier.size(size * 0.8f / stop.doors.size, size * 0.6f))
                            }
                        }
                    }
                }
            }
        }
        if (stop is Stop.Landmark && stop.room.kind == RoomKind.LAIR) {
            val fs = with(density) { (node * 0.32f).toSp() }
            Text("⭐", fontSize = fs, fontWeight = FontWeight.Black, modifier = Modifier.at(x(i) + size * 0.4f, y(i) - size * 0.42f, node * 0.4f, node * 0.4f))
        }
    }
    // The baby dragon: you are here.
    val token = node * 0.85f
    Character(
        Rigs.babyDragon, Mood.HAPPY,
        Modifier.at(x(here), y(here) - node * 0.7f, token, token).graphicsLayer { translationY = bob * 6f },
    )
}

@Composable
private fun Thumb(place: Place) {
    Image(painterResource(Art.place(place)), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
}
