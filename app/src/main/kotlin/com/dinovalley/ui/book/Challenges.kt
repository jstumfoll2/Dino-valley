package com.dinovalley.ui.book

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dinovalley.R
import com.dinovalley.engine.activity.count.CountObjectsEvaluator
import com.dinovalley.engine.model.ChildResponse
import com.dinovalley.engine.model.Hint
import com.dinovalley.engine.story.Challenge
import com.dinovalley.engine.story.ChoiceJudge
import com.dinovalley.engine.story.LostEggsStory
import com.dinovalley.ui.theme.DinoColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private fun word(n: Int) = LostEggsStory.numberWord(n)

private val noRipple = MutableInteractionSource()

/** Shakes side to side each time [key] changes. */
@Composable
private fun rememberShake(key: Any?): Animatable<Float, *> {
    val x = remember { Animatable(0f) }
    LaunchedEffect(key) {
        if (key != null && key != 0) for (v in listOf(-14f, 12f, -9f, 6f, 0f)) x.animateTo(v, tween(60))
    }
    return x
}

// ---------------------------------------------------------------- storm

@Composable
fun TapClouds(c: Challenge.TapClouds, enabled: Boolean, onDone: (Boolean) -> Unit) {
    val narrator = LocalNarrator.current
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val struck = remember { mutableStateListOf<Int>() }
    val flash = remember { Animatable(0f) }
    var bolt by remember { mutableIntStateOf(-1) }
    val spots = listOf(0.44f to 0.44f, 0.64f to 0.36f, 0.84f to 0.46f).take(c.taps)

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val w = maxWidth
        val h = maxHeight
        spots.forEachIndexed { i, (x, y) ->
            val cw = h * 0.42f
            val ch = h * 0.24f
            if (bolt == i) Bolt(Modifier.at(w * x, h * (y + 0.24f), h * 0.14f, h * 0.3f))
            Cloud(
                lit = i in struck,
                pulse = enabled && i !in struck,
                modifier = Modifier.at(w * x, h * y, cw, ch).clickable(noRipple, null, enabled = enabled) {
                    if (i !in struck) struck += i
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    narrator.blurt("Boom!")
                    scope.launch {
                        bolt = i
                        flash.snapTo(0.75f)
                        flash.animateTo(0f, tween(450))
                        bolt = -1
                        if (struck.size == spots.size) {
                            delay(400)
                            onDone(true)
                        }
                    }
                },
            )
        }
        Box(Modifier.fillMaxSize().alpha(flash.value).background(Color.White))
    }
}

@Composable
private fun Cloud(lit: Boolean, pulse: Boolean, modifier: Modifier) {
    val t = rememberInfiniteTransition(label = "cloud")
    val bob by t.animateFloat(-1f, 1f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "bob")
    val color = if (lit) Color(0xFF8C8FC8) else Color(0xFF55577F)
    Canvas(modifier.graphicsLayer { translationY = if (pulse) bob * 8f else 0f }) {
        val w = size.width
        val hh = size.height
        val puffs = listOf(0.22f to 0.6f, 0.4f to 0.4f, 0.6f to 0.36f, 0.78f to 0.58f, 0.5f to 0.66f)
        for ((x, y) in puffs) drawCircle(Color(0xFF2E2F4F), hh * 0.36f, Offset(w * x, hh * y + 6f))
        for ((x, y) in puffs) drawCircle(color, hh * 0.34f, Offset(w * x, hh * y))
        drawCircle(Color.White.copy(alpha = 0.18f), hh * 0.2f, Offset(w * 0.42f, hh * 0.3f))
    }
}

@Composable
private fun Bolt(modifier: Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val p = Path().apply {
            moveTo(w * 0.55f, 0f); lineTo(w * 0.1f, h * 0.55f); lineTo(w * 0.5f, h * 0.55f)
            lineTo(w * 0.3f, h); lineTo(w * 0.95f, h * 0.38f); lineTo(w * 0.55f, h * 0.38f); lineTo(w * 0.8f, 0f); close()
        }
        drawPath(p, Color(0xFFFFE45C))
    }
}

// ---------------------------------------------------------------- counting

