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
import com.dinovalley.engine.rpg.hero.HeroClass
import kotlinx.coroutines.delay
import kotlin.random.Random

/** How a painted layer moves. FACE marks where the eyes and mouth are drawn. */
enum class Part { STILL, BODY, HEAD, FACE, TAIL, WING, ARM }

data class Piece(@DrawableRes val res: Int, val part: Part)

/** A layered character: its pieces bottom to top, its faces, and where it bends. */
data class Rig(
    val pieces: List<Piece>,
    @DrawableRes val eyeOpen: Int,
    @DrawableRes val eyeClosed: Int,
    @DrawableRes val mouthCalm: Int,
    @DrawableRes val mouthHappy: Int,
    @DrawableRes val mouthTalk: Int,
    val aspect: Float = 1f,
    val neck: TransformOrigin = TransformOrigin(0.55f, 0.55f),
    val tailRoot: TransformOrigin = TransformOrigin(0.36f, 0.72f),
    val shoulder: TransformOrigin = TransformOrigin(0.62f, 0.6f),
    val wingRoot: TransformOrigin = TransformOrigin(0.45f, 0.43f),
)

enum class Mood { CALM, TALKING, HAPPY, SCARED, LISTENING }

object Rigs {
    val babyDragon = dragon(
        R.drawable.art_dragon_baby_shadow, R.drawable.art_dragon_baby_wing_back, R.drawable.art_dragon_baby_tail,
        R.drawable.art_dragon_baby_leg_back, R.drawable.art_dragon_baby_body, R.drawable.art_dragon_baby_leg_front,
        R.drawable.art_dragon_baby_wing_front, R.drawable.art_dragon_baby_arm, R.drawable.art_dragon_baby_head,
        R.drawable.art_dragon_baby_eye_open, R.drawable.art_dragon_baby_eye_closed, R.drawable.art_dragon_baby_mouth_calm,
        R.drawable.art_dragon_baby_mouth_happy, R.drawable.art_dragon_baby_mouth_talk,
    )

    val bigDragon = dragon(
        R.drawable.art_dragon_big_shadow, R.drawable.art_dragon_big_wing_back, R.drawable.art_dragon_big_tail,
        R.drawable.art_dragon_big_leg_back, R.drawable.art_dragon_big_body, R.drawable.art_dragon_big_leg_front,
        R.drawable.art_dragon_big_wing_front, R.drawable.art_dragon_big_arm, R.drawable.art_dragon_big_head,
        R.drawable.art_dragon_big_eye_open, R.drawable.art_dragon_big_eye_closed, R.drawable.art_dragon_big_mouth_calm,
        R.drawable.art_dragon_big_mouth_happy, R.drawable.art_dragon_big_mouth_talk,
    )

    private fun dragon(
        shadow: Int, wingBack: Int, tail: Int, legBack: Int, body: Int, legFront: Int, wingFront: Int, arm: Int, head: Int,
        eyeOpen: Int, eyeClosed: Int, mouthCalm: Int, mouthHappy: Int, mouthTalk: Int,
    ) = Rig(
        listOf(
            Piece(shadow, Part.STILL), Piece(wingBack, Part.WING), Piece(tail, Part.TAIL), Piece(legBack, Part.STILL),
            Piece(body, Part.BODY), Piece(legFront, Part.STILL), Piece(wingFront, Part.WING), Piece(arm, Part.ARM),
            Piece(head, Part.HEAD), Piece(0, Part.FACE),
        ),
        eyeOpen, eyeClosed, mouthCalm, mouthHappy, mouthTalk,
        neck = TransformOrigin(0.6f, 0.55f), shoulder = TransformOrigin(0.64f, 0.6f),
    )

    /** The hero for a class, wearing what they've unlocked. */
    fun hero(heroClass: HeroClass, unlocked: Set<String>): Rig {
        val k = kid(heroClass)
        val pieces = buildList {
            add(Piece(k.shadow, Part.STILL))
            if ("star_cape" in unlocked) add(Piece(k.cape, Part.BODY))
            add(Piece(k.legs, Part.STILL))
            add(Piece(k.armBack, Part.BODY))
            add(Piece(k.body, Part.BODY))
            add(Piece(k.head, Part.HEAD))
            add(Piece(0, Part.FACE))
            add(Piece(k.hat, Part.HEAD))
            if ("feather_hat" in unlocked) add(Piece(k.feather, Part.HEAD))
            add(Piece(k.gear, Part.ARM))
            add(Piece(k.armFront, Part.ARM))
        }
        return Rig(pieces, k.eyeOpen, k.eyeClosed, k.mouthCalm, k.mouthHappy, k.mouthTalk, neck = TransformOrigin(0.55f, 0.55f), shoulder = TransformOrigin(0.6f, 0.6f))
    }

