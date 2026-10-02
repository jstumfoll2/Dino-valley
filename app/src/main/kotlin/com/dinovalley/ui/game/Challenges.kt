package com.dinovalley.ui.game

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dinovalley.R
import com.dinovalley.audio.Narrator
import com.dinovalley.audio.Sfx
import com.dinovalley.engine.model.Speech
import com.dinovalley.engine.rpg.learn.AddChallenge
import com.dinovalley.engine.rpg.learn.Coach
import com.dinovalley.engine.rpg.learn.ColorChallenge
import com.dinovalley.engine.rpg.learn.CountChallenge
import com.dinovalley.engine.rpg.learn.GemSize
import com.dinovalley.engine.rpg.learn.Ingredient
import com.dinovalley.engine.rpg.learn.LetterChallenge
import com.dinovalley.engine.rpg.learn.MapChallenge
import com.dinovalley.engine.rpg.learn.MemoryChallenge
import com.dinovalley.engine.rpg.learn.NumberChallenge
import com.dinovalley.engine.rpg.learn.PatternChallenge
import com.dinovalley.engine.rpg.learn.PickOne
import com.dinovalley.engine.rpg.learn.RecipeChallenge
import com.dinovalley.engine.rpg.learn.RecipeStep
import com.dinovalley.engine.rpg.learn.Thing
import com.dinovalley.engine.rpg.learn.TraceChallenge
import com.dinovalley.engine.rpg.learn.SortChallenge
import com.dinovalley.engine.rpg.learn.SkipCountChallenge
import com.dinovalley.engine.rpg.learn.PuzzleChallenge
import com.dinovalley.engine.rpg.learn.Sortable
import com.dinovalley.engine.rpg.learn.Words
import com.dinovalley.engine.rpg.run.Beat
import com.dinovalley.engine.rpg.run.Prop
import com.dinovalley.engine.rpg.run.Say
import com.dinovalley.ui.art.Art
import com.dinovalley.ui.art.RuneIcon
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.cos
import kotlin.math.sin

/**
 * Misses and hints for one challenge. The help follows the hint ladder: nothing extra after the
 * first miss (just the funny moment), fewer choices after the second, a glow after the third.
 * The Wizard's sparkle power brings each rung one miss sooner.
 */
private class Ladder(private val sparkle: Boolean) {
    var misses by mutableIntStateOf(0)
    var hints by mutableIntStateOf(0)
    private val start = System.currentTimeMillis()
    val millis: Long get() = System.currentTimeMillis() - start
    val tries: Int get() = misses + 1

    /** The try number to hand to [Coach.judge] for the next answer. */
    val coachTry: Int get() = misses + 1 + if (sparkle) 1 else 0

    /** Help to give now: 0 none, 1 fewer choices, 2 the answer glows. */
    val tier: Int get() = if (misses == 0) 0 else (misses - 1 + if (sparkle) 1 else 0).coerceAtMost(2)

    fun noteHints(n: Int) {
        hints = maxOf(hints, n)
    }
}

/** What every challenge screen shares: the ladder, the narrator, and how a challenge ends. */
private class Turn(
    val beat: Beat.Ask,
    val ladder: Ladder,
    private val sparkle: Boolean,
    private val narrator: Narrator,
    private val sfx: Sfx,
    private val scope: CoroutineScope,
    val say: (List<Speech>) -> Unit,
    private val celebrate: () -> Unit,
    private val solved: (Int, Int, Long) -> Unit,
) {
    var done by mutableStateOf(false)
        private set

    /** True while a wrong answer's feedback plays; taps wait for it, so guessing fast doesn't pay. */
    var busy by mutableStateOf(false)
        private set

    private var lastMiss = 0L

    /**
     * A wrong answer: an "uh-oh" sound right away, then a funny story moment the first time and
     * help after that. Misses in quick succession (tapping everything) get "slow down" instead.
     * Returns the hint tier to show. [again] is said last, given that tier.
     */
    fun miss(again: (tier: Int) -> List<Speech> = { beat.challenge.prompt }): Int {
        ladder.misses += 1
        val tier = ladder.tier
        val now = System.currentTimeMillis()
        val rushing = ladder.misses >= 2 && now - lastMiss < RUSH_MILLIS
        lastMiss = now
        sfx.play("wrong", 0.9f)
        busy = true
        scope.launch {
            try {
                when {
                    rushing -> narrator.speak(Say.SLOW_DOWN)
                    ladder.misses == 1 && tier == 0 -> {
                        narrator.speak(beat.oops)
                        narrator.speak(Say.TRY_AGAIN)
                    }
                    ladder.misses == 1 -> {
                        narrator.speak(beat.oops)
                        narrator.speak(Say.SPARKLE)
                    }
                    tier >= 2 -> narrator.speak(Speech.of(Say.GLOW))
                    else -> narrator.speak(Speech.of(Say.WHISPER))
                }
            } finally {
                busy = false
                lastMiss = System.currentTimeMillis()
            }
            narrator.speak(again(tier))
        }
        return tier
    }

    fun win() {
        if (done) return
        done = true
        sfx.play("right")
        celebrate()
        scope.launch {
            say(beat.yay)
            narrator.speak(beat.yay)
            delay(400)
            solved(ladder.tries, ladder.hints, ladder.millis)
        }
    }

    private companion object {
        /** A miss this soon after the last one's feedback ended means guessing, not thinking. */
        const val RUSH_MILLIS = 1500L
    }
}

/** The part of the screen the challenge uses: right of the hero and dragon, under the caption. */
private class Zone(val w: Dp, val h: Dp) {
    val left: Dp = w * 0.4f
    val right: Dp = w * 0.98f
    val width: Dp get() = right - left
    val cx: Dp get() = (left + right) / 2

    /** A tile size that fits [n] tiles across, never bigger than [max]. */
    fun tile(n: Int, max: Dp): Dp = minOf(max, width / (n * 1.22f))

    /** Centers of [n] tiles in a row. */
    fun row(n: Int, tile: Dp): List<Dp> {
        val gap = tile * 0.2f
        val total = tile * n + gap * (n - 1)
        val first = cx - total / 2 + tile / 2
        return List(n) { first + (tile + gap) * it }
    }
}

/** One learning challenge, drawn as part of the room's story. */
@Composable
fun AskBeat(beat: Beat.Ask, sparkle: Boolean, say: (List<Speech>) -> Unit, celebrate: () -> Unit, solved: (Int, Int, Long) -> Unit) {
    val narrator = LocalNarrator.current
    val scope = rememberCoroutineScope()
    val sfx = LocalSfx.current
    val turn = remember { Turn(beat, Ladder(sparkle), sparkle, narrator, sfx, scope, say, celebrate, solved) }
    val c = beat.challenge
    LaunchedEffect(Unit) {
        // The memory doors say their own words: first what to remember, then the question.
        if (c !is MemoryChallenge) {
            say(c.prompt)
            narrator.speak(c.prompt)
        }
    }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val zone = Zone(maxWidth, maxHeight)
        when (c) {
            is CountChallenge -> CountRoom(c, turn, zone)
            is AddChallenge -> AddRoom(c, turn, zone)
            is ColorChallenge -> GemRoom(c, turn, zone)
            is PatternChallenge -> RuneDoor(c, turn, zone)
            is LetterChallenge -> if (beat.prop == Prop.CHEST) ChestLock(c, c.options.map { it.toString() }, turn, zone) else SpellBooks(c, turn, zone)
            is NumberChallenge -> ChestLock(c, c.options.map { it.toString() }, turn, zone)
            is MapChallenge -> MapPick(c, turn, zone)
            is TraceChallenge -> TraceRoom(c, turn, zone)
            is MemoryChallenge -> MemoryRoom(c, turn, zone)
            is RecipeChallenge -> PotionRoom(c, turn, zone)
            is SortChallenge -> SortRoom(c, turn, zone)
            is SkipCountChallenge -> PondRoom(c, turn, zone)
            is PuzzleChallenge -> PuzzleRoom(c, turn, zone)
        }
    }
}

