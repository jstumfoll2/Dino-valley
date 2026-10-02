package com.dinovalley.ui.game

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.dinovalley.engine.model.Speech
import com.dinovalley.engine.rpg.hero.HeroClass
import com.dinovalley.engine.rpg.hero.Progression
import com.dinovalley.engine.rpg.run.Place
import com.dinovalley.ui.art.Character
import com.dinovalley.ui.art.Mood
import com.dinovalley.ui.art.Picto
import com.dinovalley.ui.art.PictoIcon
import com.dinovalley.ui.art.Rigs
import kotlinx.coroutines.launch

private fun classLine(c: HeroClass) = when (c) {
    HeroClass.KNIGHT -> "The Knight! Brave and strong."
    HeroClass.WIZARD -> "The Wizard! Full of magic."
    HeroClass.RANGER -> "The Ranger! Sharp eyes that see behind doors."
    HeroClass.GUARDIAN -> "The Guardian! Everyone wants to be your friend."
    HeroClass.SPELLKEEPER -> "The Spellkeeper! Keeper of runes and stories."
}

/**
 * The camp: name the baby dragon by holding the microphone, pick a hero, and start an adventure.
 */
@Composable
fun TitleScreen(vm: GameViewModel) {
    val context = LocalContext.current
    val narrator = LocalNarrator.current
    val recorder = LocalNameRecorder.current
    val scope = rememberCoroutineScope()
    val speaking by narrator.speaking.collectAsState()
    var dragonMood by remember { mutableStateOf(Mood.CALM) }
    var listening by remember { mutableStateOf(false) }
    val hero = vm.state.hero

    val askMic = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        scope.launch {
            if (granted) narrator.speak("Yay! Now hold the microphone button, and say my name!")
            else narrator.speak("That's okay! You can call me ${com.dinovalley.audio.Narrator.DEFAULT_NAME}.")
        }
    }

    LaunchedEffect(Unit) {
        if (recorder.hasName) {
            narrator.speak(Speech.of("Welcome back, adventurer! {name} is ready. Pick your hero, then tap the big green button!"))
        } else {
            narrator.speak(
                "Welcome to the Little Dungeon! This is your baby dragon. It doesn't have a name yet. " +
                    "Hold the microphone button, and say its name!",
            )
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val h = maxHeight
        val w = maxWidth
        Backdrop(Place.CAMP)

        // Title
        val titleSize = with(LocalDensity.current) { (h * 0.11f).toSp() }
        Text(
            "The Little Dungeon",
            style = TextStyle(
                fontFamily = FontFamily.Serif, fontWeight = FontWeight.Black, fontSize = titleSize, color = Color(0xFFFFE9A8),
                shadow = Shadow(Color(0xAA3B2A1A), androidx.compose.ui.geometry.Offset(4f, 6f), 10f),
            ),
            modifier = Modifier.align(Alignment.TopCenter).padding(top = h * 0.03f),
        )

        // The hero and the dragon
        Character(
            Rigs.hero(hero.heroClass, vm.unlocked), if (speaking && !listening) Mood.TALKING else Mood.CALM,
            Modifier.at(w * 0.2f, h * 0.62f, h * 0.62f, h * 0.62f),
        )
        Character(
            Rigs.babyDragon,
            when {
                listening -> Mood.LISTENING
                dragonMood == Mood.HAPPY -> Mood.HAPPY
                else -> Mood.CALM
            },
            Modifier.at(w * 0.4f, h * 0.7f, h * 0.48f, h * 0.48f),
        )

        // Level and stars
        LevelBadge(hero.level, Progression.progress(hero.totalXp), Modifier.align(Alignment.TopStart).padding(12.dp), h * 0.12f)

        // Hold to name the dragon
        val t = rememberInfiniteTransition(label = "mic")
        val ring by t.animateFloat(1f, 1.35f, infiniteRepeatable(tween(500), RepeatMode.Reverse), label = "ring")
        val mic = h * 0.17f
        Box(Modifier.at(w * 0.4f, h * 0.3f, mic * 1.5f, mic * 1.5f), contentAlignment = Alignment.Center) {
            if (listening) {
                Box(Modifier.size(mic).graphicsLayer { scaleX = ring; scaleY = ring }.background(Palette.Berry.copy(alpha = 0.3f), CircleShape))
            }
            Box(
                Modifier
                    .size(mic)
                    .shadow(8.dp, CircleShape)
                    .background(if (listening) Color(0xFFC23A5E) else Palette.Berry, CircleShape)
                    .border(4.dp, Color.White, CircleShape)
                    .pointerInput(Unit) {
                        detectTapGestures(onPress = {
                            val allowed = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
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
                                    dragonMood = Mood.HAPPY
                                    narrator.speak(Speech.of("My name is {name}! I love my name! Let's go on an adventure!"))
                                    dragonMood = Mood.CALM
                                } else {
                                    narrator.speak("I didn't hear you. Hold the button down the whole time you talk!")
                                }
                            }
                        })
                    },
                contentAlignment = Alignment.Center,
            ) {
                PictoIcon(Picto.MIC, Color.White, Modifier.size(mic * 0.58f))
            }
        }

        // Pick a hero
        Row(
            Modifier.align(Alignment.CenterEnd).padding(end = w * 0.03f).fillMaxWidth(0.42f).height(h * 0.42f),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            for (c in HeroClass.entries) {
                val open = c.unlockLevel <= hero.level
                val chosen = c == hero.heroClass
                Box(
                    Modifier
                        .weight(1f, fill = false)
                        .width(h * 0.2f)
                        .fillMaxHeight(if (chosen) 1f else 0.82f)
                        .shadow(if (chosen) 10.dp else 4.dp, RoundedCornerShape(18.dp))
                        .background(if (chosen) Color(0xFFFFF0C2) else Color(0xCCFFF6E0), RoundedCornerShape(18.dp))
                        .border(if (chosen) 5.dp else 2.dp, if (chosen) Palette.Gold else Palette.PaperEdge, RoundedCornerShape(18.dp))
                        .clickable(enabled = true) {
                            if (open) {
                                vm.chooseClass(c)
                                scope.launch { narrator.speak(classLine(c)) }
                            } else {
                                scope.launch { narrator.speak("This hero joins at level ${c.unlockLevel}. Keep adventuring!") }
                            }
                        },
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    Character(
                        Rigs.hero(c, vm.unlocked), if (chosen) Mood.HAPPY else Mood.CALM,
                        Modifier.fillMaxSize().padding(4.dp).graphicsLayer { alpha = if (open) 1f else 0.25f },
                    )
                    if (!open) Text("${c.unlockLevel}", fontSize = 28.sp, fontWeight = FontWeight.Black, color = Palette.Ink, modifier = Modifier.align(Alignment.Center))
                }
            }
        }

        RoundButton(
            Picto.NEXT, Palette.Go, h * 0.26f,
            Modifier.align(Alignment.BottomEnd).padding(end = 22.dp, bottom = 18.dp),
            pulse = !listening,
        ) {
            narrator.stop()
            vm.start()
        }
    }
}

/** A star with the hero's level and a bar that fills toward the next one. */
@Composable
fun LevelBadge(level: Int, progress: Float, modifier: Modifier = Modifier, size: androidx.compose.ui.unit.Dp) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(contentAlignment = Alignment.Center) {
            com.dinovalley.ui.art.BossStar(true, Modifier.size(size))
            Text("$level", fontWeight = FontWeight.Black, fontSize = with(LocalDensity.current) { (size * 0.32f).toSp() }, color = Palette.Ink)
        }
        Box(
            Modifier.padding(start = 6.dp).width(size * 1.6f).height(size * 0.22f)
                .background(Color(0x88000000), RoundedCornerShape(50)).border(2.dp, Color.White, RoundedCornerShape(50)),
        ) {
            Box(Modifier.fillMaxHeight().fillMaxWidth(progress.coerceIn(0.02f, 1f)).background(Palette.Gold, RoundedCornerShape(50)))
        }
    }
}