    val ruby: Rig = run {
        val r = Kid(
            R.drawable.art_ruby_shadow, R.drawable.art_ruby_cape, R.drawable.art_ruby_legs, R.drawable.art_ruby_arm_back,
            R.drawable.art_ruby_body, R.drawable.art_ruby_head, R.drawable.art_ruby_eye_open, R.drawable.art_ruby_eye_closed,
            R.drawable.art_ruby_mouth_calm, R.drawable.art_ruby_mouth_happy, R.drawable.art_ruby_mouth_talk,
            R.drawable.art_ruby_hat, R.drawable.art_ruby_feather, R.drawable.art_ruby_gear, R.drawable.art_ruby_arm_front,
        )
        Rig(
            listOf(
                Piece(r.shadow, Part.STILL), Piece(r.cape, Part.BODY), Piece(r.legs, Part.STILL), Piece(r.armBack, Part.BODY),
                Piece(r.body, Part.BODY), Piece(r.head, Part.HEAD), Piece(0, Part.FACE), Piece(r.hat, Part.HEAD), Piece(r.armFront, Part.ARM),
            ),
            r.eyeOpen, r.eyeClosed, r.mouthCalm, r.mouthHappy, r.mouthTalk,
        )
    }

    val goblin = Rig(
        listOf(
            Piece(R.drawable.art_goblin_shadow, Part.STILL), Piece(R.drawable.art_goblin_body, Part.BODY),
            Piece(R.drawable.art_goblin_head, Part.HEAD), Piece(0, Part.FACE),
        ),
        R.drawable.art_goblin_eye_open, R.drawable.art_goblin_eye_closed, R.drawable.art_goblin_mouth_scared,
        R.drawable.art_goblin_mouth_happy, R.drawable.art_goblin_mouth_talk,
        neck = TransformOrigin(0.5f, 0.6f),
    )

    val wizard = Rig(
        listOf(Piece(R.drawable.art_wizard_shadow, Part.STILL), Piece(R.drawable.art_wizard_body, Part.HEAD), Piece(0, Part.FACE)),
        R.drawable.art_wizard_eye_open, R.drawable.art_wizard_eye_closed, R.drawable.art_wizard_mouth_calm,
        R.drawable.art_wizard_mouth_calm, R.drawable.art_wizard_mouth_talk,
        aspect = 240f / 260f, neck = TransformOrigin(0.5f, 0.95f),
    )

    val shadow = Rig(
        listOf(Piece(R.drawable.art_shadow_shadow, Part.STILL), Piece(R.drawable.art_shadow_body, Part.HEAD), Piece(0, Part.FACE)),
        R.drawable.art_shadow_eye_open, R.drawable.art_shadow_eye_closed, R.drawable.art_shadow_mouth_calm,
        R.drawable.art_shadow_mouth_calm, R.drawable.art_shadow_mouth_talk,
        neck = TransformOrigin(0.5f, 0.95f),
    )

    private class Kid(
        val shadow: Int, val cape: Int, val legs: Int, val armBack: Int, val body: Int, val head: Int,
        val eyeOpen: Int, val eyeClosed: Int, val mouthCalm: Int, val mouthHappy: Int, val mouthTalk: Int,
        val hat: Int, val feather: Int, val gear: Int, val armFront: Int,
    )