// ------------------------------------------------------------------ pick one picture

private class Pick(private val turn: Turn, private val c: PickOne) {
    var keep by mutableStateOf<List<Int>?>(null)
    var glow by mutableIntStateOf(-1)
    var wrong by mutableIntStateOf(-1)
    var wrongTick by mutableIntStateOf(0)
    var chosen by mutableIntStateOf(-1)

    /** Answers already tried: crossed out, so the child sees what didn't work. */
    val tried = mutableStateListOf<Int>()

    fun visible(i: Int) = keep?.contains(i) ?: true

    fun pick(i: Int) {
        if (turn.done || turn.busy || i < 0 || !visible(i) || i in tried) return
        val v = Coach.judge(c, i, turn.ladder.coachTry)
        turn.ladder.noteHints(v.hints)
        if (v.correct) {
            chosen = i
            turn.win()
        } else {
            wrong = i
            wrongTick += 1
            tried += i
            v.keep?.let { keep = it }
            v.glow?.let { glow = it }
            turn.miss()
        }
    }
}

/** One tappable picture: shakes when wrong, fades when hinted away, pops when right. */
@Composable
private fun Tile(pick: Pick, i: Int, modifier: Modifier, content: @Composable BoxScope.() -> Unit) {
    val shake = rememberShake(if (pick.wrong == i) pick.wrongTick else null)
    val fade by animateFloatAsState(if (pick.visible(i)) 1f else 0.2f, label = "fade")
    val pop by animateFloatAsState(if (pick.chosen == i) 1.18f else 1f, spring(dampingRatio = 0.4f), label = "pop")
    Box(
        modifier
            .graphicsLayer {
                translationX = shake.value
                alpha = if (i in pick.tried) minOf(fade, 0.6f) else fade
                scaleX = pop
                scaleY = pop
            }
            .clickable(NoRipple, null) { pick.pick(i) },
        contentAlignment = Alignment.Center,
    ) {
        if (pick.glow == i) GlowRing(Modifier.fillMaxSize())
        content()
        if (i in pick.tried) WrongMark(Modifier.fillMaxSize())
    }
}

/** A big soft red cross over an answer that was tried and wasn't right. */
@Composable
fun WrongMark(modifier: Modifier) {
    val pop = remember { Animatable(0f) }
    LaunchedEffect(Unit) { pop.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 500f)) }
    Canvas(modifier.graphicsLayer { scaleX = pop.value; scaleY = pop.value }) {
        val w = size.minDimension
        val c = Offset(size.width / 2, size.height / 2)
        val r = w * 0.32f
        val stroke = w * 0.12f
        for (sign in listOf(1f, -1f)) {
            drawLine(Color.White, c + Offset(-r, -r * sign), c + Offset(r, r * sign), stroke * 1.5f, StrokeCap.Round)
            drawLine(Color(0xFFE5484D), c + Offset(-r, -r * sign), c + Offset(r, r * sign), stroke, StrokeCap.Round)
        }
    }
}

/** A soft round spot for a picture to sit on, so it stands out from busy backgrounds. */
private fun Modifier.spot(): Modifier = this
    .shadow(4.dp, CircleShape)
    .background(Color(0xDDFFF6E0), CircleShape)
    .border(3.dp, Palette.PaperEdge, CircleShape)

// ------------------------------------------------------------------ counting

@Composable
private fun CountRoom(c: CountChallenge, turn: Turn, z: Zone) {
    val pick = remember { Pick(turn, c) }
    val narrator = LocalNarrator.current
    val counted = remember { mutableStateListOf<Int>() }
    // Tapping a thing counts it out loud, the way grown-ups count with a finger.
    ThingPile(c.thing, c.count, 0, z.left, z.right, z.h * 0.22f, z.h * 0.7f, counted) { i ->
        if (i !in counted) {
            counted += i
            narrator.blurt(Say.count(counted.size))
        }
    }
    Numbers(c.options, c.count, pick, turn, z)
}

@Composable
private fun AddRoom(c: AddChallenge, turn: Turn, z: Zone) {
    val pick = remember { Pick(turn, c) }
    val top = z.h * 0.22f
    val bottom = z.h * 0.7f
    if (c.missingAddend) {
        // What we have, and empty spaces for what's still missing.
        ThingPile(c.thing, c.have, if (turn.done) 0 else c.more, z.left, z.right, top, bottom, extra = if (turn.done) c.more else 0)
    } else {
        val mid = z.cx
        val gap = z.width * 0.06f
        ThingPile(c.thing, c.have, 0, z.left, mid - gap, top, bottom)
        val fs = with(LocalDensity.current) { (z.h * 0.12f).toSp() }
        Text(
            "+", fontSize = fs, lineHeight = fs, fontWeight = FontWeight.Black, color = Palette.Gold,
            modifier = Modifier.at(mid, (top + bottom) / 2, z.h * 0.12f, z.h * 0.16f),
        )
        ThingPile(c.thing, c.more, 0, mid + gap, z.right, top, bottom)
    }
    Numbers(c.options, c.solution, pick, turn, z)
}

/** The answer cards along the bottom: numerals with dots, so counting the dots works too. */
@Composable
private fun Numbers(options: List<Int>, answer: Int, pick: Pick, turn: Turn, z: Zone) {
    NumberRow(
        options, z.h, z.w,
        keep = pick.keep?.map { options[it] },
        glow = if (pick.glow >= 0) options[pick.glow] else -1,
        wrong = if (pick.wrong >= 0) options[pick.wrong] else null,
        solved = turn.done,
        answer = answer,
    ) { n -> pick.pick(options.indexOf(n)) }
}

/**
 * [count] things in a tidy heap between [x0] and [x1], then [ghosts] empty outlines. [extra]
 * things are drawn in where the ghosts were, once the answer is found.
 */
