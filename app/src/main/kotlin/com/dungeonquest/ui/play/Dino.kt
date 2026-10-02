package com.dungeonquest.ui.play

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer

enum class DinoMood { Idle, Talking, Thinking, Happy }

/**
 * The dino friend, drawn from layered shapes in a 200×200 box (sketch A from the picker page,
 * a placeholder until your son picks one). Breathes and blinks on its own; hops when happy and
 * tilts its head when thinking.
 */
@Composable
fun Dino(mood: DinoMood, modifier: Modifier = Modifier) {
    val idle = rememberInfiniteTransition(label = "idle")
    val breath by idle.animateFloat(
        initialValue = 1f, targetValue = 0.975f,
        animationSpec = infiniteRepeatable(tween(1300, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "breath",
    )
    val blink by idle.animateFloat(
        initialValue = 1f, targetValue = 1f,
        animationSpec = infiniteRepeatable(keyframes {
            durationMillis = 4200
            1f at 3900
            0.1f at 4000
            1f at 4100
        }),
        label = "blink",
    )
    val talk by idle.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(220), RepeatMode.Reverse),
        label = "talk",
    )
    val tilt by animateFloatAsState(if (mood == DinoMood.Thinking) -8f else 0f, spring(dampingRatio = 0.5f), label = "tilt")

    val hop = remember { Animatable(0f) }
    val squash = remember { Animatable(1f) }
    LaunchedEffect(mood) {
        if (mood == DinoMood.Happy) {
            repeat(2) {
                squash.animateTo(0.85f, tween(90))
                squash.animateTo(1.08f, tween(90))
                hop.animateTo(-0.18f, tween(220, easing = FastOutSlowInEasing))
                hop.animateTo(0f, tween(200))
                squash.animateTo(0.9f, tween(70))
                squash.animateTo(1f, spring(dampingRatio = 0.4f))
            }
        }
    }

    Canvas(
        modifier.graphicsLayer {
            translationY = hop.value * size.height
            scaleX = 2f - squash.value * breath
            scaleY = squash.value * breath
            rotationZ = tilt
            transformOrigin = TransformOrigin(0.5f, 1f)
        },
    ) {
        val unit = minOf(size.width, size.height) / 200f
        val left = (size.width - 200f * unit) / 2f
        val top = size.height - 200f * unit
        withTransform({
            translate(left, top)
            scale(unit, unit, pivot = Offset.Zero)
        }) {
            drawStego(blink = blink, mouthOpen = mood == DinoMood.Happy || (mood == DinoMood.Talking && talk > 0.5f))
        }
    }
}

private val Body = Color(0xFF6CC24A)
private val BodyShade = Color(0xFF4FA332)
private val Tail = Color(0xFF5DB83D)
private val Plate = Color(0xFFFF9F43)
private val Belly = Color(0xFFE3F5C4)
private val Cheek = Color(0xCCFF9FB0)
private val Eye = Color(0xFF1F2A22)

private fun DrawScope.drawStego(blink: Float, mouthOpen: Boolean) {
    drawPath(Path().apply { moveTo(52f, 140f); quadraticTo(20f, 140f, 8f, 118f); quadraticTo(30f, 128f, 56f, 122f); close() }, Tail)
    listOf(
        listOf(58f, 98f, 66f, 70f, 80f, 96f),
        listOf(80f, 90f, 92f, 58f, 106f, 88f),
        listOf(104f, 92f, 116f, 66f, 126f, 96f),
    ).forEach { p ->
        val plate = Path().apply { moveTo(p[0], p[1]); lineTo(p[2], p[3]); lineTo(p[4], p[5]); close() }
        drawPath(plate, Plate)
        drawPath(plate, Plate, style = Stroke(width = 8f, join = StrokeJoin.Round))
    }
    leg(62f, 150f, BodyShade)
    leg(112f, 150f, BodyShade)
    drawOval(Body, topLeft = Offset(36f, 88f), size = Size(120f, 84f))
    drawOval(Belly, topLeft = Offset(60f, 126f), size = Size(80f, 40f))
    leg(74f, 154f, Body)
    leg(124f, 154f, Body)
    drawCircle(Body, radius = 32f, center = Offset(150f, 104f))
    drawOval(Cheek, topLeft = Offset(159f, 113f), size = Size(14f, 10f))
    withTransform({ scale(1f, blink, pivot = Offset(156f, 96f)) }) {
        drawCircle(Color.White, radius = 11f, center = Offset(156f, 96f))
        drawCircle(Eye, radius = 6.5f, center = Offset(159f, 97f))
        drawCircle(Color.White, radius = 2.4f, center = Offset(161.5f, 94f))
    }
    if (mouthOpen) {
        drawPath(Path().apply { moveTo(158f, 112f); quadraticTo(170f, 130f, 180f, 110f); close() }, Eye)
    } else {
        drawPath(
            Path().apply { moveTo(160f, 116f); quadraticTo(170f, 122f, 178f, 114f) },
            Eye,
            style = Stroke(width = 3.5f, cap = StrokeCap.Round),
        )
    }
}

private fun DrawScope.leg(x: Float, y: Float, color: Color) {
    drawRoundRect(color, topLeft = Offset(x, y), size = Size(18f, 30f), cornerRadius = CornerRadius(9f, 9f))
}
