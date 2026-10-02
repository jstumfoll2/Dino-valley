package com.dinovalley.ui.art

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import com.dinovalley.R
import kotlinx.coroutines.delay
import kotlin.random.Random

enum class DinoKind { REX, MAMA }

enum class Mood { IDLE, TALKING, HAPPY, LISTENING }

private class DinoParts(
    @DrawableRes val shadow: Int,
    @DrawableRes val tail: Int,
    @DrawableRes val legBack: Int,
    @DrawableRes val body: Int,
    @DrawableRes val legFront: Int,
    @DrawableRes val arm: Int,
    @DrawableRes val head: Int,
    @DrawableRes val eyeOpen: Int,
    @DrawableRes val eyeClosed: Int,
    @DrawableRes val mouthCalm: Int,
    @DrawableRes val mouthHappy: Int,
    @DrawableRes val mouthTalk: Int,
    @DrawableRes val lashes: Int? = null,
)

private val REX = DinoParts(
    R.drawable.art_rex_shadow, R.drawable.art_rex_tail, R.drawable.art_rex_leg_back, R.drawable.art_rex_body,
    R.drawable.art_rex_leg_front, R.drawable.art_rex_arm, R.drawable.art_rex_head, R.drawable.art_rex_eye_open,
    R.drawable.art_rex_eye_closed, R.drawable.art_rex_mouth_calm, R.drawable.art_rex_mouth_happy, R.drawable.art_rex_mouth_talk,
)

private val MAMA = DinoParts(
    R.drawable.art_mama_shadow, R.drawable.art_mama_tail, R.drawable.art_mama_leg_back, R.drawable.art_mama_body,
    R.drawable.art_mama_leg_front, R.drawable.art_mama_arm, R.drawable.art_mama_head, R.drawable.art_mama_eye_open,
    R.drawable.art_mama_eye_closed, R.drawable.art_mama_mouth_calm, R.drawable.art_mama_mouth_happy, R.drawable.art_mama_mouth_talk,
    R.drawable.art_mama_lashes,
)

// Pivot points in the 300×300 artwork, as fractions: where the tail, arm and neck bend.
private val TAIL_PIVOT = TransformOrigin(0.36f, 0.72f)
private val ARM_PIVOT = TransformOrigin(0.64f, 0.6f)
private val NECK_PIVOT = TransformOrigin(0.6f, 0.55f)
private val FEET = TransformOrigin(0.5f, 0.95f)

/**
 * A painted dino built from layers, so it can breathe, blink, wag, talk and hop.
 * The art faces right; [facingLeft] mirrors it.
 */
@Composable
fun Dino(kind: DinoKind, mood: Mood, modifier: Modifier = Modifier, facingLeft: Boolean = false) {
    val parts = if (kind == DinoKind.REX) REX else MAMA
    val idle = rememberInfiniteTransition(label = "idle")
    val breath by idle.animateFloat(0f, 1f, infiniteRepeatable(tween(1500, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "breath")
    val wag by idle.animateFloat(-5f, 7f, infiniteRepeatable(tween(if (mood == Mood.HAPPY) 260 else 1300), RepeatMode.Reverse), label = "wag")
    val wave by idle.animateFloat(-18f, 14f, infiniteRepeatable(tween(220), RepeatMode.Reverse), label = "wave")

    var blink by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(Random.nextLong(2200, 4600))
            blink = true
            delay(130)
            blink = false
        }
    }
    var mouthOpen by remember { mutableStateOf(false) }
    LaunchedEffect(mood) {
        mouthOpen = false
        while (mood == Mood.TALKING) {
            delay(Random.nextLong(110, 190))
            mouthOpen = !mouthOpen
        }
    }
    val hop = remember { Animatable(0f) }
    LaunchedEffect(mood) {
        if (mood == Mood.HAPPY) repeat(2) {
            hop.animateTo(-0.12f, tween(170, easing = FastOutSlowInEasing))
            hop.animateTo(0f, spring(dampingRatio = 0.45f, stiffness = 900f))
        }
    }
    val tilt by animateFloatAsState(if (mood == Mood.LISTENING) 12f else 0f, label = "tilt")

    Box(modifier.aspectRatio(1f).graphicsLayer { scaleX = if (facingLeft) -1f else 1f }) {
        Layer(parts.shadow, Modifier.graphicsLayer { scaleX = 1f + hop.value * 1.5f })
        Box(Modifier.fillMaxSize().graphicsLayer { translationY = hop.value * size.height }) {
            Layer(parts.tail, Modifier.graphicsLayer { transformOrigin = TAIL_PIVOT; rotationZ = wag })
            Layer(parts.legBack)
            Layer(parts.body, Modifier.graphicsLayer { transformOrigin = FEET; scaleY = 1f + breath * 0.022f })
            Layer(parts.legFront)
            Layer(
                parts.arm,
                Modifier.graphicsLayer {
                    transformOrigin = ARM_PIVOT
                    rotationZ = if (mood == Mood.HAPPY) wave else breath * 6f
                    translationY = -breath * size.height * 0.012f
                },
            )
            val head = Modifier.graphicsLayer {
                transformOrigin = NECK_PIVOT
                rotationZ = tilt + (breath - 0.5f) * 2.5f
                translationY = -breath * size.height * 0.014f
            }
            Layer(parts.head, head)
            Layer(if (blink) parts.eyeClosed else parts.eyeOpen, head)
            parts.lashes?.let { Layer(it, head) }
            val mouth = when {
                mood == Mood.TALKING && mouthOpen -> parts.mouthTalk
                mood == Mood.HAPPY || mood == Mood.TALKING -> parts.mouthHappy
                else -> parts.mouthCalm
            }
            Layer(mouth, head)
        }
    }
}

@Composable
private fun Layer(@DrawableRes id: Int, modifier: Modifier = Modifier) {
    Image(painterResource(id), contentDescription = null, modifier = modifier.fillMaxSize())
}