@Composable
private fun ThingPile(
    thing: Thing, count: Int, ghosts: Int, x0: Dp, x1: Dp, top: Dp, bottom: Dp,
    counted: List<Int> = emptyList(), extra: Int = 0, onTap: ((Int) -> Unit)? = null,
) {
    val total = count + maxOf(ghosts, extra)
    if (total == 0) return
    val cols = if (total <= 5) total else (total + 1) / 2
    val rows = (total + cols - 1) / cols
    val item = minOf((x1 - x0) / (cols * 1.15f), (bottom - top) / (rows * 1.2f), (bottom - top) * 0.42f)
    val cx = (x0 + x1) / 2
    val cy = (top + bottom) / 2
    val res = Art.thing(thing)
    val badge = with(LocalDensity.current) { (item * 0.22f).toSp() }
    for (i in 0 until total) {
        val row = i / cols
        val inRow = if (row == rows - 1) total - row * cols else cols
        val col = i % cols
        val x = cx + item * 1.12f * (col - (inRow - 1) / 2f)
        val y = cy + item * 1.18f * (row - (rows - 1) / 2f)
        val ghost = i >= count && i - count >= extra
        val tilt = ((i * 37) % 21 - 10).toFloat()
        val order = counted.indexOf(i)
        val bounce by animateFloatAsState(if (order >= 0) 1.12f else 1f, spring(dampingRatio = 0.35f), label = "bounce")
        Box(
            Modifier.at(x, y, item, item)
                .graphicsLayer {
                    rotationZ = tilt
                    scaleX = bounce
                    scaleY = bounce
                    alpha = if (ghost) 0.28f else 1f
                }
                .then(if (onTap != null && !ghost) Modifier.clickable(NoRipple, null) { onTap(i) } else Modifier),
        ) {
            if (ghost) {
                Box(Modifier.fillMaxSize().padding(item * 0.06f).border(3.dp, Color.White, CircleShape))
            }
            Image(painterResource(res), null, Modifier.fillMaxSize())
            if (order >= 0) {
                Box(
                    Modifier.align(Alignment.TopEnd).size(item * 0.36f).background(Palette.Gold, CircleShape).border(2.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("${order + 1}", fontSize = badge, lineHeight = badge, fontWeight = FontWeight.Black, color = Palette.Ink)
                }
            }
        }
    }
}

// ------------------------------------------------------------------ colors

@Composable
private fun GemRoom(c: ColorChallenge, turn: Turn, z: Zone) {
    val pick = remember { Pick(turn, c) }
    val tile = z.tile(c.options.size, z.h * 0.28f)
    z.row(c.options.size, tile).forEachIndexed { i, x ->
        val gem = c.options[i]
        Tile(pick, i, Modifier.at(x, z.h * 0.6f, tile, tile).spot()) {
            val s = if (gem.size == GemSize.BIG) 0.86f else 0.5f
            Image(painterResource(Art.gem(gem.hue)), null, Modifier.size(tile * s))
        }
    }
}

// ------------------------------------------------------------------ patterns: the rune door

/** Socket centers on the rune door art, in its 600 x 700 picture. */
private const val SOCKETS = 7

@Composable
private fun RuneDoor(c: PatternChallenge, turn: Turn, z: Zone) {
    val pick = remember { Pick(turn, c) }
    // Seven sockets: the pattern and the empty one. Long patterns drop their first symbol.
    val shown = if (c.shown.size >= SOCKETS) c.shown.takeLast(SOCKETS - 1) else c.shown
    val open by animateFloatAsState(if (turn.done) 1f else 0f, tween(1400, delayMillis = 900), label = "open")
    val doorH = z.h * 0.74f
    val doorW = doorH * (600f / 700f)
    val doorX = z.left + doorW / 2
    val doorY = z.h * 0.6f
    val t = rememberInfiniteTransition(label = "rune")
    val pulse by t.animateFloat(0.75f, 1.15f, infiniteRepeatable(tween(600), RepeatMode.Reverse), label = "pulse")
    Box(Modifier.at(doorX, doorY, doorW, doorH)) {
        Image(
            painterResource(R.drawable.art_door_rune_left), null,
            Modifier.fillMaxSize().graphicsLayer { translationX = -size.width * 0.4f * open },
        )
        Image(
            painterResource(R.drawable.art_door_rune_right), null,
            Modifier.fillMaxSize().graphicsLayer { translationX = size.width * 0.4f * open },
        )
        Image(painterResource(R.drawable.art_door_rune_frame), null, Modifier.fillMaxSize())
        val s = doorW * (62f / 600f)
        Box(Modifier.fillMaxSize().graphicsLayer { alpha = 1f - open }) {
            shown.forEachIndexed { i, rune ->
                RuneIcon(rune, Modifier.at(doorW * ((90f + 70f * i) / 600f), doorH * 0.5f, s, s))
            }
            val q = Modifier.at(doorW * ((90f + 70f * shown.size) / 600f), doorH * 0.5f, s, s)
            if (turn.done) {
                RuneIcon(c.next, q)
            } else {
                val fs = with(LocalDensity.current) { (s * 0.9f).toSp() }
                Box(q.graphicsLayer { scaleX = pulse; scaleY = pulse }, contentAlignment = Alignment.Center) {
                    Text("?", fontSize = fs, lineHeight = fs, fontWeight = FontWeight.Black, color = Palette.Gold)
                }
            }
        }
    }
    // The symbols to choose from, stacked beside the door.
    val n = c.options.size
    val tile = minOf(z.h * 0.16f, z.h * 0.7f / (n * 1.15f))
    val x = (doorX + doorW / 2 + z.right) / 2
    for (i in 0 until n) {
        val y = doorY + tile * 1.15f * (i - (n - 1) / 2f)
        Tile(pick, i, Modifier.at(x, y, tile, tile).spot()) {
            RuneIcon(c.options[i], Modifier.size(tile * 0.7f))
        }
    }
}

// ------------------------------------------------------------------ letters: spell books

@Composable
private fun SpellBooks(c: LetterChallenge, turn: Turn, z: Zone) {
    val pick = remember { Pick(turn, c) }
    val bookW = z.tile(c.options.size, z.h * 0.42f)
    val bookH = bookW * (220f / 300f)
    val fs = with(LocalDensity.current) { (bookH * 0.5f).toSp() }
    z.row(c.options.size, bookW).forEachIndexed { i, x ->
        Tile(pick, i, Modifier.at(x, z.h * 0.62f, bookW, bookH)) {
            Image(painterResource(R.drawable.art_book), null, Modifier.fillMaxSize())
            Text(
                c.options[i].toString(), fontSize = fs, lineHeight = fs, fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Black, color = Color(0xFF6E1F1D),
                modifier = Modifier.align(Alignment.Center).padding(bottom = bookH * 0.08f),
            )
        }
    }
}

// ------------------------------------------------------------------ chests with a magic lock

/** A treasure chest; tap the right number or letter on the lock stones to open it. */
@Composable
private fun ChestLock(c: PickOne, labels: List<String>, turn: Turn, z: Zone) {
    val pick = remember { Pick(turn, c) }
    val chestW = z.h * 0.5f
    val chestH = chestW * (220f / 260f)
    val chestX = z.left + z.width * 0.3f
    val chestY = z.h * 0.58f
    val wiggle = rememberShake(if (pick.wrong >= 0) pick.wrongTick else null)
    Box(Modifier.at(chestX, chestY, chestW, chestH).graphicsLayer { rotationZ = wiggle.value * 0.3f }) {
        if (turn.done) {
            Image(painterResource(R.drawable.art_chest_lid_open), null, Modifier.fillMaxSize())
            Image(painterResource(R.drawable.art_chest_glow), null, Modifier.fillMaxSize())
            Image(painterResource(R.drawable.art_chest_base), null, Modifier.fillMaxSize())
        } else {
            Image(painterResource(R.drawable.art_chest_base), null, Modifier.fillMaxSize())
            Image(painterResource(R.drawable.art_chest_lid_closed), null, Modifier.fillMaxSize())
        }
    }
    val n = labels.size
    val tile = minOf(z.h * 0.17f, z.h * 0.72f / (n * 1.15f))
    val x = z.left + z.width * 0.78f
    val fs = with(LocalDensity.current) { (tile * 0.55f).toSp() }
    for (i in 0 until n) {
        val y = chestY + tile * 1.15f * (i - (n - 1) / 2f)
        Tile(pick, i, Modifier.at(x, y, tile, tile).spot()) {
            Text(labels[i], fontSize = fs, lineHeight = fs, fontWeight = FontWeight.Black, color = Palette.Ink)
        }
    }
}

// ------------------------------------------------------------------ map (a door from a clue)

@Composable
private fun MapPick(c: MapChallenge, turn: Turn, z: Zone) {
    val pick = remember { Pick(turn, c) }
    val doorW = z.tile(c.doors.size, z.h * 0.36f)
    z.row(c.doors.size, doorW).forEachIndexed { i, x ->
        Tile(pick, i, Modifier.at(x, z.h * 0.6f, doorW, doorW * 1.4f)) {
            Image(painterResource(Art.door(c.doors[i].hue)), null, Modifier.fillMaxSize())
        }
    }
}

// ------------------------------------------------------------------ tracing

@Composable
private fun TraceRoom(c: TraceChallenge, turn: Turn, z: Zone) {
    val narrator = LocalNarrator.current
    val sfx = LocalSfx.current
    val strokes = c.strokes
    // One stroke at a time, in the order it is written; each fills in as the finger covers it.
    var active by remember { mutableIntStateOf(0) }
    val covered = remember { strokes.map { s -> mutableStateListOf<Boolean>().apply { repeat(s.size) { add(false) } } } }
    val trail = remember { mutableStateListOf<Offset>() }
    var guide by remember { mutableStateOf(false) }
    val cardTop = z.h * 0.22f
    val cardW = z.width
    val cardH = z.h * 0.72f
    val t = rememberInfiniteTransition(label = "trace")
    val pulse by t.animateFloat(0.7f, 1.1f, infiniteRepeatable(tween(650), RepeatMode.Reverse), label = "pulse")
    val travel by t.animateFloat(0f, 1f, infiniteRepeatable(tween(2200)), label = "travel")
    val glowDone by animateFloatAsState(if (turn.done) 1f else 0f, tween(600), label = "lit")

    fun reachable(k: Int, i: Int): Boolean {
        val cov = covered[k]
        return i == 0 || (maxOf(0, i - 3)..minOf(cov.size - 1, i + 3)).any { it != i && cov[it] }
    }

    Canvas(
        Modifier
            .offset(z.left, cardTop)
            .size(cardW, cardH)
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    if (turn.done || turn.busy) return@awaitEachGesture
                    trail.clear()
                    var gained = 0
                    val tol = c.tolerance * size.height
                    fun touch(p: Offset) {
                        if (turn.done) return
                        val k = active
                        val stroke = strokes[k]
                        val cov = covered[k]
                        // Fill in between finger samples so quick strokes still count.
                        val last = trail.lastOrNull()
                        val steps = if (last == null) 1 else ((p - last).getDistance() / (tol / 2)).toInt().coerceIn(1, 40)
                        for (step in 1..steps) {
                            val q = if (last == null) p else last + (p - last) * (step / steps.toFloat())
                            var changed = true
                            while (changed) {
                                changed = false
                                for (i in stroke.indices) {
                                    if (cov[i]) continue
                                    val pt = Offset(stroke[i].x * size.width, stroke[i].y * size.height)
                                    if ((pt - q).getDistance() <= tol && reachable(k, i)) {
                                        cov[i] = true
                                        gained += 1
                                        changed = true
                                    }
                                }
                            }
                        }
                        trail += p
                        if (cov.count { it } >= cov.size * 0.85f) {
                            for (i in cov.indices) cov[i] = true
                            trail.clear()
                            if (k == strokes.lastIndex) {
                                turn.win()
                            } else {
                                active = k + 1
                                sfx.play("poof")
                                narrator.blurt(Say.NEXT_LINE)
                            }
                        }
                    }
                    touch(down.position)
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: break
                        if (!change.pressed) break
                        touch(change.position)
                        change.consume()
                    }
                    if (turn.done) return@awaitEachGesture
                    if (gained == 0 && trail.size > 3) {
                        val tier = turn.miss { Speech.of(Say.TRACE_HELP) }
                        if (tier >= 1) {
                            guide = true
                            turn.ladder.noteHints(1)
                        }
                    } else if (gained > 0) {
                        narrator.blurt(Say.KEEP_GOING)
                    }
                }
            },
    ) {
        val stroke = size.height * 0.035f
        drawRoundRect(Color(0xAA1C1830), cornerRadius = CornerRadius(28.dp.toPx()))
        strokes.forEachIndexed { k, pts0 ->
            val pts = pts0.map { Offset(it.x * size.width, it.y * size.height) }
            val path = Path().apply {
                moveTo(pts[0].x, pts[0].y)
                pts.drop(1).forEach { lineTo(it.x, it.y) }
            }
            val now = k == active && !turn.done
            // The letter's shape, wide and soft, so the child sees the whole thing.
            drawPath(path, if (now) Color(0x55FFE680) else Color(0x26FFE680), style = Stroke(c.tolerance * size.height * 1.3f, cap = StrokeCap.Round, join = StrokeJoin.Round))
            if (now) pts.forEach { drawCircle(Color(0xCCFFF3C4), stroke * 0.26f, it) }
            // What has been drawn: gold.
            val cov = covered[k]
            for (i in 1 until pts.size) {
                if (cov[i] && cov[i - 1]) drawLine(lerp(Palette.Gold, Color.White, glowDone * 0.6f), pts[i - 1], pts[i], stroke, StrokeCap.Round)
            }
            if (now) {
                // Arrows along the stroke show which way to go.
                val every = maxOf(3, pts.size / 4)
                for (i in every until pts.size - 1 step every) {
                    val a = pts[i - 1]
                    val b = pts[i + 1]
                    val d = (b - a).let { it / maxOf(1f, it.getDistance()) }
                    val n = Offset(-d.y, d.x)
                    val tip = pts[i] + d * stroke * 0.9f
                    drawLine(Color(0xFFFFF3C4), tip, tip - d * stroke * 1.1f + n * stroke * 0.8f, stroke * 0.3f, StrokeCap.Round)
                    drawLine(Color(0xFFFFF3C4), tip, tip - d * stroke * 1.1f - n * stroke * 0.8f, stroke * 0.3f, StrokeCap.Round)
                }
                // Where to start: a green glowing spot.
                drawCircle(Color(0x6636B24A), stroke * 2.2f * pulse, pts[0])
                drawCircle(Color(0xFF36B24A), stroke * 0.9f, pts[0])
                drawCircle(Color.White, stroke * 0.9f, pts[0], style = Stroke(stroke * 0.25f))
                if (guide) {
                    val p = pts[(travel * (pts.size - 1)).toInt().coerceIn(0, pts.size - 1)]
                    drawCircle(Color(0x88FFFFFF), stroke * 1.6f, p)
                    drawCircle(Color.White, stroke * 0.7f, p)
                }
            }
        }
        for (i in 1 until trail.size) drawLine(Color(0x88FFFFFF), trail[i - 1], trail[i], stroke * 0.4f, StrokeCap.Round)
        if (glowDone > 0f) drawRoundRect(Color(0xFFFFE680).copy(alpha = 0.18f * glowDone), cornerRadius = CornerRadius(28.dp.toPx()))
    }
    if (c.glyph == null) {
        // A path or shape leads to a glowing crystal.
        val end = c.path.last()
        val gem = z.h * 0.12f
        Image(
            painterResource(R.drawable.art_gem_blue), null,
            Modifier.at(z.left + cardW * end.x, cardTop + cardH * end.y, gem, gem).graphicsLayer {
                val s = if (turn.done) 1.4f else pulse
                scaleX = s
                scaleY = s
            },
        )
    }
}