    private fun kid(c: HeroClass): Kid = when (c) {
        HeroClass.KNIGHT -> Kid(
            R.drawable.art_hero_knight_shadow, R.drawable.art_hero_knight_cape, R.drawable.art_hero_knight_legs, R.drawable.art_hero_knight_arm_back,
            R.drawable.art_hero_knight_body, R.drawable.art_hero_knight_head, R.drawable.art_hero_knight_eye_open, R.drawable.art_hero_knight_eye_closed,
            R.drawable.art_hero_knight_mouth_calm, R.drawable.art_hero_knight_mouth_happy, R.drawable.art_hero_knight_mouth_talk,
            R.drawable.art_hero_knight_hat, R.drawable.art_hero_knight_feather, R.drawable.art_hero_knight_gear, R.drawable.art_hero_knight_arm_front,
        )
        HeroClass.WIZARD -> Kid(
            R.drawable.art_hero_wizard_shadow, R.drawable.art_hero_wizard_cape, R.drawable.art_hero_wizard_legs, R.drawable.art_hero_wizard_arm_back,
            R.drawable.art_hero_wizard_body, R.drawable.art_hero_wizard_head, R.drawable.art_hero_wizard_eye_open, R.drawable.art_hero_wizard_eye_closed,
            R.drawable.art_hero_wizard_mouth_calm, R.drawable.art_hero_wizard_mouth_happy, R.drawable.art_hero_wizard_mouth_talk,
            R.drawable.art_hero_wizard_hat, R.drawable.art_hero_wizard_feather, R.drawable.art_hero_wizard_gear, R.drawable.art_hero_wizard_arm_front,
        )
        HeroClass.RANGER -> Kid(
            R.drawable.art_hero_ranger_shadow, R.drawable.art_hero_ranger_cape, R.drawable.art_hero_ranger_legs, R.drawable.art_hero_ranger_arm_back,
            R.drawable.art_hero_ranger_body, R.drawable.art_hero_ranger_head, R.drawable.art_hero_ranger_eye_open, R.drawable.art_hero_ranger_eye_closed,
            R.drawable.art_hero_ranger_mouth_calm, R.drawable.art_hero_ranger_mouth_happy, R.drawable.art_hero_ranger_mouth_talk,
            R.drawable.art_hero_ranger_hat, R.drawable.art_hero_ranger_feather, R.drawable.art_hero_ranger_gear, R.drawable.art_hero_ranger_arm_front,
        )
        HeroClass.GUARDIAN -> Kid(
            R.drawable.art_hero_guardian_shadow, R.drawable.art_hero_guardian_cape, R.drawable.art_hero_guardian_legs, R.drawable.art_hero_guardian_arm_back,
            R.drawable.art_hero_guardian_body, R.drawable.art_hero_guardian_head, R.drawable.art_hero_guardian_eye_open, R.drawable.art_hero_guardian_eye_closed,
            R.drawable.art_hero_guardian_mouth_calm, R.drawable.art_hero_guardian_mouth_happy, R.drawable.art_hero_guardian_mouth_talk,
            R.drawable.art_hero_guardian_hat, R.drawable.art_hero_guardian_feather, R.drawable.art_hero_guardian_gear, R.drawable.art_hero_guardian_arm_front,
        )
        HeroClass.SPELLKEEPER -> Kid(
            R.drawable.art_hero_spellkeeper_shadow, R.drawable.art_hero_spellkeeper_cape, R.drawable.art_hero_spellkeeper_legs, R.drawable.art_hero_spellkeeper_arm_back,
            R.drawable.art_hero_spellkeeper_body, R.drawable.art_hero_spellkeeper_head, R.drawable.art_hero_spellkeeper_eye_open, R.drawable.art_hero_spellkeeper_eye_closed,
            R.drawable.art_hero_spellkeeper_mouth_calm, R.drawable.art_hero_spellkeeper_mouth_happy, R.drawable.art_hero_spellkeeper_mouth_talk,
            R.drawable.art_hero_spellkeeper_hat, R.drawable.art_hero_spellkeeper_feather, R.drawable.art_hero_spellkeeper_gear, R.drawable.art_hero_spellkeeper_arm_front,
        )
    }
}

/**
 * Draws a [Rig] and brings it to life: breathing, blinking, a wagging tail, flapping wings,
 * a bobbing head, a talking mouth, and a hop when happy. With [voice], the mouth opens with the
 * loudness of the words being heard, so it moves with the sound. [animate] false draws it still
 * (small pictures, like the hero cards), which saves the phone work.
 */
