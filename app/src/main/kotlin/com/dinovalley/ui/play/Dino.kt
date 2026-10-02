package com.dinovalley.ui.play

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
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer

enum class DinoMood { Idle, Talking, Thinking, Happy }

/**
 * The dino friend: the little T. rex your son picked, drawn from layered shapes in a 200×200
 * box. Breathes and blinks on its own, talks while narrating, hops when happy, and tilts its head
 * and looks up when thinking.
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
            drawRex(
                blink = blink,
                mouthOpen = mood == DinoMood.Happy || (mood == DinoMood.Talking && talk > 0.5f),
                lookUp = mood == DinoMood.Thinking,
            )
        }
    }
}

private val Body = Color(0xFFFF8A3D)
private val BodyShade = Color(0xFFE46D20)
private val Tail = Color(0xFFF07A2C)
private val Belly = Color(0xFFFFD9A8)
private val Cheek = Color(0xD9FF9FB0)
private val Eye = Color(0xFF1F2A22)

/** The little T. rex your son picked (sketch C). */
private fun DrawScope.drawRex(blink: Float, mouthOpen: Boolean, lookUp: Boolean) {
    drawPath(Path().apply { moveTo(60f, 150f); quadraticTo(22f, 160f, 6f, 140f); quadraticTo(34f, 140f, 60f, 128f); close() }, Tail)
    leg(66f, 156f, BodyShade)
    drawOval(Body, topLeft = Offset(46f, 78f), size = Size(92f, 100f))
    drawOval(Belly, topLeft = Offset(72f, 108f), size = Size(56f, 64f))
    leg(98f, 158f, Body)
    rotate(30f, pivot = Offset(134f, 122f)) {
        drawOval(Body, topLeft = Offset(125f, 117f), size = Size(18f, 10f))
    }
    drawPath(Path().apply { moveTo(74f, 78f); lineTo(82f, 66f); lineTo(88f, 78f); close() }, BodyShade)
    drawPath(Path().apply { moveTo(62f, 92f); lineTo(68f, 80f); lineTo(76f, 91f); close() }, BodyShade)
    drawOval(Body, topLeft = Offset(76f, 38f), size = Size(92f, 72f))
    drawOval(Cheek, topLeft = Offset(139f, 83f), size = Size(14f, 10f))
    val look = if (lookUp) -3.5f else 0f
    withTransform({ scale(1f, blink, pivot = Offset(126f, 64f)) }) {
        drawCircle(Color.White, radius = 11f, center = Offset(126f, 64f))
        drawCircle(Eye, radius = 6.5f, center = Offset(129f, 65f + look))
        drawCircle(Color.White, radius = 2.4f, center = Offset(131.5f, 62f + look))
    }
    if (mouthOpen) {
        drawPath(Path().apply { moveTo(130f, 86f); quadraticTo(150f, 112f, 166f, 82f); close() }, Eye)
        drawPath(Path().apply { moveTo(140f, 88f); lineTo(144f, 94f); lineTo(148f, 88f); close() }, Color.White)
        drawPath(Path().apply { moveTo(152f, 87f); lineTo(156f, 93f); lineTo(160f, 87f); close() }, Color.White)
    } else {
        drawPath(
            Path().apply { moveTo(132f, 90f); quadraticTo(150f, 100f, 164f, 86f) },
            Eye,
            style = Stroke(width = 3.5f, cap = StrokeCap.Round),
        )
    }
}

private fun DrawScope.leg(x: Float, y: Float, color: Color) {
    drawRoundRect(color, topLeft = Offset(x, y), size = Size(20f, 28f), cornerRadius = CornerRadius(10f, 10f))
}