// ------------------------------------------------------------------ memory: the mirror doors

@Composable
private fun MemoryRoom(c: MemoryChallenge, turn: Turn, z: Zone) {
    val narrator = LocalNarrator.current
    val scope = rememberCoroutineScope()
    var hidden by remember { mutableStateOf(false) }
    var peeking by remember { mutableStateOf(false) }
    var step by remember { mutableIntStateOf(0) }
    val found = remember { mutableStateListOf<Int>() }
    var glow by remember { mutableIntStateOf(-1) }
    var wrong by remember { mutableIntStateOf(-1) }
    var wrongTick by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        turn.say(c.remember)
        narrator.speak(c.remember)
        delay(c.showMillis)
        hidden = true
        turn.say(c.prompt)
        narrator.speak(c.prompt)
    }
    val doorW = z.tile(c.doors.size, z.h * 0.34f)
    val doorH = doorW * (280f / 200f)
    val fs = with(LocalDensity.current) { (doorW * 0.4f).toSp() }
    z.row(c.doors.size, doorW).forEachIndexed { i, x ->
        val covered = hidden && !peeking && i !in found
        val cover by animateFloatAsState(if (covered) 1f else 0f, tween(400), label = "cover")
        val shake = rememberShake(if (wrong == i) wrongTick else null)
        val pop by animateFloatAsState(if (i in found) 1.08f else 1f, spring(dampingRatio = 0.4f), label = "pop")
        Box(
            Modifier.at(x, z.h * 0.6f, doorW, doorH)
                .graphicsLayer {
                    translationX = shake.value
                    scaleX = pop
                    scaleY = pop
                }
                .clickable(NoRipple, null) {
                    if (!hidden || peeking || turn.done || turn.busy || i in found) return@clickable
                    val want = c.sequence[step]
                    if (i == want) {
                        found += i
                        step += 1
                        glow = -1
                        narrator.blurt(Say.doorName(c.doors[i]))
                        if (step == c.sequence.size) turn.win()
                    } else {
                        wrong = i
                        wrongTick += 1
                        val tier = turn.miss { if (c.sequence.size == 1) c.prompt else Speech.of(Say.WHICH_NEXT_DOOR) }
                        if (tier >= 1) {
                            turn.ladder.noteHints(1)
                            scope.launch {
                                peeking = true
                                delay(1600)
                                peeking = false
                            }
                        }
                        if (tier >= 2) {
                            turn.ladder.noteHints(2)
                            glow = want
                        }
                    }
                },
        ) {
            if (glow == i) GlowRing(Modifier.fillMaxSize())
            Image(painterResource(Art.door(c.doors[i])), null, Modifier.fillMaxSize())
            // A silvery magic mirror slides over the door while it hides.
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(start = doorW * 0.14f, end = doorW * 0.14f, top = doorH * 0.12f, bottom = doorH * 0.08f)
                    .graphicsLayer { alpha = cover }
                    .background(
                        Brush.linearGradient(listOf(Color(0xFFE9F1FA), Color(0xFFA9B8CC), Color(0xFFDDE7F3))),
                        RoundedCornerShape(topStartPercent = 50, topEndPercent = 50, bottomEndPercent = 6, bottomStartPercent = 6),
                    )
                    .border(4.dp, Color(0xFF6B7586), RoundedCornerShape(topStartPercent = 50, topEndPercent = 50, bottomEndPercent = 6, bottomStartPercent = 6)),
                contentAlignment = Alignment.Center,
            ) {
                Text("?", fontSize = fs, lineHeight = fs, fontWeight = FontWeight.Black, color = Color(0xFF6B7586))
            }
        }
    }
}

