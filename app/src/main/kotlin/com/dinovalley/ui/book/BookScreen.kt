package com.dinovalley.ui.book

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dinovalley.R
import com.dinovalley.engine.story.Backdrop
import com.dinovalley.engine.story.Cast
import com.dinovalley.engine.story.Challenge
import com.dinovalley.engine.story.StoryPage
import com.dinovalley.ui.art.Dino
import com.dinovalley.ui.art.DinoKind
import com.dinovalley.ui.art.Mood
import com.dinovalley.ui.art.Picto
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** The whole game: a picture book whose pages slide like turning paper. */
@Composable
fun BookScreen(vm: BookViewModel) {
    AnimatedContent(
        targetState = vm.page,
        transitionSpec = {
            (slideInHorizontally(tween(550)) { it } + fadeIn(tween(300))) togetherWith
                (slideOutHorizontally(tween(550)) { -it / 3 } + fadeOut(tween(500)))
        },
        label = "page",
    ) { p ->
        val book = vm.book
        when {
            p == BookViewModel.COVER -> Cover(book.title, onOpen = vm::next)
            p >= book.pages.size -> TheEnd(onAgain = vm::readAgain, onHome = vm::toCover)
            else -> PageView(book.pages[p], onSolved = { vm.solved(p, it) }, onNext = vm::next)
        }
    }
}

private enum class Phase { READING, PLAYING, DONE }

@Composable
private fun PageView(page: StoryPage, onSolved: (Boolean) -> Unit, onNext: () -> Unit) {
    val narrator = LocalNarrator.current
    val scope = rememberCoroutineScope()
    val speaking by narrator.speaking.collectAsState()
    var phase by remember { mutableStateOf(Phase.READING) }
    var mood by remember { mutableStateOf(Mood.IDLE) }
    var caption by remember { mutableStateOf(page.narration) }

    LaunchedEffect(Unit) {
        delay(500) // let the page finish turning
        narrator.speak(page.narration)
        phase = if (page.challenge == null) Phase.DONE else Phase.PLAYING
    }
    val replay: () -> Unit = {
        scope.launch {
            narrator.speak(if (phase == Phase.PLAYING && page.prompt.isNotEmpty()) page.prompt else caption)
        }
    }
    val finish: (Boolean) -> Unit = { firstTry ->
        if (phase == Phase.PLAYING) {
            phase = Phase.READING
            onSolved(firstTry)
            caption = page.afterward
            scope.launch {
                mood = Mood.HAPPY
                narrator.speak(page.afterward)
                mood = Mood.IDLE
                phase = Phase.DONE
            }
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val h = maxHeight
        val w = maxWidth
        BackdropImage(page.backdrop)
        if (page.backdrop == Backdrop.VALLEY) {
            EmptyNest(Modifier.at(w * 0.58f, h * 0.8f, h * 0.7f, h * 0.315f))
        }
        Dino(
            DinoKind.REX,
            if (speaking && mood != Mood.HAPPY) Mood.TALKING else mood,
            Modifier.align(Alignment.BottomStart).padding(start = w * 0.01f).size(h * 0.58f),
        )
        if (Cast.MAMA in page.cast) {
            Dino(
                DinoKind.MAMA,
                if (mood == Mood.HAPPY || page.challenge is Challenge.HatchEggs && phase == Phase.DONE) Mood.HAPPY else Mood.IDLE,
                Modifier.align(Alignment.BottomEnd).padding(end = w * 0.02f).size(h * 0.64f),
                facingLeft = true,
            )
        }
        page.challenge?.let { ChallengeLayer(it, enabled = phase == Phase.PLAYING, onDone = finish) }
        Caption(
            caption,
            fontSize = with(LocalDensity.current) { (h * 0.052f).toSp() },
            modifier = Modifier.align(Alignment.TopCenter).padding(top = h * 0.03f).fillMaxWidth(0.7f),
            onClick = replay,
        )
        RoundButton(Picto.LISTEN, BookColors.Sky, h * 0.15f, Modifier.align(Alignment.TopStart).padding(12.dp), onClick = replay)
        if (phase == Phase.DONE) {
            RoundButton(
                Picto.NEXT, BookColors.Leaf, h * 0.22f,
                Modifier.align(Alignment.BottomEnd).padding(end = 18.dp, bottom = 14.dp),
                pulse = true,
                onClick = onNext,
            )
        }
    }
}

@Composable
private fun ChallengeLayer(c: Challenge, enabled: Boolean, onDone: (Boolean) -> Unit) {
    when (c) {
        is Challenge.TapClouds -> TapClouds(c, enabled, onDone)
        is Challenge.CountEggs -> CountEggs(c, enabled, onDone)
        is Challenge.FindNumeral -> FindNumeral(c, enabled, onDone)
        is Challenge.FindLetter -> FindLetter(c, enabled, onDone)
        is Challenge.HatchEggs -> HatchEggs(c, enabled, onDone)
    }
}

@Composable
fun EmptyNest(modifier: Modifier) {
    Box(modifier) {
        Image(painterResource(R.drawable.art_nest_back), null, Modifier.fillMaxSize())
        Image(painterResource(R.drawable.art_nest_front), null, Modifier.fillMaxSize())
    }
}

/** Places something by its center, in the page's own coordinates. */
fun Modifier.at(centerX: Dp, centerY: Dp, width: Dp, height: Dp): Modifier =
    this.offset(centerX - width / 2, centerY - height / 2).size(width, height)
