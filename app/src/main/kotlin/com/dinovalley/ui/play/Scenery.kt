package com.dinovalley.ui.play

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import com.dinovalley.ui.theme.DinoColors

/** Dino Valley: sky, a sleepy volcano puffing smoke, rolling hills and grass. */
@Composable
fun Scenery(modifier: Modifier = Modifier) {
    val ambient = rememberInfiniteTransition(label = "ambient")
    val puff by ambient.animateFloat(0f, 1f, infiniteRepeatable(tween(4000, easing = LinearEasing)), label = "puff")
    val sway by ambient.animateFloat(-1f, 1f, infiniteRepeatable(tween(3000), RepeatMode.Reverse), label = "sway")

    Canvas(modifier) {
        val w = size.width
        val h = size.height
        drawRect(Brush.verticalGradient(listOf(DinoColors.SkyTop, DinoColors.SkyBottom)))
        drawCircle(Color(0xFFFFE27A), radius = h * 0.09f, center = Offset(w * 0.1f, h * 0.14f))

        // Volcano with smoke puffs drifting up.
        val baseY = h * 0.62f
        val volcano = Path().apply {
            moveTo(w * 0.62f, baseY)
            lineTo(w * 0.74f, h * 0.24f)
            lineTo(w * 0.82f, h * 0.24f)
            lineTo(w * 0.95f, baseY)
            close()
        }
        drawPath(volcano, DinoColors.Volcano)
        drawRect(DinoColors.Lava, topLeft = Offset(w * 0.745f, h * 0.24f), size = Size(w * 0.07f, h * 0.025f))
        for (i in 0..2) {
            val t = (puff + i / 3f) % 1f
            drawCircle(
                Color.White.copy(alpha = 0.75f * (1f - t)),
                radius = h * (0.03f + 0.05f * t),
                center = Offset(w * 0.78f + sway * w * 0.01f + t * w * 0.03f, h * 0.22f - t * h * 0.2f),
            )
        }

        drawOval(DinoColors.HillFar, topLeft = Offset(-w * 0.1f, h * 0.48f), size = Size(w * 0.7f, h * 0.4f))
        drawOval(DinoColors.HillFar, topLeft = Offset(w * 0.45f, h * 0.52f), size = Size(w * 0.75f, h * 0.4f))
        drawRect(DinoColors.Grass, topLeft = Offset(0f, h * 0.68f), size = Size(w, h * 0.32f))
        drawRect(DinoColors.GrassDark, topLeft = Offset(0f, h * 0.68f), size = Size(w, h * 0.012f))
        // A few tufts of grass that sway.
        for (i in 0 until 9) {
            val x = w * (0.05f + i * 0.11f)
            val y = h * (0.78f + (i % 3) * 0.06f)
            val tuft = Path().apply {
                moveTo(x, y)
                quadraticTo(x - 6f + sway * 4f, y - 22f, x - 10f + sway * 6f, y - 30f)
                moveTo(x, y)
                quadraticTo(x + 2f + sway * 4f, y - 24f, x + 4f + sway * 6f, y - 34f)
            }
            drawPath(tuft, DinoColors.GrassDark, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 5f))
        }
    }
}
