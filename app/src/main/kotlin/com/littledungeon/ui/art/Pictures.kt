package com.littledungeon.ui.art

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.runtime.Composable
import com.littledungeon.R
import com.littledungeon.engine.rpg.learn.Hue
import com.littledungeon.engine.rpg.learn.Ingredient
import com.littledungeon.engine.rpg.learn.PotionKind
import com.littledungeon.engine.rpg.learn.PuzzlePicture
import com.littledungeon.engine.rpg.learn.Rune
import com.littledungeon.engine.rpg.learn.RuneShape
import com.littledungeon.engine.rpg.learn.Thing
import com.littledungeon.engine.rpg.run.ChoicePicture
import com.littledungeon.engine.rpg.run.LootKind
import com.littledungeon.engine.rpg.run.Place
import com.littledungeon.engine.rpg.world.RoomKind
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Where every picture lives. One place to look when art changes. */
object Art {
    fun color(h: Hue): Color = when (h) {
        Hue.RED -> Color(0xFFE23B3B)
        Hue.BLUE -> Color(0xFF2F7DE1)
        Hue.GREEN -> Color(0xFF36B24A)
        Hue.YELLOW -> Color(0xFFF5C518)
        Hue.PURPLE -> Color(0xFF8E4AD8)
        Hue.ORANGE -> Color(0xFFF07F1A)
    }

    /** The painted backdrop for a place, found by its name (`art_scene_<id>`), so new places need only a picture. */
    @DrawableRes
    fun place(p: Place): Int = byName("art_scene_${p.id}") ?: R.drawable.art_scene_camp

    private val found = HashMap<String, Int?>()

    /** A picture by resource name, or null if this build has none: the way open content finds its art. */
    @DrawableRes
    fun byName(name: String): Int? = synchronized(found) {
        found.getOrPut(name) {
            runCatching { R.drawable::class.java.getField(name).getInt(null) }.getOrNull()
        }
    }

    /** The picture a mosaic puzzle is made of. */
    @DrawableRes
    fun puzzle(p: PuzzlePicture): Int = when (p) {
        PuzzlePicture.CAMP -> R.drawable.art_scene_camp
        PuzzlePicture.BRIDGE -> R.drawable.art_scene_bridge
        PuzzlePicture.CRYSTAL_CAVE -> R.drawable.art_scene_crystal_cave
        PuzzlePicture.LIBRARY -> R.drawable.art_scene_library
        PuzzlePicture.LAIR -> R.drawable.art_scene_lair
        PuzzlePicture.POND -> R.drawable.art_scene_pond
    }

    /** The sign on a door at a fork: what kind of puzzle is behind it. */
    fun sign(kind: RoomKind): String = when (kind) {
        RoomKind.RUNE_DOOR -> "⭐🌙"
        RoomKind.BRIDGE -> "123"
        RoomKind.CRYSTAL_CAVE -> "🎨"
        RoomKind.LIBRARY -> "ABC"
        RoomKind.TUNNEL -> "✏️"
        RoomKind.MIRROR_HALL -> "👀"
        RoomKind.VAULT -> "1+1"
        RoomKind.STOREROOM -> "🧺"
        RoomKind.POND -> "🐸"
        RoomKind.MOSAIC_HALL -> "🧩"
        else -> "❓"
    }

    @DrawableRes
    fun gem(h: Hue): Int = when (h) {
        Hue.RED -> R.drawable.art_gem_red
        Hue.BLUE -> R.drawable.art_gem_blue
        Hue.GREEN -> R.drawable.art_gem_green
        Hue.YELLOW -> R.drawable.art_gem_yellow
        Hue.PURPLE -> R.drawable.art_gem_purple
        Hue.ORANGE -> R.drawable.art_gem_orange
    }

    @DrawableRes
    fun door(h: Hue): Int = when (h) {
        Hue.RED -> R.drawable.art_door_red
        Hue.BLUE -> R.drawable.art_door_blue
        Hue.GREEN -> R.drawable.art_door_green
        Hue.YELLOW -> R.drawable.art_door_yellow
        Hue.PURPLE -> R.drawable.art_door_purple
        Hue.ORANGE -> R.drawable.art_door_orange
    }

    @DrawableRes
    fun thing(t: Thing): Int = when (t) {
        Thing.STONE -> R.drawable.art_stone
        Thing.COIN -> R.drawable.art_coin
        Thing.GEM -> R.drawable.art_gem_purple
        Thing.POTION -> R.drawable.art_potion_glow
        Thing.KEY -> R.drawable.art_key
        Thing.MUSHROOM -> R.drawable.art_ingredient_mushroom
    }

