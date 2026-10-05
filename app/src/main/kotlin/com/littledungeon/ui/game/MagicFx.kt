package com.littledungeon.ui.game

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import com.littledungeon.R
import com.littledungeon.audio.Narrator
import com.littledungeon.engine.rpg.run.Fx
import com.littledungeon.ui.art.Art
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Magic you can see. Whenever the narrator says something magical happens (a spell is cast,
 * a hat turns into a bunny, rainbow sparkles), the matching effect plays on the screen at that
 * moment. Which words and sounds count is the engine's [Fx]; here is only how each one looks.
 * [hat] is where the wizard's hat is, as fractions of the screen, if a wizard is in the scene.
 */
@Composable
fun MagicLayer(narrator: Narrator, w: Dp, h: Dp, hat: Offset?) {
    val bursts = remember { mutableStateListOf<Burst>() }
    LaunchedEffect(narrator) {
        val scope = this
        var next = 0
        narrator.magic.collect { fx ->
            val burst = Burst(next++, fx)
            bursts += burst
            scope.launch {
                delay(lifeOf(fx) + 200L)
                bursts.remove(burst)
            }
        }
    }
    for (burst in bursts) key(burst.id) { Magic(burst, w, h, hat) }
}

private class Burst(val id: Int, val fx: Fx)

private class Particle(val angle: Float, val speed: Float, val size: Float, val color: Int)

private fun lifeOf(fx: Fx): Int = when (fx) {
    Fx.SPARKLE -> 1400
    Fx.SPELL -> 2000
    Fx.ZAP -> 700
    Fx.RAINBOW -> 2800
    Fx.POOF -> 1100
    Fx.FIZZ -> 1700
    Fx.BUBBLES -> 2400
    Fx.GLOW -> 1900
    Fx.BUNNY -> 3800
    Fx.FROG -> 1700
}

private val sparkColors = listOf(Color(0xFFFFE066), Color.White, Color(0xFFFFB3D9), Color(0xFFB3E5FF))
private val rainbow = listOf(Color(0xFFE5484D), Color(0xFFFF9F2E), Color(0xFFFFE066), Color(0xFF4FC24A), Color(0xFF4F8FD9), Color(0xFF9B59D6))

