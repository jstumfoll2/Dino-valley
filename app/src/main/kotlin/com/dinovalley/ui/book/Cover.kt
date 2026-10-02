package com.dinovalley.ui.book

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.dinovalley.R
import com.dinovalley.engine.story.Backdrop
import com.dinovalley.engine.model.Speech
import com.dinovalley.ui.art.Dino
import com.dinovalley.ui.art.DinoKind
import com.dinovalley.ui.art.Mood
import com.dinovalley.ui.art.Picto
import com.dinovalley.ui.art.PictoIcon
import kotlinx.coroutines.launch

private val START = Speech.of("Tap the book to start!")

/**
 * The cover, where the child names their dino: hold the microphone, say a name, let go.
 * The dino says its new name back in the child's own voice.
 */
@Composable
fun Cover(title: List<Speech>, onOpen: () -> Unit) {
    val context = LocalContext.current
    val narrator = LocalNarrator.current
    val recorder = LocalNameRecorder.current
    val scope = rememberCoroutineScope()
    val speaking by narrator.speaking.collectAsState()
    var mood by remember { mutableStateOf(Mood.IDLE) }
    var listening by remember { mutableStateOf(false) }

    val askMic = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        scope.launch {
            if (granted) narrator.speak("Yay! Now hold the microphone button, and say my name!")
            else narrator.speak("That's okay! You can call me Rex.")
        }
    }

    LaunchedEffect(Unit) {
        if (recorder.hasName) {
            narrator.speak(title + START)
        } else {
            narrator.speak("Hi! I'm a baby dinosaur, and I don't have a name yet. Hold the microphone button, and tell me my name!")
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val h = maxHeight
        val w = maxWidth
        BackdropImage(Backdrop.VALLEY)
        Image(painterResource(R.drawable.art_baby), null, Modifier.at(w * 0.62f, h * 0.84f, h * 0.2f, h * 0.2f))

        Dino(
            DinoKind.REX,
            when {
                listening -> Mood.LISTENING
                speaking && mood != Mood.HAPPY -> Mood.TALKING
                else -> mood
            },
            Modifier.at(w * 0.33f, h * 0.62f, h * 0.7f, h * 0.7f),
        )

        Column(
            Modifier.align(Alignment.TopCenter).padding(top = h * 0.04f).fillMaxWidth(0.62f),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val small = with(LocalDensity.current) { (h * 0.05f).toSp() }
            Text("Dino Valley", fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = small, color = Color.White)
            Caption(title, fontSize = with(LocalDensity.current) { (h * 0.085f).toSp() }, onClick = {
                scope.launch { narrator.speak(title) }
            })
        }

        // Hold to record. The ring grows while the dino is listening.
        val t = rememberInfiniteTransition(label = "mic")
        val ring by t.animateFloat(1f, 1.35f, infiniteRepeatable(tween(500), RepeatMode.Reverse), label = "ring")
        val micSize = h * 0.24f
        Box(Modifier.at(w * 0.7f, h * 0.6f, micSize * 1.5f, micSize * 1.5f), contentAlignment = Alignment.Center) {
            if (listening) {
                Box(
                    Modifier.size(micSize).graphicsLayer { scaleX = ring; scaleY = ring }
                        .background(BookColors.Berry.copy(alpha = 0.3f), CircleShape),
                )
            }
            Box(
                Modifier
                    .size(micSize)
                    .shadow(8.dp, CircleShape)
                    .background(if (listening) Color(0xFFC23A5E) else BookColors.Berry, CircleShape)
                    .border(5.dp, Color.White, CircleShape)
                    .pointerInput(Unit) {
                        detectTapGestures(onPress = {
                            val allowed = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                                PackageManager.PERMISSION_GRANTED
                            if (!allowed) {
                                askMic.launch(Manifest.permission.RECORD_AUDIO)
                                return@detectTapGestures
                            }
                            narrator.stop()
                            if (!recorder.start()) {
                                scope.launch { narrator.speak("Hmm, I can't hear right now.") }
                                return@detectTapGestures
                            }
                            listening = true
                            tryAwaitRelease()
                            listening = false
                            val kept = recorder.stop()
                            scope.launch {
                                if (kept) {
                                    mood = Mood.HAPPY
                                    narrator.speak(Speech.of("My name is {name}! I love my name!"))
                                    mood = Mood.IDLE
                                    narrator.speak(START)
                                } else {
                                    narrator.speak("I didn't hear you. Hold the button down the whole time you talk!")
                                }
                            }
                        })
                    },
                contentAlignment = Alignment.Center,
            ) {
                PictoIcon(Picto.MIC, Color.White, Modifier.size(micSize * 0.58f))
            }
        }

        RoundButton(
            Picto.BOOK, BookColors.Leaf, h * 0.26f,
            Modifier.align(Alignment.BottomEnd).padding(end = 22.dp, bottom = 18.dp),
            pulse = recorder.hasName && !listening,
        ) {
            narrator.stop()
            onOpen()
        }
    }
}

/** The last page: everyone together, then read again or go back to the cover. */
@Composable
fun TheEnd(onAgain: () -> Unit, onHome: () -> Unit) {
    val narrator = LocalNarrator.current
    val speaking by narrator.speaking.collectAsState()
    LaunchedEffect(Unit) {
        narrator.speak(Speech.of("The end! {name} found all the eggs. Do you want to read it again?"))
    }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val h = maxHeight
        val w = maxWidth
        BackdropImage(Backdrop.HOME)
        EmptyNest(Modifier.at(w * 0.52f, h * 0.82f, h * 0.7f, h * 0.315f))
        listOf(0.42f, 0.52f, 0.62f).forEachIndexed { i, x ->
            val t = rememberInfiniteTransition(label = "baby$i")
            val bounce by t.animateFloat(0f, 1f, infiniteRepeatable(tween(420 + i * 90), RepeatMode.Reverse), label = "bounce")
            Image(
                painterResource(R.drawable.art_baby), null,
                Modifier.at(w * x, h * 0.7f, h * 0.2f, h * 0.2f).graphicsLayer { translationY = -bounce * 18f },
            )
        }
        Dino(DinoKind.REX, if (speaking) Mood.TALKING else Mood.HAPPY, Modifier.align(Alignment.BottomStart).size(h * 0.58f))
        Dino(DinoKind.MAMA, Mood.HAPPY, Modifier.align(Alignment.BottomEnd).padding(end = w * 0.14f).size(h * 0.62f), facingLeft = true)
        val big = with(LocalDensity.current) { (h * 0.14f).toSp() }
        Text(
            "The End",
            fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = big, color = Color.White,
            modifier = Modifier.align(Alignment.TopCenter).padding(top = h * 0.06f),
        )
        RoundButton(Picto.HOME, BookColors.Sky, h * 0.16f, Modifier.align(Alignment.TopStart).padding(14.dp), onClick = onHome)
        RoundButton(
            Picto.AGAIN, BookColors.Leaf, h * 0.24f,
            Modifier.align(Alignment.BottomEnd).padding(end = 18.dp, bottom = 14.dp),
            pulse = true,
            onClick = onAgain,
        )
    }
}