    @DrawableRes
    fun ingredient(i: Ingredient): Int = when (i) {
        Ingredient.LEAF -> R.drawable.art_ingredient_leaf
        Ingredient.BERRY -> R.drawable.art_ingredient_berry
        Ingredient.CRYSTAL -> R.drawable.art_ingredient_crystal
        Ingredient.FLOWER -> R.drawable.art_ingredient_flower
        Ingredient.MUSHROOM -> R.drawable.art_ingredient_mushroom
    }

    @DrawableRes
    fun potion(p: PotionKind): Int = when (p) {
        PotionKind.GIANT_STRENGTH -> R.drawable.art_potion_strength
        PotionKind.GLOW -> R.drawable.art_potion_glow
        PotionKind.BUBBLE -> R.drawable.art_potion_bubble
        PotionKind.FRIENDSHIP -> R.drawable.art_potion_friendship
    }

    fun potionColor(p: PotionKind): Color = when (p) {
        PotionKind.GIANT_STRENGTH -> Color(0xFFE23B3B)
        PotionKind.GLOW -> Color(0xFFFFD84A)
        PotionKind.BUBBLE -> Color(0xFF59B6FF)
        PotionKind.FRIENDSHIP -> Color(0xFFFF8FC0)
    }

    @DrawableRes
    fun choice(c: ChoicePicture): Int = when (c) {
        ChoicePicture.SHARE_SNACK -> R.drawable.art_choice_snack
        ChoicePicture.SING_SONG -> R.drawable.art_choice_song
        ChoicePicture.TIPTOE -> R.drawable.art_choice_tiptoe
        ChoicePicture.MAKE_FRIENDS -> R.drawable.art_choice_friends
        ChoicePicture.CAST_SPELL -> R.drawable.art_choice_spell
        ChoicePicture.LIGHT_SPELL -> R.drawable.art_choice_light
        ChoicePicture.LULLABY -> R.drawable.art_choice_lullaby
    }

    /** A choice's picture: one of the story pictures, or a painted icon found by name (`art_<icon>`). */
    @DrawableRes
    fun choice(c: com.littledungeon.engine.rpg.run.Choice): Int =
        c.picture?.let { choice(it) } ?: c.icon?.let { byName("art_$it") } ?: R.drawable.art_choice_friends

    /** The icon of an item, found by name (`art_item_<id>`). */
    @DrawableRes
    fun item(id: String): Int = byName("art_item_$id") ?: R.drawable.art_treasure

    @DrawableRes
    fun loot(k: LootKind): Int = when (k) {
        LootKind.COINS -> R.drawable.art_coin
        LootKind.GEM -> R.drawable.art_gem_blue
        LootKind.MAGIC_KEY -> R.drawable.art_key
        LootKind.POTION -> R.drawable.art_potion_glow
        LootKind.TREASURE -> R.drawable.art_treasure
        LootKind.ITEM -> R.drawable.art_treasure
    }
}

/** A magic rune: a bold symbol in its color, drawn in code so any shape and color can pair. */
@Composable
fun RuneIcon(rune: Rune, modifier: Modifier = Modifier) {
    Canvas(modifier) { drawRune(rune.shape, Art.color(rune.hue)) }
}