@Composable
fun Character(
    rig: Rig,
    mood: Mood,
    modifier: Modifier = Modifier,
    facingLeft: Boolean = false,
    flap: Boolean = true,
    voice: (() -> Float)? = null,
    animate: Boolean = true,
) {
    if (!animate) {
        StillCharacter(rig, mood, modifier, facingLeft)
        return
    }
    val idle = rememberInfiniteTransition(label = "idle")
    val breath by idle.animateFloat(0f, 1f, infiniteRepeatable(tween(1500, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "breath")
    val wag by idle.animateFloat(-5f, 7f, infiniteRepeatable(tween(if (mood == Mood.HAPPY) 260 else 1300), RepeatMode.Reverse), label = "wag")
    val wing by idle.animateFloat(-8f, 10f, infiniteRepeatable(tween(if (mood == Mood.HAPPY) 180 else 900), RepeatMode.Reverse), label = "wing")
    val wave by idle.animateFloat(-14f, 10f, infiniteRepeatable(tween(240), RepeatMode.Reverse), label = "wave")
    val shiver by idle.animateFloat(-1f, 1f, infiniteRepeatable(tween(70), RepeatMode.Reverse), label = "shiver")

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
    LaunchedEffect(mood, voice) {
        mouthOpen = false
        while (mood == Mood.TALKING) {
            if (voice != null) {
                delay(40)
                mouthOpen = voice() > 0.3f
            } else {
                delay(Random.nextLong(110, 190))
                mouthOpen = !mouthOpen
            }
        }
    }
    val hop = remember { Animatable(0f) }
    LaunchedEffect(mood) {
        if (mood == Mood.HAPPY) repeat(2) {
            hop.animateTo(-0.1f, tween(170, easing = FastOutSlowInEasing))
            hop.animateTo(0f, spring(dampingRatio = 0.45f, stiffness = 900f))
        }
    }
    val tilt by animateFloatAsState(if (mood == Mood.LISTENING) 10f else 0f, label = "tilt")

    Box(modifier.aspectRatio(rig.aspect).graphicsLayer { scaleX = if (facingLeft) -1f else 1f }) {
        Box(
            Modifier.fillMaxSize().graphicsLayer {
                translationY = hop.value * size.height
                translationX = if (mood == Mood.SCARED) shiver * size.width * 0.006f else 0f
            },
        ) {
            for (piece in rig.pieces) {
                val head = Modifier.graphicsLayer {
                    transformOrigin = rig.neck
                    rotationZ = tilt + (breath - 0.5f) * 2.5f
                    translationY = -breath * size.height * 0.012f
                }
                when (piece.part) {
                    Part.STILL -> Layer(piece.res)
                    Part.BODY -> Layer(piece.res, Modifier.graphicsLayer { transformOrigin = TransformOrigin(0.5f, 0.95f); scaleY = 1f + breath * 0.02f })
                    Part.HEAD -> Layer(piece.res, head)
                    Part.TAIL -> Layer(piece.res, Modifier.graphicsLayer { transformOrigin = rig.tailRoot; rotationZ = wag })
                    Part.WING -> Layer(piece.res, Modifier.graphicsLayer { transformOrigin = rig.wingRoot; rotationZ = if (flap) wing else 0f })
                    Part.ARM -> Layer(
                        piece.res,
                        Modifier.graphicsLayer {
                            transformOrigin = rig.shoulder
                            rotationZ = if (mood == Mood.HAPPY) wave else breath * 4f
                            translationY = -breath * size.height * 0.01f
                        },
                    )
                    Part.FACE -> {
                        Layer(if (blink) rig.eyeClosed else rig.eyeOpen, head)
                        val mouth = when {
                            mood == Mood.TALKING && mouthOpen -> rig.mouthTalk
                            mood == Mood.HAPPY || mood == Mood.TALKING -> rig.mouthHappy
                            else -> rig.mouthCalm
                        }
                        Layer(mouth, head)
                    }
                }
            }
        }
    }
}

@Composable
private fun Layer(@DrawableRes id: Int, modifier: Modifier = Modifier) {
    Image(painterResource(id), contentDescription = null, modifier = modifier.fillMaxSize())
}

/** A character standing still: the same layers, no animation. */
@Composable
private fun StillCharacter(rig: Rig, mood: Mood, modifier: Modifier, facingLeft: Boolean) {
    Box(modifier.aspectRatio(rig.aspect).graphicsLayer { scaleX = if (facingLeft) -1f else 1f }) {
        for (piece in rig.pieces) {
            if (piece.part == Part.FACE) {
                Layer(rig.eyeOpen)
                Layer(if (mood == Mood.HAPPY) rig.mouthHappy else rig.mouthCalm)
            } else {
                Layer(piece.res)
            }
        }
    }
}
