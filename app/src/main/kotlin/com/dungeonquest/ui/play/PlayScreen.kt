package com.dungeonquest.ui.play

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dungeonquest.audio.Narrator
import com.dungeonquest.engine.model.ChildResponse
import com.dungeonquest.engine.model.CountObjectsInstance
import com.dungeonquest.engine.model.Hint
import com.dungeonquest.engine.session.SessionEvent
import com.dungeonquest.engine.session.SessionState
import com.dungeonquest.ui.theme.DinoColors
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/**
 * The counting game. Every visual and spoken beat is driven by the engine's [SessionState];
 * this screen never decides whether an answer is right.
 */
@Composable
fun PlayScreen(
    state: SessionState,
    onEvent: (SessionEvent) -> Unit,
    onPlayAgain: () -> Unit,
    narrator: Narrator,
) {
    val item: CountObjectsInstance? = state.currentItem()
    // Egg index -> the number shown above it. Reset for every new question.
    val labels = remember(item?.seed) { mutableStateMapOf<Int, Int>() }
    var highlighted by remember(item?.seed) { mutableIntStateOf(-1) }

    suspend fun countTogether(question: CountObjectsInstance) {
        labels.clear()
        question.scene.objects.withIndex().filter { it.value.countable }.forEachIndexed { n, egg ->
            highlighted = egg.index
            labels[egg.index] = n + 1
            narrator.say(Narrator.numberWord(n + 1))
            delay(800)
        }
        highlighted = -1
    }

    LaunchedEffect(state) {
        when (state) {
            is SessionState.Introducing -> {
                delay(900)
                if (state.demonstrate) {
                    narrator.say("Mama Dino's eggs rolled away! Let's count them together.")
                    delay(3200)
                    countTogether(state.item)
                    delay(400)
                } else {
                    narrator.say("More eggs! Tap each one to count.")
                    delay(2200)
                }
                narrator.say("How many eggs?")
                delay(1000)
                onEvent(SessionEvent.IntroFinished)
            }
            is SessionState.Helping -> {
                when (val hint = state.hint) {
                    Hint.TryAgain -> {
                        narrator.say("Hmm, let's look again!")
                        delay(1800)
                    }
                    Hint.CountTogether -> {
                        narrator.say("Let's count together!")
                        delay(1600)
                        countTogether(state.item)
                        narrator.say("How many eggs?")
                        delay(1200)
                    }
                    is Hint.NarrowChoices -> {
                        narrator.say("Is it ${Narrator.numberWord(hint.keep[0])}, or ${Narrator.numberWord(hint.keep[1])}?")
                        delay(2400)
                    }
                }
                onEvent(SessionEvent.HintShown)
            }
            is SessionState.Celebrating -> {
                narrator.say("Yes! ${Narrator.numberWord(state.item.answer)} eggs! Look, baby dinos!")
                delay(3200)
                onEvent(SessionEvent.CelebrationFinished)
            }
            is SessionState.RoundComplete -> narrator.say("You did it! You found all of Mama Dino's eggs! Tap the big egg to play again.")
            is SessionState.AwaitingAnswer -> Unit
        }
    }

    val mood = when (state) {
        is SessionState.Celebrating, is SessionState.RoundComplete -> DinoMood.Happy
        is SessionState.Helping -> if (state.hint == Hint.TryAgain) DinoMood.Thinking else DinoMood.Talking
        is SessionState.Introducing -> DinoMood.Talking
        is SessionState.AwaitingAnswer -> DinoMood.Idle
    }

    Box(Modifier.fillMaxSize()) {
        Scenery(Modifier.fillMaxSize())
        Row(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxHeight().weight(0.26f), contentAlignment = Alignment.BottomCenter) {
                Dino(mood, Modifier.fillMaxWidth().fillMaxHeight(0.62f).padding(bottom = 12.dp))
            }
            Column(Modifier.fillMaxHeight().weight(0.74f).padding(end = 16.dp)) {
                RoundProgress(state, Modifier.align(Alignment.End).padding(top = 12.dp))
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    if (item != null) {
                        key(item.seed) {
                            EggField(
                                item = item,
                                labels = labels,
                                highlighted = highlighted,
                                hatched = state is SessionState.Celebrating,
                                onEggTapped = { index ->
                                    if (state is SessionState.AwaitingAnswer && index !in labels) {
                                        val n = labels.size + 1
                                        labels[index] = n
                                        narrator.say(Narrator.numberWord(n))
                                    }
                                },
                            )
                        }
                    }
                }
                if (item != null) {
                    Choices(
                        state = state,
                        item = item,
                        onChoose = { onEvent(SessionEvent.Answered(ChildResponse.NumberChosen(it))) },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp),
                    )
                }
            }
        }
        if (state is SessionState.RoundComplete) {
            PlayAgain(onPlayAgain, Modifier.align(Alignment.Center))
        }
    }
}

private fun SessionState.currentItem(): CountObjectsInstance? = when (this) {
    is SessionState.Introducing -> item
    is SessionState.AwaitingAnswer -> item
    is SessionState.Helping -> item
    is SessionState.Celebrating -> item
    is SessionState.RoundComplete -> null
}