fun DrawScope.drawRune(shape: RuneShape, color: Color) {
    val w = size.minDimension
    val c = Offset(size.width / 2, size.height / 2)
    val ink = Color(0xFF3D2A1C)
    val path = Path()
    when (shape) {
        RuneShape.STAR -> {
            for (i in 0 until 10) {
                val r = if (i % 2 == 0) w * 0.46f else w * 0.2f
                val a = -PI / 2 + i * PI / 5
                val p = Offset(c.x + r * cos(a).toFloat(), c.y + r * sin(a).toFloat())
                if (i == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
            }
            path.close()
        }
        RuneShape.MOON -> {
            path.addOval(androidx.compose.ui.geometry.Rect(c, w * 0.42f))
            val bite = Path().apply { addOval(androidx.compose.ui.geometry.Rect(Offset(c.x + w * 0.2f, c.y - w * 0.1f), w * 0.36f)) }
            path.op(path, bite, androidx.compose.ui.graphics.PathOperation.Difference)
        }
        RuneShape.SUN -> {
            drawCircle(color, w * 0.24f, c)
            drawCircle(ink, w * 0.24f, c, style = Stroke(w * 0.05f))
            for (i in 0 until 8) {
                val a = i * PI / 4
                val from = Offset(c.x + w * 0.32f * cos(a).toFloat(), c.y + w * 0.32f * sin(a).toFloat())
                val to = Offset(c.x + w * 0.46f * cos(a).toFloat(), c.y + w * 0.46f * sin(a).toFloat())
                drawLine(ink, from, to, w * 0.11f, androidx.compose.ui.graphics.StrokeCap.Round)
                drawLine(color, from, to, w * 0.06f, androidx.compose.ui.graphics.StrokeCap.Round)
            }
            return
        }
        RuneShape.HEART -> {
            path.moveTo(c.x, c.y + w * 0.38f)
            path.cubicTo(c.x - w * 0.6f, c.y - w * 0.02f, c.x - w * 0.24f, c.y - w * 0.5f, c.x, c.y - w * 0.16f)
            path.cubicTo(c.x + w * 0.24f, c.y - w * 0.5f, c.x + w * 0.6f, c.y - w * 0.02f, c.x, c.y + w * 0.38f)
            path.close()
        }
        RuneShape.DROP -> {
            path.moveTo(c.x, c.y - w * 0.46f)
            path.cubicTo(c.x + w * 0.08f, c.y - w * 0.2f, c.x + w * 0.34f, c.y + w * 0.02f, c.x + w * 0.3f, c.y + w * 0.2f)
            path.cubicTo(c.x + w * 0.26f, c.y + w * 0.5f, c.x - w * 0.26f, c.y + w * 0.5f, c.x - w * 0.3f, c.y + w * 0.2f)
            path.cubicTo(c.x - w * 0.34f, c.y + w * 0.02f, c.x - w * 0.08f, c.y - w * 0.2f, c.x, c.y - w * 0.46f)
            path.close()
        }
    }
    drawPath(path, Brush.radialGradient(listOf(Color.White.copy(alpha = 0.55f).compositeOver(color), color), center = Offset(c.x - w * 0.12f, c.y - w * 0.14f), radius = w * 0.6f))
    drawPath(path, ink, style = Stroke(w * 0.05f))
}

private fun Color.compositeOver(background: Color): Color {
    val a = alpha
    return Color(red * a + background.red * (1 - a), green * a + background.green * (1 - a), blue * a + background.blue * (1 - a), 1f)
}

/** A six-sided die showing [value] pips. The first [lit] pips glow gold, for counting them out loud. */
@Composable
fun DieFace(value: Int, modifier: Modifier = Modifier, glow: Boolean = false, lit: Int = 0) {
    Canvas(modifier) {
        val w = size.minDimension
        val r = CornerRadius(w * 0.2f)
        if (glow) drawRoundRect(Color(0x66FFD34D), Offset(-w * 0.06f, -w * 0.06f), Size(w * 1.12f, w * 1.12f), CornerRadius(w * 0.26f))
        drawRoundRect(Brush.linearGradient(listOf(Color.White, Color(0xFFF1E6D0))), size = Size(w, w), cornerRadius = r)
        drawRoundRect(Color(0xFF3D2A1C), size = Size(w, w), cornerRadius = r, style = Stroke(w * 0.05f))
        val spots = when (value) {
            1 -> listOf(0.5f to 0.5f)
            2 -> listOf(0.28f to 0.28f, 0.72f to 0.72f)
            3 -> listOf(0.26f to 0.26f, 0.5f to 0.5f, 0.74f to 0.74f)
            4 -> listOf(0.28f to 0.28f, 0.72f to 0.28f, 0.28f to 0.72f, 0.72f to 0.72f)
            5 -> listOf(0.27f to 0.27f, 0.73f to 0.27f, 0.5f to 0.5f, 0.27f to 0.73f, 0.73f to 0.73f)
            else -> listOf(0.28f to 0.24f, 0.72f to 0.24f, 0.28f to 0.5f, 0.72f to 0.5f, 0.28f to 0.76f, 0.72f to 0.76f)
        }
        spots.forEachIndexed { i, (x, y) ->
            if (i < lit) {
                drawCircle(Color(0x88FFD34D), w * 0.14f, Offset(w * x, w * y))
                drawCircle(Color(0xFFFFB000), w * 0.095f, Offset(w * x, w * y))
            } else {
                drawCircle(Color(0xFF3D2A1C), w * 0.085f, Offset(w * x, w * y))
            }
        }
    }
}

/** A boss star: empty outline until lit. */
@Composable
fun BossStar(lit: Boolean, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.minDimension
        val c = Offset(size.width / 2, size.height / 2)
        val path = Path()
        for (i in 0 until 10) {
            val r = if (i % 2 == 0) w * 0.48f else w * 0.22f
            val a = -PI / 2 + i * PI / 5
            val p = Offset(c.x + r * cos(a).toFloat(), c.y + r * sin(a).toFloat())
            if (i == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
        }
        path.close()
        if (lit) {
            drawCircle(Color(0x55FFE066), w * 0.6f, c)
            drawPath(path, Brush.radialGradient(listOf(Color(0xFFFFF6B0), Color(0xFFFFC21F)), c, w * 0.5f))
        } else {
            drawPath(path, Color(0x33000000))
        }
        drawPath(path, Color(0xFF3D2A1C), style = Stroke(w * 0.05f))
    }
}