// ------------------------------------------------------------------ potions

@Composable
private fun PotionRoom(c: RecipeChallenge, turn: Turn, z: Zone) {
    val narrator = LocalNarrator.current
    val scope = rememberCoroutineScope()
    val added = remember { mutableStateMapOf<Ingredient, Int>() }
    var stirs by remember { mutableIntStateOf(0) }
    var showCard by remember { mutableStateOf(c.riddle == null) }
    var keep by remember { mutableStateOf<Set<Ingredient>?>(null) }
    var glow by remember { mutableStateOf<Ingredient?>(null) }
    var wrong by remember { mutableStateOf<Ingredient?>(null) }
    var wrongTick by remember { mutableIntStateOf(0) }
    var flash by remember { mutableStateOf<Color?>(null) }
    val spin = remember { Animatable(0f) }
    val need = c.steps.sumOf { it.count }
    val have = added.values.sum()
    val stirring = have >= need && c.stirs > 0

    fun left(s: RecipeStep) = s.count - (added[s.ingredient] ?: 0)
    fun nextStep(): RecipeStep? = c.steps.firstOrNull { left(it) > 0 }
    fun fits(i: Ingredient): Boolean =
        if (c.ordered) nextStep()?.ingredient == i else c.steps.any { it.ingredient == i && left(it) > 0 }

    LaunchedEffect(Unit) {
        if (c.hideRecipe) {
            // Show the recipe while it's read out, then hide it: now it's a memory game too.
            withTimeoutOrNull(20_000) {
                narrator.speaking.first { it }
                narrator.speaking.first { !it }
            }
            delay(1500)
            showCard = false
        }
    }

    fun splash(color: Color) {
        flash = color
        scope.launch {
            delay(550)
            flash = null
        }
    }

    fun drop(i: Ingredient) {
        if (turn.done || turn.busy || have >= need || keep?.contains(i) == false) return
        if (fits(i)) {
            val n = (added[i] ?: 0) + 1
            added[i] = n
            keep = null
            glow = null
            splash(Art.color(i.hue))
            narrator.blurt(Say.count(n))
            if (added.values.sum() >= need) {
                if (c.stirs == 0) {
                    turn.win()
                } else {
                    scope.launch {
                        delay(700)
                        val l = Speech.of(Say.stir(c.stirs))
                        turn.say(l)
                        narrator.speak(l)
                    }
                }
            }
        } else {
            wrong = i
            wrongTick += 1
            splash(Art.color(i.hue))
            val target = nextStep()?.ingredient ?: return
            val tier = turn.miss { tier -> if (tier >= 1) Speech.of(Say.LOOK_RECIPE) else c.riddle ?: c.prompt }
            if (tier >= 1) {
                showCard = true
                keep = setOf(target, i)
                turn.ladder.noteHints(1)
            }
            if (tier >= 2) {
                glow = target
                turn.ladder.noteHints(2)
            }
        }
    }

    fun stir() {
        if (!stirring || turn.done || stirs >= c.stirs) return
        stirs += 1
        narrator.blurt(Say.count(stirs))
        scope.launch { spin.animateTo(spin.value + 360f, tween(550)) }
        if (stirs == c.stirs) turn.win()
    }

    // The cauldron: its brew turns toward the potion's color as the recipe comes together.
    val progress = (have + stirs).toFloat() / (need + c.stirs)
    val base = lerp(Color(0xFFD8E4D2), Art.potionColor(c.potion), progress)
    val brew by animateColorAsState(flash ?: base, tween(300), label = "brew")
    val potW = z.h * 0.5f
    val potH = potW * (320f / 360f)
    val potX = z.left + z.width * 0.36f
    val potY = z.h * 0.52f
    Box(Modifier.at(potX, potY, potW, potH).clickable(NoRipple, null) { stir() }) {
        Image(painterResource(R.drawable.art_cauldron_back), null, Modifier.fillMaxSize())
        Image(
            painterResource(R.drawable.art_cauldron_brew), null, Modifier.fillMaxSize(),
            colorFilter = ColorFilter.tint(brew, BlendMode.Modulate),
        )
        Image(painterResource(R.drawable.art_cauldron_front), null, Modifier.fillMaxSize())
    }
    if (stirring || (turn.done && c.stirs > 0)) {
        val spoonH = potH * 0.85f
        Image(
            painterResource(R.drawable.art_spoon), null,
            Modifier.at(potX, potY - potH * 0.32f, spoonH * (100f / 260f), spoonH)
                .graphicsLayer {
                    val a = Math.toRadians(spin.value.toDouble())
                    translationX = (cos(a) * potW.toPx() * 0.16f).toFloat()
                    translationY = (sin(a) * potH.toPx() * 0.04f).toFloat()
                    rotationZ = 18f * cos(a).toFloat()
                }
                .clickable(NoRipple, null) { stir() },
        )
    }

    RecipeCard(c, showCard, added, stirs, z)

    // The shelf of ingredients along the bottom.
    val tile = z.tile(c.shelf.size, z.h * 0.16f)
    z.row(c.shelf.size, tile).forEachIndexed { k, x ->
        val ing = c.shelf[k]
        val shake = rememberShake(if (wrong == ing) wrongTick else null)
        val fade by animateFloatAsState(if (keep?.contains(ing) == false) 0.2f else 1f, label = "fade")
        Box(
            Modifier.at(x, z.h * 0.87f, tile, tile)
                .graphicsLayer {
                    translationX = shake.value
                    alpha = fade
                }
                .spot()
                .clickable(NoRipple, null) { drop(ing) },
            contentAlignment = Alignment.Center,
        ) {
            if (glow == ing) GlowRing(Modifier.fillMaxSize())
            Image(painterResource(Art.ingredient(ing)), null, Modifier.size(tile * 0.78f))
        }
    }
}

