package com.littledungeon.ui.game

import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.animation.core.animateFloatAsState
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
import com.littledungeon.R
import com.littledungeon.audio.Narrator
import com.littledungeon.audio.Sfx
import com.littledungeon.data.DragonName
import com.littledungeon.engine.model.Speech
import com.littledungeon.engine.model.Who
import com.littledungeon.engine.rpg.run.Journey
import com.littledungeon.engine.rpg.run.Place
import com.littledungeon.ui.art.Art
import com.littledungeon.ui.art.BossStar
import com.littledungeon.ui.art.Picto
import com.littledungeon.ui.art.PictoIcon
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

/** An ease that overshoots a little and settles, for answers popping in. */
val OutBack = Easing { t ->
    val c1 = 1.70158f
    val c3 = c1 + 1f
    val u = t - 1f
    1f + c3 * u * u * u + c1 * u * u
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
        // Characters' words are labelled with who says them; the narrator's are plain.
        var who = Who.NARRATOR
        var fresh = true
        fun space(next: String?) {
            val punct = next?.firstOrNull()?.let { it in ",.!?;:" } == true
            if (length > 0 && !fresh && !punct) append(' ')
            fresh = false
        }
        speech.forEach { part ->
            when (part) {
                is Speech.Words -> {
                    space(part.text)
                    append(part.text)
                }
                Speech.Name -> {
                    space(null)
                    withStyle(SpanStyle(color = Palette.Name, fontWeight = FontWeight.Black)) { append(name) }
                }
                is Speech.As -> if (part.who != who) {
                    who = part.who
                    if (who != Who.NARRATOR) {
                        space(null)
                        val label = if (who == Who.PET) name else who.tag.replaceFirstChar { it.uppercase() }
                        withStyle(SpanStyle(color = speakerColor(who), fontWeight = FontWeight.Black)) { append("$label:") }
                    }
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

/** A color for each character's name tag in the caption. */
private fun speakerColor(who: Who): Color = when (who) {
    Who.NARRATOR -> Palette.Ink
    Who.PET -> Palette.Name
    Who.WIZARD -> Color(0xFF7B4FD1)
    Who.GOBLIN -> Color(0xFF3E8E41)
    Who.RUBY -> Color(0xFFD1405F)
    Who.DRAGON -> Color(0xFFB23A1F)
    Who.SHADOW -> Color(0xFF4A4A6A)
    // The people of the kingdom: a warm brown, each told apart by their name in the caption.
    else -> Color(0xFF8A5A2B)
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

/** The bag in the corner: health as numbers, then the coins, the key once it is found, and Storybook pages. */
@Composable
fun BagBar(bag: Journey.Bag, height: Dp, modifier: Modifier = Modifier) {
    Row(
        modifier
            .background(Color(0xCC2A1C10), RoundedCornerShape(50))
            .border(2.dp, Palette.PaperEdge, RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HealthBar(bag.hp, bag.maxHp, height)
        Image(painterResource(R.drawable.art_coin), null, Modifier.size(height))
        Text("${bag.coins}", color = Color.White, fontWeight = FontWeight.Black, fontSize = 20.sp)
        if (bag.hasKey) Image(painterResource(R.drawable.art_key), null, Modifier.size(height))
        if (bag.pages > 0) Text("Pages ${bag.pages}", color = Color(0xFFFFE9B0), fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}

/** Health as a bar with its numbers, so the child can see how much is left, and how much the most is. */
@Composable
fun HealthBar(hp: Int, maxHp: Int, height: Dp, modifier: Modifier = Modifier, label: String = "HP") {
    val fraction by animateFloatAsState((hp.toFloat() / maxHp.coerceAtLeast(1)).coerceIn(0f, 1f), tween(400), label = "hp")
    val color = when {
        fraction > 0.5f -> Color(0xFF4CC15A)
        fraction > 0.25f -> Color(0xFFF2B233)
        else -> Color(0xFFE5484D)
    }
    Box(
        modifier.size(height * 4.2f, height * 0.9f).clip(RoundedCornerShape(50)).background(Color(0xFF3B2A1A)).border(2.dp, Palette.PaperEdge, RoundedCornerShape(50)),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(Modifier.fillMaxHeight().fillMaxWidth(fraction).background(color))
        Text(
            "$label $hp/$maxHp", color = Color.White, fontWeight = FontWeight.Black, fontSize = with(LocalDensity.current) { (height * 0.5f).toSp() },
            modifier = Modifier.align(Alignment.Center), maxLines = 1,
        )
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
            if (long) PictoIcon(Picto.BOOK, Palette.Ink, Modifier.size(40.dp).graphicsLayer { rotationZ = spin * 0.05f - 9f })
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