@Composable
private fun Magic(burst: Burst, w: Dp, h: Dp, hat: Offset?) {
    val fx = burst.fx
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) { progress.animateTo(1f, tween(lifeOf(fx), easing = LinearEasing)) }
    val p = progress.value
    val particles = remember {
        val r = Random(burst.id * 7919 + 13)
        List(28) { Particle(r.nextFloat() * 2f * PI.toFloat(), 0.35f + r.nextFloat() * 0.65f, 0.6f + r.nextFloat() * 0.8f, r.nextInt(sparkColors.size)) }
    }
    val target = Offset(0.68f, 0.5f)
    val spot = if (fx == Fx.BUNNY && hat != null) hat else target
    val handX = 0.11f + 0.22f * (h / w)
    Canvas(Modifier.fillMaxSize()) {
        val at = Offset(spot.x * size.width, spot.y * size.height)
        val reach = size.height * 0.28f
        when (fx) {
            Fx.SPARKLE -> sparkleBurst(at, p, reach, particles)
            Fx.SPELL -> {
                val from = Offset(handX * size.width, size.height * 0.52f)
                val to = Offset(target.x * size.width, target.y * size.height)
                val flight = 0.4f
                if (p < flight) {
                    val u = p / flight
                    // The bolt: a glowing orb arcing from the wand, with a trail of stars behind it.
                    for (k in 0 until 8) {
                        val uu = (u - k * 0.05f).coerceAtLeast(0f)
                        val c = along(from, to, uu, size.height * 0.15f)
                        starAt(c, size.height * 0.035f * (1f - k / 9f), sparkColors[k % sparkColors.size].copy(alpha = 1f - k / 9f), uu * 9f)
                    }
                    val head = along(from, to, u, size.height * 0.15f)
                    drawCircle(Color(0x44FFE066), size.height * 0.09f, head)
                    drawCircle(Color(0xAAFFF3C4), size.height * 0.05f, head)
                    drawCircle(Color.White, size.height * 0.025f, head)
                } else {
                    val u = (p - flight) / (1f - flight)
                    drawCircle(Color.White.copy(alpha = (0.55f * (1f - u)).coerceAtLeast(0f)), size.height * 0.22f * u, to)
                    sparkleBurst(to, u, reach * 1.2f, particles)
                }
            }
            Fx.ZAP -> {
                // A few quick flashes of lightning.
                if ((p * 9).toInt() % 2 == 0) {
                    val top = Offset(at.x, at.y - size.height * 0.3f)
                    val bolt = Path().apply {
                        moveTo(top.x, top.y)
                        for (k in 1..5) {
                            val jitter = (if (k % 2 == 0) 1f else -1f) * size.height * 0.05f * particles[k].size
                            lineTo(at.x + jitter, top.y + (at.y - top.y) * k / 5f)
                        }
                    }
                    drawPath(bolt, Color(0x66FFE066), style = Stroke(size.height * 0.04f, cap = StrokeCap.Round))
                    drawPath(bolt, Color.White, style = Stroke(size.height * 0.012f, cap = StrokeCap.Round))
                }
            }
            Fx.RAINBOW -> {
                val grow = (p * 2.2f).coerceAtMost(1f)
                val alpha = ((1f - p) * 3f).coerceIn(0f, 1f)
                val c = Offset(size.width * 0.58f, size.height * 0.98f)
                val band = size.height * 0.035f
                rainbow.forEachIndexed { i, color ->
                    val r = size.height * 0.55f - i * band
                    drawArc(
                        color.copy(alpha = 0.85f * alpha), 180f, 180f * grow, false,
                        topLeft = Offset(c.x - r, c.y - r), size = Size(2 * r, 2 * r), style = Stroke(band * 1.05f),
                    )
                }
            }
            Fx.POOF -> puffs(at, p, reach, particles, Color(0xFFF2EEF8))
            Fx.FIZZ -> {
                puffs(Offset(at.x, at.y - reach * 0.3f * p), p, reach * 0.7f, particles, Color(0xFFB9A7CF))
                // A few sad sparks that drop instead of flying.
                for (q in particles.take(8)) {
                    val c = Offset(at.x + cos(q.angle) * reach * 0.35f * q.speed, at.y + reach * 0.5f * p * p * q.size - reach * 0.1f)
                    drawCircle(Color(0xFFFF9F2E).copy(alpha = (1f - p).coerceAtLeast(0f)), size.height * 0.008f * q.size, c)
                }
            }
            Fx.BUBBLES -> {
                for (q in particles.take(12)) {
                    val x = at.x + cos(q.angle * 2f) * size.width * 0.16f * q.speed
                    val y = size.height * 0.9f - p * size.height * 0.75f * q.speed
                    val r = size.height * 0.03f * q.size
                    val a = (1f - p * 0.7f).coerceIn(0f, 1f)
                    drawCircle(Color(0xFFB3E5FF).copy(alpha = 0.25f * a), r, Offset(x, y))
                    drawCircle(Color.White.copy(alpha = 0.8f * a), r, Offset(x, y), style = Stroke(r * 0.18f))
                    drawCircle(Color.White.copy(alpha = 0.9f * a), r * 0.2f, Offset(x - r * 0.35f, y - r * 0.35f))
                }
            }
            Fx.GLOW -> {
                val a = sin(PI.toFloat() * p)
                drawRect(
                    Brush.radialGradient(
                        listOf(Color(0xFFFFE680), Color.Transparent),
                        center = Offset(size.width * 0.45f, size.height * 0.6f), radius = size.width * 0.6f,
                    ),
                    alpha = 0.6f * a,
                )
                for (q in particles.take(10)) {
                    starAt(Offset(size.width * (0.15f + 0.7f * q.speed), size.height * (0.2f + 0.6f * (q.angle / 6.3f))), size.height * 0.02f * q.size, Color.White.copy(alpha = 0.8f * a), q.angle)
                }
            }
            Fx.BUNNY -> {
                if (p < 0.3f) puffs(at, p / 0.3f, reach * 0.6f, particles, Color(0xFFF2EEF8))
                sparkleBurst(at, (p * 1.5f).coerceAtMost(1f), reach * 0.8f, particles)
            }
            Fx.FROG -> Unit
        }
    }
    // The hat really turns into a bunny, and a frog really hops out.
    when (fx) {
        Fx.BUNNY -> Pop("art_mini_bunny", spot.x, spot.y - 0.02f, 0.17f, w, h, p, wobble = true)
        Fx.FROG -> Pop("art_mini_frog", 0.55f + 0.25f * p, 0.55f - 0.3f * sin(PI.toFloat() * p), 0.16f, w, h, p, wobble = false)
        else -> Unit
    }
}

