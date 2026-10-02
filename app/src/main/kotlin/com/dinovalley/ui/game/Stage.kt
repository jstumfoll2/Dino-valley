package com.dinovalley.ui.game

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dinovalley.R
import com.dinovalley.audio.Narrator
import com.dinovalley.audio.Sfx
import com.dinovalley.data.DragonName
import com.dinovalley.engine.model.Speech
import com.dinovalley.engine.rpg.run.Adventure
import com.dinovalley.engine.rpg.run.Place
import com.dinovalley.ui.art.Art
import com.dinovalley.ui.art.BossStar
import com.dinovalley.ui.art.Picto
import com.dinovalley.ui.art.PictoIcon
import kotlinx.coroutines.delay

val LocalNarrator = staticCompositionLocalOf<Narrator> { error("No narrator") }
val LocalDragonName = staticCompositionLocalOf<DragonName> { error("No dragon name") }
val LocalSfx = staticCompositionLocalOf<Sfx> { error("No sound effects") }

object Palette {
    val Paper = Color(0xFFFFF6E0)
    val PaperEdge = Color(0xFFC9A46A)
    val Ink = Color(0xFF3B2A1A)
    val Name = Color(0xFFE5641F)
    val Go = Color(0xFF4FAE45)
    val Sky = Color(0xFF4F8FD9)
    val Berry = Color(0xFFE0567A)
    val Gold = Color(0xFFFFC21F)
    val Right = Color(0xFF4FC24A)
}

/** Shared by every screen: a tap target with no ripple, since the art does its own feedback. */
val NoRipple = MutableInteractionSource()

@Composable
fun Backdrop(place: Place) {
    Image(painterResource(Art.place(place)), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
}

/** Places something by its center, in the screen's own coordinates. */
fun Modifier.at(centerX: Dp, centerY: Dp, width: Dp, height: Dp): Modifier =
    this.offset(centerX - width / 2, centerY - height / 2).size(width, height)

/** The narrator's words, printed on parchment for grown-ups reading along. The dragon's name is in orange. */
@Composable
fun Caption(speech: List<Speech>, fontSize: TextUnit, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val name = LocalDragonName.current.name
    val text = buildAnnotatedString {
        speech.forEach { part ->
            val glue = length > 0 && !(part is Speech.Words && part.text.firstOrNull()?.let { it in ",.!?;:" } == true)
            when (part) {
                is Speech.Words -> {
                    if (glue) append(' ')
                    append(part.text)
                }
                Speech.Name -> {
                    if (glue) append(' ')
                    withStyle(SpanStyle(color = Palette.Name, fontWeight = FontWeight.Black)) { append(name) }
                }
                is Speech.Sound -> Unit
            }
        }
    }
    Box(
        modifier
            .shadow(6.dp, RoundedCornerShape(16.dp))
            .background(Palette.Paper, RoundedCornerShape(16.dp))
            .border(3.dp, Palette.PaperEdge, RoundedCornerShape(16.dp))
            .clickable(NoRipple, indication = null, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(
            text,
            fontFamily = FontFamily.Serif,
            fontSize = fontSize,
            lineHeight = fontSize * 1.2f,
            color = Palette.Ink,
            textAlign = TextAlign.Center,
            maxLines = 4,
            modifier = Modifier.align(Alignment.Center),
        )
    }
}

/** A big round picture button. [pulse] makes it breathe to say "tap me next". */
@Composable
fun RoundButton(picto: Picto, color: Color, size: Dp, modifier: Modifier = Modifier, pulse: Boolean = false, onClick: () -> Unit) {
    val t = rememberInfiniteTransition(label = "button")
    val beat by t.animateFloat(1f, 1.1f, infiniteRepeatable(tween(650), RepeatMode.Reverse), label = "beat")
    Box(
        modifier
            .graphicsLayer {
                val s = if (pulse) beat else 1f
                scaleX = s
                scaleY = s
            }
            .size(size)
            .shadow(8.dp, CircleShape)
            .background(color, CircleShape)
            .border(4.dp, Color.White, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        PictoIcon(picto, Color.White, Modifier.size(size * 0.56f))
    }
}

/** The bag in the corner: hearts, then the coins, gems, key and potion found on this adventure. */
@Composable
fun BagBar(bag: Adventure.Bag, height: Dp, modifier: Modifier = Modifier) {
    Row(
        modifier
            .background(Color(0xCC2A1C10), RoundedCornerShape(50))
            .border(2.dp, Palette.PaperEdge, RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(3) { i -> Text(if (i < bag.hearts) "❤️" else "🤍", fontSize = 18.sp) }
        Image(painterResource(R.drawable.art_coin), null, Modifier.size(height))
        Text("${bag.coins}", color = Color.White, fontWeight = FontWeight.Black, fontSize = 20.sp)
        if (bag.gems > 0) {
            Image(painterResource(R.drawable.art_gem_blue), null, Modifier.size(height))
            Text("${bag.gems}", color = Color.White, fontWeight = FontWeight.Black, fontSize = 20.sp)
        }
        if (bag.key) Image(painterResource(R.drawable.art_key), null, Modifier.size(height))
        bag.potion?.let { Image(painterResource(Art.potion(it)), null, Modifier.size(height)) }
    }
}

@Composable
fun BossStars(lit: Int, size: Dp, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(size * 0.15f)) {
        repeat(3) { BossStar(it < lit, Modifier.size(size)) }
    }
}


/**
 * Shown while the narrator's next words are still being made: a little scroll with bouncing
 * dots, so a pause reads as "getting ready", not "stuck". After a moment it grows into a
 * proper loading card.
 */
@Composable
fun VoiceLoading(modifier: Modifier = Modifier) {
    val narrator = LocalNarrator.current
    val preparing by narrator.preparing.collectAsState()
    var long by remember { mutableStateOf(false) }
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(preparing) {
        long = false
        visible = false
        if (preparing) {
            delay(250) // most waits are too short to notice; don't flash
            visible = true
            delay(1500)
            long = true
        }
    }
    if (!visible) return
    val t = rememberInfiniteTransition(label = "loading")
    val phase by t.animateFloat(0f, 3f, infiniteRepeatable(tween(900)), label = "phase")
    val spin by t.animateFloat(0f, 360f, infiniteRepeatable(tween(1400)), label = "spin")
    Box(modifier.fillMaxSize(), contentAlignment = if (long) Alignment.Center else Alignment.TopCenter) {
        Row(
            Modifier
                .padding(top = if (long) 0.dp else 70.dp)
                .shadow(8.dp, RoundedCornerShape(28.dp))
                .background(Palette.Paper, RoundedCornerShape(28.dp))
                .border(3.dp, Palette.PaperEdge, RoundedCornerShape(28.dp))
                .padding(horizontal = if (long) 28.dp else 16.dp, vertical = if (long) 18.dp else 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (long) Text("📖", fontSize = 40.sp, modifier = Modifier.graphicsLayer { rotationZ = spin * 0.05f - 9f })
            repeat(3) { i ->
                val up = (phase - i).let { if (it in 0f..1f) kotlin.math.sin(it * Math.PI).toFloat() else 0f }
                Box(
                    Modifier
                        .graphicsLayer { translationY = -up * 14f }
                        .size(if (long) 18.dp else 12.dp)
                        .background(Palette.Name, CircleShape),
                )
            }
            if (long) BossStar(true, Modifier.size(40.dp).graphicsLayer { rotationZ = spin })
        }
    }
}