@Composable
private fun EggField(
    item: CountObjectsInstance,
    labels: Map<Int, Int>,
    highlighted: Int,
    hatched: Boolean,
    onEggTapped: (Int) -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }
        val baseSize = minOf(maxWidth * 0.15f, maxHeight * 0.42f)
        var countableSeen = 0
        item.scene.objects.forEachIndexed { index, obj ->
            val sizeDp = baseSize * obj.scale
            val sizePx = with(density) { sizeDp.toPx() }
            val position = Modifier
                .offset { IntOffset((obj.x * widthPx - sizePx / 2).roundToInt(), (obj.y * heightPx - sizePx / 2).roundToInt()) }
                .size(sizeDp)
            if (obj.countable) {
                val hatchOrder = countableSeen++
                Egg(
                    color = DinoColors.eggs[obj.sprite.value] ?: DinoColors.eggs.values.first(),
                    label = labels[index],
                    highlighted = highlighted == index,
                    hatched = hatched,
                    appearDelayMs = 150L * index,
                    hatchDelayMs = 180L * hatchOrder,
                    onTap = { onEggTapped(index) },
                    modifier = position,
                )
            } else {
                Leaf(obj.rotationDeg, position)
            }
        }
    }
}

@Composable
private fun Choices(
    state: SessionState,
    item: CountObjectsInstance,
    onChoose: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val choices = when (state) {
        is SessionState.AwaitingAnswer -> state.choices
        is SessionState.Helping -> state.choices
        else -> item.choices
    }
    val enabled = state is SessionState.AwaitingAnswer
    val narrowed = choices.size < item.choices.size
    val wrong = (state as? SessionState.Helping)?.wrongAnswer
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterHorizontally)) {
        choices.forEach { n ->
            key(n) {
                NumberButton(
                    number = n,
                    enabled = enabled,
                    shake = n == wrong,
                    glow = narrowed && n == item.answer,
                    correct = state is SessionState.Celebrating && n == item.answer,
                    onClick = { onChoose(n) },
                )
            }
        }
    }
}

/** A big round button showing the numeral and the same number of dots. */
@Composable
private fun NumberButton(number: Int, enabled: Boolean, shake: Boolean, glow: Boolean, correct: Boolean, onClick: () -> Unit) {
    val shakeX = remember { Animatable(0f) }
    LaunchedEffect(shake) {
        if (shake) for (x in listOf(-14f, 12f, -9f, 6f, 0f)) shakeX.animateTo(x, tween(60))
    }
    val pulse = rememberInfiniteTransition(label = "pulse")
    val glowScale by pulse.animateFloat(1f, 1.08f, infiniteRepeatable(tween(600), RepeatMode.Reverse), label = "glowScale")
    val ring = when {
        correct -> DinoColors.Correct
        glow -> DinoColors.Highlight
        else -> Color(0xFFE9DFC4)
    }
    Column(
        Modifier
            .graphicsLayer {
                translationX = shakeX.value
                val s = if (glow || correct) glowScale else 1f
                scaleX = s
                scaleY = s
            }
            .size(104.dp)
            .background(DinoColors.Card, CircleShape)
            .border(5.dp, ring, CircleShape)
            .alpha(if (enabled || glow || correct) 1f else 0.85f)
            .clickable(enabled = enabled, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(number.toString(), fontSize = 46.sp, fontWeight = FontWeight.Black, color = DinoColors.Ink, lineHeight = 46.sp)
        Dots(number)
    }
}

@Composable
private fun Dots(count: Int) {
    val perRow = 5
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        for (row in 0 until (count + perRow - 1) / perRow) {
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                repeat(minOf(perRow, count - row * perRow)) {
                    Box(Modifier.size(8.dp).background(DinoColors.Lava, CircleShape))
                }
            }
            Spacer(Modifier.height(2.dp))
        }
    }
}

/** A nest with one spot per question; it fills as eggs are found. Progress, not a score. */
@Composable
private fun RoundProgress(state: SessionState, modifier: Modifier = Modifier) {
    val done = when (state) {
        is SessionState.Celebrating -> state.itemNumber
        is SessionState.RoundComplete -> state.itemsInRound
        else -> state.itemNumber - 1
    }
    Row(
        modifier
            .background(DinoColors.Nest, CircleShape)
            .border(3.dp, DinoColors.NestDark, CircleShape)
            .padding(horizontal = 14.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        repeat(state.itemsInRound) { i ->
            val filled = i < done
            Box(
                Modifier
                    .size(width = 20.dp, height = 26.dp)
                    .background(if (filled) DinoColors.eggs.values.elementAt(i % 3) else DinoColors.NestDark.copy(alpha = 0.35f), CircleShape),
            )
        }
    }
}

@Composable
private fun PlayAgain(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val bounce = rememberInfiniteTransition(label = "playAgain")
    val y by bounce.animateFloat(0f, -18f, infiniteRepeatable(tween(500), RepeatMode.Reverse), label = "y")
    Box(
        modifier
            .graphicsLayer { translationY = y }
            .size(width = 170.dp, height = 210.dp)
            .clickable(onClick = onClick),
    ) {
        Egg(
            color = DinoColors.eggs.getValue("egg_orange"),
            label = null,
            highlighted = true,
            hatched = false,
            appearDelayMs = 0,
            hatchDelayMs = 0,
            onTap = onClick,
            modifier = Modifier.fillMaxSize(),
        )
        // A play triangle on the egg: no words needed.
        Canvas(Modifier.size(64.dp).align(Alignment.Center).offset(y = 16.dp)) {
            val p = androidx.compose.ui.graphics.Path().apply {
                moveTo(size.width * 0.3f, size.height * 0.2f)
                lineTo(size.width * 0.82f, size.height * 0.5f)
                lineTo(size.width * 0.3f, size.height * 0.8f)
                close()
            }
            drawPath(p, Color.White)
        }
    }
}