@Composable
fun CountEggs(c: Challenge.CountEggs, enabled: Boolean, onDone: (Boolean) -> Unit) {
    val q = c.question
    val narrator = LocalNarrator.current
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    var tries by remember { mutableIntStateOf(0) }
    var keep by remember { mutableStateOf<List<Int>?>(null) }
    val counted = remember { mutableStateMapOf<Int, Int>() }
    var highlight by remember { mutableIntStateOf(-1) }
    var wiggle by remember { mutableIntStateOf(0) }
    var wrong by remember { mutableStateOf<Int?>(null) }
    var solved by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    val lit = remember { mutableStateListOf<Int>() }
    val lightsOn = !c.inTheDark || lit.size >= MUSHROOMS
    val countable = q.scene.objects.indices.filter { q.scene.objects[it].countable }

    LaunchedEffect(lightsOn) {
        if (lightsOn && c.inTheDark) narrator.speak("Now it's bright! How many eggs are in the cave?")
    }

    fun choose(n: Int) {
        if (busy || solved) return
        tries += 1
        val result = CountObjectsEvaluator.evaluate(q, ChildResponse.NumberChosen(n), tries)
        if (result.correct) {
            solved = true
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            scope.launch {
                narrator.speak("Yes!")
                onDone(tries == 1)
            }
            return
        }
        busy = true
        wrong = n
        scope.launch {
            when (val hint = result.nextHint) {
                Hint.TryAgain -> {
                    wiggle += 1
                    narrator.speak("Hmm, that's ${word(n)}. Let's look again!")
                }
                Hint.CountTogether -> {
                    narrator.speak("Let's count them together!")
                    counted.clear()
                    countable.forEachIndexed { k, idx ->
                        highlight = idx
                        counted[idx] = k + 1
                        narrator.speak(word(k + 1))
                    }
                    highlight = -1
                    narrator.speak("So how many eggs?")
                }
                is Hint.NarrowChoices -> {
                    keep = hint.keep
                    narrator.speak("Is it ${word(hint.keep.first())}, or ${word(hint.keep.last())}?")
                }
                null -> Unit
            }
            wrong = null
            busy = false
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val w = maxWidth
        val h = maxHeight
        q.scene.objects.forEachIndexed { i, o ->
            val eh = h * 0.21f * o.scale
            val ew = eh * (200f / 260f)
            EggSprite(
                art = eggArt(o.sprite.value),
                number = counted[i],
                glow = highlight == i || (solved && o.countable),
                wiggleKey = wiggle,
                modifier = Modifier
                    .at(w * (0.37f + 0.57f * o.x), h * (0.33f + 0.4f * o.y), ew, eh)
                    .clickable(noRipple, null, enabled = enabled && lightsOn && !solved && !busy && o.countable) {
                        if (i !in counted) {
                            counted[i] = counted.size + 1
                            narrator.blurt(word(counted.getValue(i)))
                        }
                    },
            )
        }
        if (c.inTheDark) {
            val dark by animateFloatAsState(0.88f * (1f - lit.size / MUSHROOMS.toFloat()), tween(700), label = "dark")
            Box(Modifier.fillMaxSize().alpha(dark).background(Color(0xFF07061A)))
            if (!lightsOn) {
                listOf(0.42f, 0.64f, 0.86f).forEachIndexed { k, x ->
                    Mushroom(
                        on = k in lit,
                        modifier = Modifier.at(w * x, h * 0.84f, h * 0.2f, h * 0.225f).clickable(noRipple, null, enabled = enabled) {
                            if (k !in lit) {
                                lit += k
                                narrator.blurt(listOf("Glow!", "Shine!", "Sparkle!")[k])
                            }
                        },
                    )
                }
            }
        }
        if (lightsOn) {
            Row(
                Modifier.align(Alignment.BottomCenter).offset(x = w * 0.13f, y = -h * 0.03f),
                horizontalArrangement = Arrangement.spacedBy(h * 0.04f),
            ) {
                q.choices.forEach { n ->
                    NumberCard(
                        n,
                        size = h * 0.2f,
                        visible = keep?.contains(n) ?: true,
                        wrong = wrong == n,
                        correct = solved && n == q.answer,
                        enabled = enabled && !busy,
                        onClick = { choose(n) },
                    )
                }
            }
        }
    }
}

private const val MUSHROOMS = 3

@Composable
private fun EggSprite(@DrawableRes art: Int, number: Int?, glow: Boolean, wiggleKey: Int, modifier: Modifier) {
    val rot = remember { Animatable(0f) }
    LaunchedEffect(wiggleKey) {
        if (wiggleKey > 0) for (v in listOf(-10f, 9f, -7f, 5f, 0f)) rot.animateTo(v, tween(90))
    }
    val pop by animateFloatAsState(if (glow) 1.12f else 1f, spring(dampingRatio = 0.4f), label = "pop")
    Box(
        modifier.graphicsLayer {
            transformOrigin = TransformOrigin(0.5f, 0.95f)
            rotationZ = rot.value
            scaleX = pop
            scaleY = pop
        },
    ) {
        if (glow) Box(Modifier.fillMaxSize().background(DinoColors.Highlight.copy(alpha = 0.35f), CircleShape))
        Image(painterResource(art), null, Modifier.fillMaxSize())
        if (number != null) {
            Box(
                Modifier.align(Alignment.Center).size(34.dp).background(Color.White, CircleShape).border(3.dp, DinoColors.Highlight, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(number.toString(), fontWeight = FontWeight.Black, color = DinoColors.Ink, fontSize = 20.sp)
            }
        }
    }
}

@Composable
private fun Mushroom(on: Boolean, modifier: Modifier) {
    val t = rememberInfiniteTransition(label = "mushroom")
    val beat by t.animateFloat(0.92f, 1.06f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "beat")
    Box(modifier.graphicsLayer { val s = if (on) 1f else beat; scaleX = s; scaleY = s }) {
        if (on) Image(painterResource(R.drawable.art_mushroom_glow), null, Modifier.fillMaxSize().graphicsLayer { scaleX = 1.8f; scaleY = 1.8f })
        Image(painterResource(R.drawable.art_mushroom_all), null, Modifier.fillMaxSize().alpha(if (on) 1f else 0.75f))
    }
}

/** A numeral with the same number of dots under it, so the symbol and the amount go together. */
@Composable
private fun NumberCard(n: Int, size: Dp, visible: Boolean, wrong: Boolean, correct: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val shake = rememberShake(if (wrong) n else null)
    val fade by animateFloatAsState(if (visible) 1f else 0.2f, label = "fade")
    val ring = if (correct) DinoColors.Correct else Color(0xFFE9DFC4)
    Column(
        Modifier
            .graphicsLayer { translationX = shake.value; alpha = fade; val s = if (correct) 1.12f else 1f; scaleX = s; scaleY = s }
            .size(size)
            .shadow(6.dp, CircleShape)
            .background(DinoColors.Card, CircleShape)
            .border(5.dp, ring, CircleShape)
            .clickable(enabled = enabled && visible, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        val fs = with(LocalDensity.current) { (size * 0.42f).toSp() }
        Text(n.toString(), fontSize = fs, lineHeight = fs, fontWeight = FontWeight.Black, color = DinoColors.Ink)
        Dots(n, size * 0.07f)
    }
}

@Composable
private fun Dots(count: Int, dot: Dp) {
    val perRow = 5
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        for (row in 0 until (count + perRow - 1) / perRow) {
            Row(horizontalArrangement = Arrangement.spacedBy(dot * 0.4f)) {
                repeat(minOf(perRow, count - row * perRow)) {
                    Box(Modifier.size(dot).background(DinoColors.Lava, CircleShape))
                }
            }
            Spacer(Modifier.height(dot * 0.3f))
        }
    }
}

// ---------------------------------------------------------------- numerals: river stones

@Composable
fun FindNumeral(c: Challenge.FindNumeral, enabled: Boolean, onDone: (Boolean) -> Unit) {
    val narrator = LocalNarrator.current
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    var tries by remember { mutableIntStateOf(0) }
    var keep by remember { mutableStateOf<List<Int>?>(null) }
    var wrong by remember { mutableStateOf<Int?>(null) }
    var solved by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val w = maxWidth
        val h = maxHeight
        val egg = remember { Animatable(0f) }
        LaunchedEffect(solved) {
            if (solved) egg.animateTo(1f, tween(900, easing = FastOutSlowInEasing))
        }
        // The egg waits on the far bank, then hops over to the dino.
        Image(
            painterResource(R.drawable.art_egg_orange_whole), null,
            Modifier.at(w * (0.9f - 0.62f * egg.value), h * (0.5f + 0.2f * egg.value) - h * 0.25f * (1f - (2f * egg.value - 1f).let { it * it }), h * 0.13f, h * 0.17f),
        )
        val n = c.choices.size
        val stoneW = minOf(h * 0.3f, w * 0.52f / n)
        c.choices.forEachIndexed { i, value ->
            val x = w * (0.4f + 0.52f * (i + 0.5f) / n)
            val shake = rememberShake(if (wrong == value) value else null)
            val visible = keep?.contains(value) ?: true
            val fade by animateFloatAsState(if (visible) 1f else 0.25f, label = "fade")
            val right = solved && value == c.target
            val lift by animateFloatAsState(if (right) -0.04f else 0f, spring(dampingRatio = 0.35f), label = "lift")
            Box(
                Modifier
                    .at(x, h * 0.84f, stoneW, stoneW * 0.625f)
                    .graphicsLayer { translationX = shake.value; alpha = fade; translationY = lift * h.toPx() }
                    .clickable(noRipple, null, enabled = enabled && !busy && !solved && visible) {
                        tries += 1
                        val verdict = ChoiceJudge.judge(c.choices, c.target, value, tries)
                        if (verdict.correct) {
                            solved = true
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            scope.launch {
                                narrator.speak("Yes! ${word(value)}!")
                                delay(400)
                                onDone(tries == 1)
                            }
                        } else {
                            busy = true
                            wrong = value
                            scope.launch {
                                narrator.speak("That's ${word(value)}. Find ${word(c.target)}!")
                                verdict.keep?.let { keep = it }
                                wrong = null
                                busy = false
                            }
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Image(painterResource(R.drawable.art_stone), null, Modifier.fillMaxSize())
                val fs = with(LocalDensity.current) { (stoneW * 0.34f).toSp() }
                Text(
                    value.toString(),
                    fontSize = fs, lineHeight = fs, fontWeight = FontWeight.Black,
                    color = if (right) DinoColors.Correct else Color(0xFF3B3A36),
                    modifier = Modifier.offset(y = -stoneW * 0.04f),
                )
            }
        }
    }
}

// ---------------------------------------------------------------- letters: forest bushes

@Composable
fun FindLetter(c: Challenge.FindLetter, enabled: Boolean, onDone: (Boolean) -> Unit) {
    val narrator = LocalNarrator.current
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    var tries by remember { mutableIntStateOf(0) }
    var keep by remember { mutableStateOf<List<Char>?>(null) }
    var wrong by remember { mutableStateOf<Char?>(null) }
    var solved by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val w = maxWidth
        val h = maxHeight
        val n = c.choices.size
        val bushW = minOf(h * 0.44f, w * 0.56f / n)
        val bushH = bushW * 0.65f
        c.choices.forEachIndexed { i, letter ->
            val x = w * (0.39f + 0.55f * (i + 0.5f) / n)
            val y = h * 0.72f
            val shake = rememberShake(if (wrong == letter) letter else null)
            val visible = keep?.contains(letter) ?: true
            val fade by animateFloatAsState(if (visible) 1f else 0.3f, label = "fade")
            val found = solved && letter == c.target
            val peek by animateFloatAsState(if (found) 1f else 0f, spring(dampingRatio = 0.45f, stiffness = 300f), label = "peek")
            Image(
                painterResource(R.drawable.art_egg_green_whole), null,
                Modifier.at(x, y - bushH * 0.55f * peek, bushH * 0.42f, bushH * 0.55f),
            )
            Box(
                Modifier
                    .at(x, y, bushW, bushH)
                    .graphicsLayer { translationX = shake.value; alpha = fade; rotationZ = if (found) (1f - peek) * 6f else 0f }
                    .clickable(noRipple, null, enabled = enabled && !busy && !solved && visible) {
                        tries += 1
                        val verdict = ChoiceJudge.judge(c.choices, c.target, letter, tries)
                        if (verdict.correct) {
                            solved = true
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            scope.launch {
                                narrator.speak("Yes! $letter!")
                                delay(500)
                                onDone(tries == 1)
                            }
                        } else {
                            busy = true
                            wrong = letter
                            val itsWord = LostEggsStory.LETTER_WORDS.getValue(letter)
                            scope.launch {
                                narrator.speak("That's $letter, as in $itsWord. Find ${c.target}, as in ${c.word}!")
                                verdict.keep?.let { keep = it }
                                wrong = null
                                busy = false
                            }
                        }
                    },
            ) {
                Image(painterResource(R.drawable.art_bush), null, Modifier.fillMaxSize())
                val sign = bushH * 0.5f
                Box(
                    Modifier.align(Alignment.Center).size(sign)
                        .shadow(4.dp, CircleShape)
                        .background(BookColors.Paper, CircleShape)
                        .border(4.dp, if (found) DinoColors.Correct else BookColors.PaperEdge, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    val fs = with(LocalDensity.current) { (sign * 0.62f).toSp() }
                    Text(letter.toString(), fontSize = fs, lineHeight = fs, fontWeight = FontWeight.Black, color = BookColors.Ink)
                }
            }
        }
    }
}

// ---------------------------------------------------------------- hatching

@Composable
fun HatchEggs(c: Challenge.HatchEggs, enabled: Boolean, onDone: (Boolean) -> Unit) {
    val narrator = LocalNarrator.current
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val hatched = remember { mutableStateListOf<Int>() }
    val colors = listOf("blue", "green", "orange")

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val w = maxWidth
        val h = maxHeight
        val cx = w * 0.56f
        val nestW = h * 0.78f
        val nestH = nestW * 0.45f
        val nestY = h * 0.8f
        Image(painterResource(R.drawable.art_nest_back), null, Modifier.at(cx, nestY, nestW, nestH))
        repeat(c.eggs) { i ->
            val x = cx + nestW * (i - (c.eggs - 1) / 2f) * 0.25f
            val eh = h * 0.26f
            val ew = eh * (200f / 260f)
            val y = nestY - nestH * 0.32f
            val open = i in hatched
            val fly by animateFloatAsState(if (open) 1f else 0f, tween(700), label = "fly")
            val color = colors[i % colors.size]
            AnimatedVisibility(open, enter = scaleIn(spring(dampingRatio = 0.4f)) + fadeIn(), exit = fadeOut(), modifier = Modifier.at(x, y - eh * 0.28f, eh * 0.8f, eh * 0.8f)) {
                Image(painterResource(R.drawable.art_baby), null, Modifier.fillMaxSize())
            }
            val t = rememberInfiniteTransition(label = "egg$i")
            val jiggle by t.animateFloat(-3f, 3f, infiniteRepeatable(tween(380), RepeatMode.Reverse), label = "jiggle")
            val wobble = if (!open && enabled) jiggle else 0f
            Image(
                painterResource(eggPart(color, top = false)), null,
                Modifier.at(x, y, ew, eh).graphicsLayer { transformOrigin = TransformOrigin(0.5f, 0.95f); rotationZ = wobble },
            )
            Image(
                painterResource(eggPart(color, top = true)), null,
                Modifier.at(x, y, ew, eh).graphicsLayer {
                    transformOrigin = TransformOrigin(0.5f, 0.95f)
                    translationY = -fly * eh.toPx() * 0.9f
                    translationX = fly * ew.toPx() * (if (i % 2 == 0) -0.6f else 0.6f)
                    rotationZ = wobble + fly * (if (i % 2 == 0) -50f else 50f)
                    alpha = 1f - fly
                },
            )
            Box(
                Modifier.at(x, y, ew, eh)
                    .clickable(noRipple, null, enabled = enabled && !open) {
                        hatched += i
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        narrator.blurt("Crack!")
                        if (hatched.size == c.eggs) scope.launch {
                            delay(900)
                            onDone(true)
                        }
                    },
            )
        }
        Image(painterResource(R.drawable.art_nest_front), null, Modifier.at(cx, nestY, nestW, nestH))
    }
}

@DrawableRes
private fun eggPart(color: String, top: Boolean): Int = when (color) {
    "green" -> if (top) R.drawable.art_egg_green_top else R.drawable.art_egg_green_bottom
    "orange" -> if (top) R.drawable.art_egg_orange_top else R.drawable.art_egg_orange_bottom
    else -> if (top) R.drawable.art_egg_blue_top else R.drawable.art_egg_blue_bottom
}