/** The recipe as pictures: each ingredient as many times as it's needed, then the stirs. */
@Composable
private fun RecipeCard(c: RecipeChallenge, show: Boolean, added: Map<Ingredient, Int>, stirs: Int, z: Zone) {
    val cardW = z.width * 0.3f
    val cardX = z.right - cardW / 2
    val rows = c.steps.size + if (c.stirs > 0) 1 else 0
    val icon = minOf(z.h * 0.085f, cardW / 4.6f)
    val fs = with(LocalDensity.current) { (icon * 0.55f).toSp() }
    Box(
        Modifier
            .offset(cardX - cardW / 2, z.h * 0.2f)
            .size(cardW, icon * 1.25f * rows + 24.dp)
            .shadow(6.dp, RoundedCornerShape(14.dp))
            .background(Palette.Paper, RoundedCornerShape(14.dp))
            .border(3.dp, Palette.PaperEdge, RoundedCornerShape(14.dp))
            .padding(10.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (!show) {
            val big = with(LocalDensity.current) { (icon * 1.4f).toSp() }
            Text("?", fontSize = big, lineHeight = big, fontWeight = FontWeight.Black, color = Palette.PaperEdge)
        } else Column(verticalArrangement = Arrangement.spacedBy(icon * 0.25f)) {
            c.steps.forEachIndexed { k, s ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (c.ordered) {
                        Text("${k + 1}", fontSize = fs, lineHeight = fs, fontWeight = FontWeight.Black, color = Palette.Name, modifier = Modifier.padding(end = 4.dp))
                    }
                    val done = added[s.ingredient] ?: 0
                    repeat(s.count) { j ->
                        Image(
                            painterResource(Art.ingredient(s.ingredient)), null,
                            Modifier.size(icon).graphicsLayer { alpha = if (j < done) 0.3f else 1f },
                        )
                    }
                }
            }
            if (c.stirs > 0) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(painterResource(R.drawable.art_spoon), null, Modifier.size(icon * 0.5f, icon))
                    Text(
                        "× ${c.stirs}", fontSize = fs, lineHeight = fs, fontWeight = FontWeight.Black,
                        color = if (stirs >= c.stirs) Palette.Right else Palette.Ink, modifier = Modifier.padding(start = 6.dp),
                    )
                }
            }
        }
    }
}

// ------------------------------------------------------------------ sorting: the goblins' storeroom

/** A thing to sort, drawn: a gem in its color and size, or the thing itself. */
@Composable
private fun SortableImage(item: Sortable, size: Dp) {
    if (item.thing == Thing.GEM) {
        val s = if (item.size == GemSize.BIG) 1f else 0.55f
        Image(painterResource(Art.gem(item.hue)), null, Modifier.size(size * s))
    } else {
        Image(painterResource(Art.thing(item.thing)), null, Modifier.size(size * 0.9f))
    }
}

/**
 * Drag every thing into its basket. Each basket wears a tag showing what belongs in it. A
 * thing dropped in the wrong basket bounces back out.
 */