@Composable
private fun Pop(art: String, x: Float, y: Float, size: Float, w: Dp, h: Dp, p: Float, wobble: Boolean) {
    val s = if (p < 0.1f) p / 0.1f else if (wobble) 1f + 0.06f * sin(p * 30f) else 1f
    val fade = if (p > 0.9f) (1f - p) / 0.1f else 1f
    Box(
        Modifier
            .at(w * x, h * y, h * size, h * size)
            .graphicsLayer {
                scaleX = s
                scaleY = s
                alpha = fade.coerceIn(0f, 1f)
                rotationZ = if (wobble) 6f * sin(p * 18f) else 0f
            },
    ) {
        Image(painterResource(Art.byName(art) ?: R.drawable.art_treasure), null, Modifier.fillMaxSize())
    }
}

/** A point part of the way from [a] to [b], lifted into an arc by [lift]. */
private fun along(a: Offset, b: Offset, u: Float, lift: Float): Offset =
    Offset(a.x + (b.x - a.x) * u, a.y + (b.y - a.y) * u - lift * sin(PI.toFloat() * u))

private fun DrawScope.starAt(c: Offset, r: Float, color: Color, rotation: Float = 0f) {
    val path = Path().apply {
        for (k in 0 until 8) {
            val a = rotation + k * (PI / 4).toFloat()
            val radius = if (k % 2 == 0) r else r * 0.35f
            val point = Offset(c.x + radius * cos(a), c.y + radius * sin(a))
            if (k == 0) moveTo(point.x, point.y) else lineTo(point.x, point.y)
        }
        close()
    }
    drawPath(path, color)
}

/** Stars flying out from [c] and fading, [reach] being how far the farthest goes. */
private fun DrawScope.sparkleBurst(c: Offset, p: Float, reach: Float, ps: List<Particle>) {
    val fade = (1f - p).coerceIn(0f, 1f)
    val out = 1f - (1f - p) * (1f - p)
    for (q in ps) {
        val d = reach * q.speed * out
        val pos = Offset(c.x + d * cos(q.angle), c.y + d * sin(q.angle) + reach * 0.25f * p * p)
        starAt(pos, reach * 0.1f * q.size * (0.6f + 0.4f * fade), sparkColors[q.color].copy(alpha = fade), q.angle + p * 3f)
    }
}

/** Round puffs of smoke spreading from [c] and fading. */
private fun DrawScope.puffs(c: Offset, p: Float, reach: Float, ps: List<Particle>, color: Color) {
    val fade = (1f - p).coerceIn(0f, 1f)
    for (q in ps.take(9)) {
        val d = reach * 0.6f * q.speed * p
        val pos = Offset(c.x + d * cos(q.angle), c.y + d * sin(q.angle) * 0.7f)
        drawCircle(color.copy(alpha = 0.85f * fade), reach * 0.22f * q.size * (0.5f + p), pos)
    }
}
