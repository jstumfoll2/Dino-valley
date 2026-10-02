package com.dinovalley.ui.game

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dinovalley.R
import com.dinovalley.engine.rpg.hero.Attribute
import com.dinovalley.engine.rpg.hero.Progression
import com.dinovalley.engine.rpg.run.Beat
import com.dinovalley.ui.art.BossStar
import com.dinovalley.ui.art.Character
import com.dinovalley.ui.art.Mood
import com.dinovalley.ui.art.Picto
import com.dinovalley.ui.art.Rigs
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private fun Attribute.icon() = when (this) {
    Attribute.COURAGE -> "❤️"
    Attribute.CLEVERNESS -> "🧠"
    Attribute.WISDOM -> "📖"
    Attribute.KINDNESS -> "💛"
    Attribute.MAGIC -> "✨"
}

/**
 * Back at camp: the treasure, the stars earned for each attribute, the star bar filling up, and
 * a fanfare when the hero levels up. Then play again or go home.
 */
@Composable
fun FinaleScreen(beat: Beat.Finale, vm: GameViewModel) {
    val narrator = LocalNarrator.current
    val scope = rememberCoroutineScope()
    val summary = beat.summary
    val hero = vm.state.hero
    val earned = summary.starsEarned.values.sum()
    val xpBefore = (hero.totalXp - earned).coerceAtLeast(0)
    var shownLevel by remember { mutableIntStateOf(summary.levelBefore) }
    val bar = remember { Animatable(Progression.progress(xpBefore)) }
    var talking by remember { mutableStateOf(true) }
    val levelPop = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        launch {
            delay(900)
            if (summary.levelAfter > summary.levelBefore) {
                bar.animateTo(1f, tween(1200))
                shownLevel = summary.levelAfter
                bar.snapTo(0f)
                levelPop.snapTo(1.6f)
                launch { levelPop.animateTo(1f, spring(dampingRatio = 0.3f, stiffness = 200f)) }
            }
            bar.animateTo(Progression.progress(hero.totalXp), tween(1000))
        }
        narrator.speak(summary.lines)
        talking = false
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val w = maxWidth
        val h = maxHeight
        Backdrop(beat.scene.place)
        val t = rememberInfiniteTransition(label = "party")
        val hop by t.animateFloat(0f, 1f, infiniteRepeatable(tween(420), RepeatMode.Reverse), label = "hop")
        Character(
            Rigs.hero(hero.heroClass, vm.unlocked), Mood.HAPPY,
            Modifier.at(w * 0.12f, h * 0.68f, h * 0.56f, h * 0.56f).graphicsLayer { translationY = -hop * 12f },
        )
        Character(
            Rigs.babyDragon, if (talking) Mood.TALKING else Mood.HAPPY,
            Modifier.at(w * 0.28f, h * 0.78f, h * 0.38f, h * 0.38f),
        )
        Image(painterResource(R.drawable.art_treasure), null, Modifier.at(w * 0.24f, h * 0.3f, h * 0.26f, h * 0.26f))

        // The scroll of stars.
        val panelW = w * 0.52f
        val big = with(LocalDensity.current) { (h * 0.1f).toSp() }
        val mid = with(LocalDensity.current) { (h * 0.075f).toSp() }
        Column(
            Modifier
                .offset(w * 0.44f, h * 0.05f)
                .size(panelW, h * 0.7f)
                .shadow(8.dp, RoundedCornerShape(20.dp))
                .background(Palette.Paper, RoundedCornerShape(20.dp))
                .border(4.dp, Palette.PaperEdge, RoundedCornerShape(20.dp))
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                LevelBadge(
                    shownLevel, bar.value,
                    Modifier.graphicsLayer { scaleX = levelPop.value; scaleY = levelPop.value },
                    h * 0.16f,
                )
            }
            if (shownLevel > summary.levelBefore) {
                Text(
                    "LEVEL UP!", fontSize = big, lineHeight = big, fontWeight = FontWeight.Black, color = Palette.Name,
                    modifier = Modifier.graphicsLayer { val s = 1f + hop * 0.06f; scaleX = s; scaleY = s },
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(h * 0.04f), verticalAlignment = Alignment.CenterVertically) {
                summary.starsEarned.entries.filter { it.value > 0 }.sortedByDescending { it.value }.forEach { (attr, stars) ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(attr.icon(), fontSize = mid, lineHeight = mid)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            BossStar(true, Modifier.size(h * 0.05f))
                            Text(
                                "$stars", fontSize = mid * 0.7f, lineHeight = mid * 0.7f, fontWeight = FontWeight.Black,
                                color = Palette.Ink, modifier = Modifier.padding(start = 2.dp),
                            )
                        }
                    }
                }
            }
            if (summary.unlocked.isNotEmpty()) {
                Row(
                    Modifier.background(Color(0x22E5641F), RoundedCornerShape(50)).padding(horizontal = 14.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    summary.unlocked.forEach { Text("🎁", fontSize = mid, lineHeight = mid) }
                }
            }
        }

        RoundButton(Picto.LISTEN, Palette.Sky, h * 0.14f, Modifier.align(Alignment.TopStart).padding(10.dp)) {
            scope.launch { narrator.speak(summary.lines) }
        }
        Row(
            Modifier.align(Alignment.BottomEnd).padding(end = w * 0.04f, bottom = h * 0.04f),
            horizontalArrangement = Arrangement.spacedBy(h * 0.06f),
        ) {
            RoundButton(Picto.HOME, Palette.Berry, h * 0.18f) {
                narrator.stop()
                vm.home()
            }
            RoundButton(Picto.AGAIN, Palette.Go, h * 0.2f, pulse = !talking) {
                narrator.stop()
                vm.start()
            }
        }
    }
}
