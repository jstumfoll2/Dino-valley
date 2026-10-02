package com.dungeonquest.ui.play

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.dungeonquest.ui.theme.DinoColors
import kotlinx.coroutines.delay

/**
 * One egg. Pops in with a spring, wiggles when tapped, shows its count number, and hatches a
 * baby dino when the question is answered (the reward is the quantity itself).
 */
@Composable
fun Egg(
    color: Color,
    label: Int?,
    highlighted: Boolean,
    hatched: Boolean,
    appearDelayMs: Long,
    hatchDelayMs: Long,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val appear = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(appearDelayMs)
        appear.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
    }
    val wiggle = remember { Animatable(0f) }
    LaunchedEffect(label) {
        if (label != null) {
            for (angle in listOf(-14f, 12f, -8f, 5f, 0f)) wiggle.animateTo(angle, tween(70))
        }
    }
    val hatch = remember { Animatable(0f) }
    LaunchedEffect(hatched) {
        if (hatched) {
            delay(hatchDelayMs)
            hatch.animateTo(1f, tween(650, easing = FastOutSlowInEasing))
        }
    }
    val glow by animateFloatAsState(if (highlighted) 1f else 0f, tween(200), label = "glow")

    Box(
        modifier
            .graphicsLayer {
                scaleX = appear.value
                scaleY = appear.value
                rotationZ = wiggle.value
            }
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onTap),
    ) {
        Canvas(Modifier.fillMaxSize()) { drawEgg(color, glow, hatch.value) }
        if (label != null) {
            Text(
                text = label.toString(),
                modifier = Modifier.align(Alignment.TopCenter).graphicsLayer { translationY = -size.height * 0.55f },
                fontSize = 34.sp,
                fontWeight = FontWeight.Black,
                color = DinoColors.Ink,
                textAlign = TextAlign.Center,
            )
        }
    }
}

private fun DrawScope.drawEgg(color: Color, glow: Float, hatch: Float) {
    val w = size.width
    val h = size.height
    val eggW = w * 0.74f
    val eggH = h * 0.92f
    val left = (w - eggW) / 2f
    val top = h - eggH
    if (glow > 0f) {
        drawOval(DinoColors.Highlight.copy(alpha = 0.45f * glow), topLeft = Offset(left - 10f, top - 10f), size = Size(eggW + 20f, eggH + 20f))
    }
    // Baby dino rises out of the egg as it hatches.
    if (hatch > 0f) {
        val r = eggW * 0.36f
        val cy = top + eggH * 0.55f - hatch * eggH * 0.45f
        drawCircle(Color(0xFFFFA05C), radius = r, center = Offset(w / 2f, cy))
        drawCircle(Color.White, radius = r * 0.28f, center = Offset(w / 2f - r * 0.35f, cy - r * 0.15f))
        drawCircle(Color.White, radius = r * 0.28f, center = Offset(w / 2f + r * 0.35f, cy - r * 0.15f))
        drawCircle(Color(0xFF1F2A22), radius = r * 0.15f, center = Offset(w / 2f - r * 0.3f, cy - r * 0.12f))
        drawCircle(Color(0xFF1F2A22), radius = r * 0.15f, center = Offset(w / 2f + r * 0.4f, cy - r * 0.12f))
        drawArc(Color(0xFF1F2A22), 20f, 140f, false, topLeft = Offset(w / 2f - r * 0.3f, cy), size = Size(r * 0.6f, r * 0.4f), style = Stroke(width = r * 0.08f))
    }
    val crack = top + eggH * 0.45f
    // Bottom half stays put.
    clipRect(top = crack) { eggShape(color, left, top, eggW, eggH) }
    // Top half lifts off and tips over.
    translate(top = -hatch * eggH * 0.55f, left = hatch * eggW * 0.25f) {
        rotate(degrees = hatch * 35f, pivot = Offset(w / 2f, crack)) {
            clipRect(bottom = crack) { eggShape(color, left, top, eggW, eggH) }
        }
    }
    if (hatch > 0f) {
        val zig = Path().apply {
            moveTo(left, crack)
            var x = left
            var up = true
            while (x < left + eggW) {
                x += eggW / 8f
                lineTo(x, if (up) crack - 6f else crack)
                up = !up
            }
        }
        drawPath(zig, Color.White.copy(alpha = 0.9f), style = Stroke(width = 4f))
    }
}

private fun DrawScope.eggShape(color: Color, left: Float, top: Float, w: Float, h: Float) {
    val egg = Path().apply {
        moveTo(left + w / 2f, top)
        cubicTo(left + w * 0.85f, top, left + w, top + h * 0.55f, left + w, top + h * 0.65f)
        cubicTo(left + w, top + h * 0.88f, left + w * 0.78f, top + h, left + w / 2f, top + h)
        cubicTo(left + w * 0.22f, top + h, left, top + h * 0.88f, left, top + h * 0.65f)
        cubicTo(left, top + h * 0.55f, left + w * 0.15f, top, left + w / 2f, top)
        close()
    }
    drawPath(egg, color)
    val spot = Color.White.copy(alpha = 0.7f)
    drawOval(spot, topLeft = Offset(left + w * 0.22f, top + h * 0.38f), size = Size(w * 0.18f, h * 0.14f))
    drawOval(spot, topLeft = Offset(left + w * 0.58f, top + h * 0.58f), size = Size(w * 0.2f, h * 0.12f))
    drawOval(spot, topLeft = Offset(left + w * 0.5f, top + h * 0.2f), size = Size(w * 0.12f, h * 0.09f))
    drawOval(Color.Black.copy(alpha = 0.08f), topLeft = Offset(left + w * 0.1f, top + h * 0.78f), size = Size(w * 0.8f, h * 0.2f))
}

/** A leaf lying in the grass: something to look at that is not counted. */
@Composable
fun Leaf(rotation: Float, modifier: Modifier = Modifier) {
    Canvas(modifier.graphicsLayer { rotationZ = rotation + 30f }) {
        val w = size.width
        val h = size.height
        val leaf = Path().apply {
            moveTo(w * 0.15f, h * 0.5f)
            quadraticTo(w * 0.5f, h * 0.1f, w * 0.85f, h * 0.5f)
            quadraticTo(w * 0.5f, h * 0.9f, w * 0.15f, h * 0.5f)
            close()
        }
        drawPath(leaf, Color(0xFF4F9E3A))
        drawLine(Color(0xFF3B7E2B), Offset(w * 0.15f, h * 0.5f), Offset(w * 0.8f, h * 0.5f), strokeWidth = 3f)
    }
}