@Composable
private fun SortRoom(c: SortChallenge, turn: Turn, z: Zone) {
    val n = c.baskets.size
    val basketW = minOf(z.h * 0.34f, z.width / (n * 1.3f))
    val basketH = basketW * (200f / 220f)
    val basketY = z.h * 0.78f
    val basketXs = z.row(n, basketW)
    val count = c.items.size
    val cols = (count + 1) / 2
    val item = minOf(z.h * 0.14f, z.width / (cols * 1.35f))
    val homes = List(count) { i ->
        val row = i / cols
        val inRow = if (row == 0) cols else count - cols
        val col = i % cols
        z.cx + item * 1.3f * (col - (inRow - 1) / 2f) to z.h * (0.32f + 0.17f * row)
    }
    val placed = remember { mutableStateMapOf<Int, Int>() } // item -> basket

    // The baskets: back, the things already in it, front, and the tag.
    basketXs.forEachIndexed { b, x ->
        Image(painterResource(R.drawable.art_basket_back), null, Modifier.at(x, basketY, basketW, basketH))
        val inside = placed.filterValues { it == b }.keys.sorted()
        inside.forEachIndexed { k, i ->
            val dx = basketW * 0.16f * ((k % 4) - 1.5f)
            val dy = -basketH * (0.12f + 0.08f * (k / 4))
            Box(Modifier.at(x + dx, basketY + dy, item * 0.7f, item * 0.7f), contentAlignment = Alignment.Center) {
                SortableImage(c.items[i], item * 0.7f)
            }
        }
        Image(painterResource(R.drawable.art_basket_front), null, Modifier.at(x, basketY, basketW, basketH))
        Box(Modifier.at(x, basketY - basketH * 0.66f, basketW * 0.42f, basketW * 0.42f).spot(), contentAlignment = Alignment.Center) {
            SortableImage(c.baskets[b], basketW * 0.32f)
        }
    }

    // The things still to sort.
    c.items.forEachIndexed { i, thing ->
        if (i !in placed) key(i) { SortItem(c, i, thing, homes[i], item, turn, basketXs, basketY, basketW, basketH, placed, count) }
    }
}

