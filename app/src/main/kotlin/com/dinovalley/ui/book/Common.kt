package com.dinovalley.ui.book

import androidx.annotation.DrawableRes
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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.dinovalley.R
import com.dinovalley.audio.NameRecorder
import com.dinovalley.audio.Narrator
import com.dinovalley.engine.story.Backdrop
import com.dinovalley.engine.story.Speech
import com.dinovalley.ui.art.Picto
import com.dinovalley.ui.art.PictoIcon
import com.dinovalley.ui.theme.DinoColors

val LocalNarrator = staticCompositionLocalOf<Narrator> { error("No narrator") }
val LocalNameRecorder = staticCompositionLocalOf<NameRecorder> { error("No recorder") }

object BookColors {
    val Paper = Color(0xFFFFF8E7)
    val PaperEdge = Color(0xFFE2C99A)
    val Ink = Color(0xFF3B2A1A)
    val Name = Color(0xFFE5621D)
    val Leaf = Color(0xFF5FAE45)
    val Sun = Color(0xFFFFB627)
    val Berry = Color(0xFFE0567A)
    val Sky = Color(0xFF4FA3D9)
}

@DrawableRes
fun backdropArt(b: Backdrop): Int = when (b) {
    Backdrop.VALLEY, Backdrop.HOME -> R.drawable.art_scene_valley
    Backdrop.STORM -> R.drawable.art_scene_storm
    Backdrop.MEADOW -> R.drawable.art_scene_meadow
    Backdrop.RIVER -> R.drawable.art_scene_river
    Backdrop.FOREST -> R.drawable.art_scene_forest
    Backdrop.CAVE -> R.drawable.art_scene_cave
}

@DrawableRes
fun eggArt(sprite: String): Int = when (sprite) {
    "egg_green" -> R.drawable.art_egg_green_whole
    "egg_orange" -> R.drawable.art_egg_orange_whole
    else -> R.drawable.art_egg_blue_whole
}

@Composable
fun BackdropImage(b: Backdrop) {
    Image(painterResource(backdropArt(b)), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
}

/**
 * The words of the page, printed like a picture book for grown-ups reading along. The name shows
 * as a little dino badge when it is the child's own recording, since we can't spell it.
 */
@Composable
fun Caption(speech: List<Speech>, fontSize: TextUnit, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val hasName = LocalNameRecorder.current.hasName
    val text = buildAnnotatedString {
        speech.forEachIndexed { i, part ->
            if (i > 0) append(' ')
            when (part) {
                is Speech.Words -> append(part.text)
                Speech.Name -> withStyle(SpanStyle(color = BookColors.Name, fontWeight = FontWeight.Black)) {
                    append(if (hasName) "🦖" else Narrator.DEFAULT_NAME)
                }
            }
        }
    }
    Box(
        modifier
            .shadow(6.dp, RoundedCornerShape(18.dp))
            .background(BookColors.Paper, RoundedCornerShape(18.dp))
            .border(3.dp, BookColors.PaperEdge, RoundedCornerShape(18.dp))
            .clickable(remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 10.dp),
    ) {
        Text(
            text,
            fontFamily = FontFamily.Serif,
            fontSize = fontSize,
            lineHeight = fontSize * 1.25f,
            color = BookColors.Ink,
            textAlign = TextAlign.Center,
            modifier = Modifier.align(Alignment.Center),
        )
    }
}

/** A big round picture button. [pulse] makes it breathe to say "tap me next". */
@Composable
fun RoundButton(
    picto: Picto,
    color: Color,
    size: Dp,
    modifier: Modifier = Modifier,
    pulse: Boolean = false,
    onClick: () -> Unit,
) {
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

