package com.littledungeon.ui.game

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.window.Dialog
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.littledungeon.engine.model.Speech
import com.littledungeon.engine.model.Who
import com.littledungeon.engine.model.Voice
import com.littledungeon.engine.rpg.run.Say
import com.littledungeon.engine.rpg.hero.HeroClass
import com.littledungeon.engine.rpg.hero.Progression
import com.littledungeon.engine.rpg.run.Place
import com.littledungeon.ui.art.Character
import com.littledungeon.ui.art.Mood
import com.littledungeon.ui.art.Picto
import com.littledungeon.ui.art.PictoIcon
import com.littledungeon.ui.art.Rigs
import kotlinx.coroutines.launch

/**
 * The camp: meet the baby dragon, pick a hero, and start an adventure. A grown-up can give the
 * dragon a name with the pencil; the narrator says it from then on (decision #46).
 */
@Composable
fun TitleScreen(vm: GameViewModel) {
    val narrator = LocalNarrator.current
    val dragon = LocalDragonName.current
    val sfx = LocalSfx.current
    val scope = rememberCoroutineScope()
    // Only the dragon's own words move its mouth; the narrator welcoming you does not.
    val speakingAs by narrator.speakingAs.collectAsState()
    val speaking = speakingAs == Who.PET
    var dragonMood by remember { mutableStateOf(Mood.CALM) }
    var naming by remember { mutableStateOf(false) }
    val hero = vm.state.hero

    LaunchedEffect(Unit) {
        narrator.speak(Speech.of(if (vm.state.world.adventures == 0) Say.WELCOME_NEW else Say.WELCOME_BACK))
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

        // The hero and the dragon; the dragon does the talking here.
        Character(Rigs.hero(hero.heroClass, vm.unlocked), Mood.CALM, Modifier.at(w * 0.2f, h * 0.62f, h * 0.62f, h * 0.62f))
        Character(
            Rigs.babyDragon,
            when {
                dragonMood == Mood.HAPPY -> Mood.HAPPY
                speaking -> Mood.TALKING
                else -> Mood.CALM
            },
            Modifier.at(w * 0.4f, h * 0.7f, h * 0.48f, h * 0.48f),
            voice = narrator::level,
        )

        // The dragon's name tag; the pencil is for grown-ups.
        val tag = h * 0.12f
        Row(
            Modifier
                .at(w * 0.4f, h * 0.38f, w * 0.26f, tag)
                .shadow(6.dp, RoundedCornerShape(50))
                .background(Palette.Paper, RoundedCornerShape(50))
                .border(3.dp, Palette.PaperEdge, RoundedCornerShape(50))
                .clickable {
                    sfx.play("tap")
                    naming = true
                    scope.launch { narrator.speak(Say.NAME_ASK) }
                }
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        ) {
            Text(
                dragon.name, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Black, color = Palette.Name,
                fontSize = with(LocalDensity.current) { (tag * 0.5f).toSp() }, maxLines = 1,
            )
            Box(Modifier.size(tag * 0.7f).background(Palette.Berry, CircleShape), contentAlignment = Alignment.Center) {
                PictoIcon(Picto.PENCIL, Color.White, Modifier.size(tag * 0.42f))
            }
        }

        // Level and stars
        LevelBadge(hero.level, Progression.progress(hero.totalXp), Modifier.align(Alignment.TopStart).padding(12.dp), h * 0.12f)
        FeedbackButton(h * 0.1f, Modifier.align(Alignment.BottomStart).padding(12.dp))

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
                                sfx.play("tap")
                                vm.chooseClass(c)
                                scope.launch { narrator.speak(Say.heroLine(c)) }
                            } else {
                                scope.launch { narrator.speak(Say.heroLocked(c.unlockLevel)) }
                            }
                        },
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    Character(
                        Rigs.hero(c, vm.unlocked), if (chosen) Mood.HAPPY else Mood.CALM,
                        Modifier.fillMaxSize().padding(4.dp).graphicsLayer { alpha = if (open) 1f else 0.25f },
                        animate = false,
                    )
                    if (!open) Text("${c.unlockLevel}", fontSize = 28.sp, fontWeight = FontWeight.Black, color = Palette.Ink, modifier = Modifier.align(Alignment.Center))
                }
            }
        }

        RoundButton(
            Picto.NEXT, Palette.Go, h * 0.26f,
            Modifier.align(Alignment.BottomEnd).padding(end = 22.dp, bottom = 18.dp),
            pulse = true,
        ) {
            sfx.play("tap")
            narrator.stop()
            vm.start()
        }

        if (naming) {
            NameDialog(
                current = dragon.name,
                done = { typed ->
                    naming = false
                    if (typed != null && dragon.set(typed)) {
                        dragonMood = Mood.HAPPY
                        scope.launch {
                            narrator.speak(Speech.of(Say.NAME_SET))
                            dragonMood = Mood.CALM
                        }
                    }
                },
            )
        }
    }
}

/** For grown-ups: type the name the child chose. Picture buttons only for the child elsewhere. */
@Composable
private fun NameDialog(current: String, done: (String?) -> Unit) {
    var text by remember { mutableStateOf(if (current == Voice.DEFAULT_NAME) "" else current) }
    Dialog(onDismissRequest = { done(null) }) {
        Column(
            Modifier
                .background(Palette.Paper, RoundedCornerShape(24.dp))
                .border(3.dp, Palette.PaperEdge, RoundedCornerShape(24.dp))
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("The baby dragon's name", fontFamily = FontFamily.Serif, fontWeight = FontWeight.Black, fontSize = 22.sp, color = Palette.Ink)
            OutlinedTextField(
                value = text,
                onValueChange = { text = it.take(16) },
                singleLine = true,
                placeholder = { Text(Voice.DEFAULT_NAME) },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { done(text) }),
                textStyle = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Palette.Name),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                RoundButton(Picto.HOME, Palette.Berry, 56.dp) { done(null) }
                RoundButton(Picto.CHECK, Palette.Go, 56.dp) { done(text) }
            }
        }
    }
}

/** A star with the hero's level and a bar that fills toward the next one. */
@Composable
fun LevelBadge(level: Int, progress: Float, modifier: Modifier = Modifier, size: androidx.compose.ui.unit.Dp) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(contentAlignment = Alignment.Center) {
            com.littledungeon.ui.art.BossStar(true, Modifier.size(size))
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