/** One thing to sort, dragged by the finger; it drops into the right basket or bounces back. */
@Composable
private fun SortItem(
    c: SortChallenge, i: Int, thing: Sortable, home: Pair<Dp, Dp>, item: Dp, turn: Turn,
    basketXs: List<Dp>, basketY: Dp, basketW: Dp, basketH: Dp, placed: MutableMap<Int, Int>, count: Int,
) {
    val sfx = LocalSfx.current
    val density = LocalDensity.current
    run {
        val (hx, hy) = home
        val dx = remember { Animatable(0f) }
        val dy = remember { Animatable(0f) }
        val scope = rememberCoroutineScope()
        var dragging by remember { mutableStateOf(false) }
        Box(
            Modifier
                .at(hx, hy, item, item)
                .graphicsLayer {
                    translationX = dx.value
                    translationY = dy.value
                    val s = if (dragging) 1.2f else 1f
                    scaleX = s
                    scaleY = s
                }
                .pointerInput(i) {
                    detectDragGestures(
                        onDragStart = {
                            if (!turn.busy && !turn.done) {
                                dragging = true
                                sfx.play("tap", 0.5f)
                            }
                        },
                        onDrag = { change, amount ->
                            if (dragging) {
                                change.consume()
                                scope.launch {
                                    dx.snapTo(dx.value + amount.x)
                                    dy.snapTo(dy.value + amount.y)
                                }
                            }
                        },
                        onDragEnd = {
                            if (!dragging) return@detectDragGestures
                            dragging = false
                            // Which basket is it over?
                            val px = with(density) { hx.toPx() } + dx.value
                            val py = with(density) { hy.toPx() } + dy.value
                            val over = basketXs.indexOfFirst { bx ->
                                val cx = with(density) { bx.toPx() }
                                val cy = with(density) { basketY.toPx() }
                                val hw = with(density) { (basketW * 0.6f).toPx() }
                                val hh = with(density) { (basketH * 0.75f).toPx() }
                                px in (cx - hw)..(cx + hw) && py in (cy - hh * 1.3f)..(cy + hh)
                            }
                            when {
                                over == c.home[i] -> {
                                    placed[i] = over
                                    sfx.play("poof", 0.6f)
                                    if (placed.size == count) turn.win()
                                }
                                over >= 0 -> {
                                    turn.miss { emptyList() }
                                    scope.launch { dx.animateTo(0f, spring(dampingRatio = 0.5f)) }
                                    scope.launch { dy.animateTo(0f, spring(dampingRatio = 0.5f)) }
                                }
                                else -> {
                                    scope.launch { dx.animateTo(0f, spring(dampingRatio = 0.6f)) }
                                    scope.launch { dy.animateTo(0f, spring(dampingRatio = 0.6f)) }
                                }
                            }
                        },
                        onDragCancel = {
                            dragging = false
                            scope.launch { dx.animateTo(0f) }
                            scope.launch { dy.animateTo(0f) }
                        },
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            SortableImage(thing, item)
        }
    }
}

// ------------------------------------------------------------------ counting by jumps: the frog pond

/**
 * Lily pads across the pond, each holding the same number of things. The first pads show the
 * count so far (2, 4, 6); the last asks for its number. Tapping a pad says its count.
 */
@Composable
private fun PondRoom(c: SkipCountChallenge, turn: Turn, z: Zone) {
    val pick = remember { Pick(turn, c) }
    val narrator = LocalNarrator.current
    val n = c.shown
    val pad = minOf(z.h * 0.3f, z.width / (n * 1.12f))
    val xs = z.row(n, pad)
    val t = rememberInfiniteTransition(label = "pond")
    val pulse by t.animateFloat(0.85f, 1.15f, infiniteRepeatable(tween(600), RepeatMode.Reverse), label = "pulse")
    xs.forEachIndexed { k, x ->
        val y = z.h * (0.46f + 0.05f * kotlin.math.sin(k * 1.7f))
        Box(Modifier.at(x, y, pad, pad).clickable(NoRipple, null) { narrator.blurt(Say.count(c.step * (k + 1))) }) {
            Image(painterResource(R.drawable.art_lily_pad), null, Modifier.fillMaxSize())
            // The things on this pad, in a little cluster.
            val thing = pad * if (c.step <= 3) 0.3f else 0.2f
            for (j in 0 until c.step) {
                val a = j * 2 * Math.PI / c.step
                val r = if (c.step == 1) 0f else 0.18f
                Image(
                    painterResource(Art.thing(c.thing)), null,
                    Modifier.at(pad * (0.5f + r * kotlin.math.cos(a).toFloat()), pad * (0.42f + r * 0.8f * kotlin.math.sin(a).toFloat()), thing, thing),
                )
            }
        }
        // The count so far, under each pad; the last one is the question.
        val last = k == n - 1
        val label = if (last && !turn.done) "?" else "${c.step * (k + 1)}"
        val fs = with(LocalDensity.current) { (pad * 0.24f).toSp() }
        Box(
            Modifier.at(x, y + pad * 0.5f, pad * 0.44f, pad * 0.36f)
                .graphicsLayer { if (last && !turn.done) { scaleX = pulse; scaleY = pulse } }
                .background(if (last) Palette.Gold else Palette.Paper, RoundedCornerShape(50))
                .border(3.dp, Palette.PaperEdge, RoundedCornerShape(50)),
            contentAlignment = Alignment.Center,
        ) {
            Text(label, fontSize = fs, lineHeight = fs, fontWeight = FontWeight.Black, color = Palette.Ink)
        }
    }
    Numbers(c.options, c.total, pick, turn, z)
}

// ------------------------------------------------------------------ puzzles: the broken mosaic

/**
 * A picture in pieces. Drag each piece onto its place in the frame; it clicks in when it's close.
 * With [PuzzleChallenge.ghost] a faint copy of the picture shows where each piece goes.
 */
@Composable
private fun PuzzleRoom(c: PuzzleChallenge, turn: Turn, z: Zone) {
    val image = ImageBitmap.imageResource(Art.puzzle(c.picture))
    val frameW = minOf(z.width * 0.56f, z.h * 0.66f * 16f / 9f)
    val frameH = frameW * 9f / 16f
    val frameLeft = z.left
    val frameTop = z.h * 0.6f - frameH / 2
    val pw = frameW / c.cols
    val ph = frameH / c.rows
    val placed = remember { mutableStateListOf<Int>() }
    var hint by remember { mutableIntStateOf(-1) }
    // Loose pieces wait to the right of the frame, in a shuffled order.
    val order = remember { (0 until c.pieces).shuffled(kotlin.random.Random(c.seed)) }
    val areaLeft = frameLeft + frameW + z.width * 0.04f
    val areaW = z.right - areaLeft
    val looseCols = if (c.pieces <= 4) 1 else 2
    val scale = minOf(1f, (areaW / looseCols / (pw * 1.08f)), (z.h * 0.76f / ((c.pieces + looseCols - 1) / looseCols) / (ph * 1.1f)))

    fun slotCenter(p: Int): Pair<Dp, Dp> = (frameLeft + pw * (p % c.cols) + pw / 2) to (frameTop + ph * (p / c.cols) + ph / 2)

    // The frame, with the ghost picture or just its outline.
    Canvas(Modifier.offset(frameLeft, frameTop).size(frameW, frameH)) {
        drawRect(Color(0xCC1C1830))
        if (c.ghost) {
            drawImage(image, dstSize = IntSize(size.width.toInt(), size.height.toInt()), alpha = 0.28f)
        }
        for (k in 1 until c.cols) drawLine(Color(0x88FFF3C4), Offset(size.width * k / c.cols, 0f), Offset(size.width * k / c.cols, size.height), 3f)
        for (k in 1 until c.rows) drawLine(Color(0x88FFF3C4), Offset(0f, size.height * k / c.rows), Offset(size.width, size.height * k / c.rows), 3f)
    }
    // Pieces in place.
    placed.forEach { p -> PuzzlePiece(image, c, p, Modifier.at(slotCenter(p).first, slotCenter(p).second, pw, ph)) }
    if (hint >= 0 && hint !in placed) GlowRing(Modifier.at(slotCenter(hint).first, slotCenter(hint).second, pw * 0.7f, ph * 0.7f))
    Box(Modifier.offset(frameLeft, frameTop).size(frameW, frameH).border(5.dp, if (turn.done) Palette.Gold else Palette.PaperEdge, RoundedCornerShape(6.dp)))

    order.forEachIndexed { slot, p ->
        if (p !in placed) key(p) { LoosePiece(image, c, p, slot, looseCols, areaLeft, areaW, scale, pw, ph, z, turn, frameLeft, frameTop, frameW, frameH, placed, ::slotCenter) { hint = it } }
    }
}

/** A puzzle piece waiting beside the frame; drag it to its place. */
@Composable
private fun LoosePiece(
    image: ImageBitmap, c: PuzzleChallenge, p: Int, slot: Int, looseCols: Int, areaLeft: Dp, areaW: Dp, scale: Float, pw: Dp, ph: Dp,
    z: Zone, turn: Turn, frameLeft: Dp, frameTop: Dp, frameW: Dp, frameH: Dp, placed: MutableList<Int>,
    slotCenter: (Int) -> Pair<Dp, Dp>, setHint: (Int) -> Unit,
) {
    val sfx = LocalSfx.current
    val density = LocalDensity.current
    run {
        val col = slot % looseCols
        val row = slot / looseCols
        val hx = areaLeft + areaW * ((col + 0.5f) / looseCols)
        val hy = z.h * 0.24f + ph * scale * 1.1f * (row + 0.5f)
        val dx = remember { Animatable(0f) }
        val dy = remember { Animatable(0f) }
        val scope = rememberCoroutineScope()
        var dragging by remember { mutableStateOf(false) }
        val tilt = remember { ((p * 53) % 13 - 6).toFloat() }
        Box(
            Modifier
                .at(hx, hy, pw, ph)
                .graphicsLayer {
                    translationX = dx.value
                    translationY = dy.value
                    val s = if (dragging) 1f else scale
                    scaleX = s
                    scaleY = s
                    rotationZ = if (dragging) 0f else tilt
                    shadowElevation = if (dragging) 16f else 6f
                }
                .pointerInput(p) {
                    detectDragGestures(
                        onDragStart = {
                            if (!turn.busy && !turn.done) {
                                dragging = true
                                sfx.play("tap", 0.5f)
                            }
                        },
                        onDrag = { change, amount ->
                            if (dragging) {
                                change.consume()
                                scope.launch {
                                    dx.snapTo(dx.value + amount.x)
                                    dy.snapTo(dy.value + amount.y)
                                }
                            }
                        },
                        onDragEnd = {
                            if (!dragging) return@detectDragGestures
                            dragging = false
                            val px = with(density) { hx.toPx() } + dx.value
                            val py = with(density) { hy.toPx() } + dy.value
                            // The empty slot nearest to where it was dropped, if it's in the frame.
                            val near = (0 until c.pieces).filter { it !in placed }.minByOrNull { q ->
                                val (sx, sy) = slotCenter(q)
                                kotlin.math.hypot(with(density) { sx.toPx() } - px, with(density) { sy.toPx() } - py)
                            }
                            val inFrame = with(density) {
                                px in frameLeft.toPx()..(frameLeft + frameW).toPx() && py in frameTop.toPx()..(frameTop + frameH).toPx()
                            }
                            when {
                                inFrame && near == p -> {
                                    placed += p
                                    setHint(-1)
                                    sfx.play("unlock", 0.6f)
                                    if (placed.size == c.pieces) turn.win()
                                }
                                inFrame -> {
                                    val tier = turn.miss { emptyList() }
                                    if (tier >= 1) {
                                        setHint(p)
                                        turn.ladder.noteHints(1)
                                    }
                                    scope.launch { dx.animateTo(0f, spring(dampingRatio = 0.55f)) }
                                    scope.launch { dy.animateTo(0f, spring(dampingRatio = 0.55f)) }
                                }
                                else -> {
                                    scope.launch { dx.animateTo(0f) }
                                    scope.launch { dy.animateTo(0f) }
                                }
                            }
                        },
                        onDragCancel = {
                            dragging = false
                            scope.launch { dx.animateTo(0f) }
                            scope.launch { dy.animateTo(0f) }
                        },
                    )
                },
        ) {
            PuzzlePiece(image, c, p, Modifier.fillMaxSize())
        }
    }
}

/** One piece: its part of the picture, with a light edge. */
@Composable
private fun PuzzlePiece(image: ImageBitmap, c: PuzzleChallenge, p: Int, modifier: Modifier) {
    Canvas(modifier) {
        val sw = image.width / c.cols
        val sh = image.height / c.rows
        drawImage(
            image,
            srcOffset = IntOffset((p % c.cols) * sw, (p / c.cols) * sh),
            srcSize = IntSize(sw, sh),
            dstSize = IntSize(size.width.toInt(), size.height.toInt()),
        )
        drawRect(Color(0xCCFFF3C4), style = Stroke(3f))
    }
}
