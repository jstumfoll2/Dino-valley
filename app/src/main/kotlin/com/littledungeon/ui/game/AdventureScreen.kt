package com.littledungeon.ui.game

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.littledungeon.R
import com.littledungeon.engine.model.Speech
import com.littledungeon.engine.model.Who
import com.littledungeon.feedback.FeedbackLog
import com.littledungeon.engine.rpg.hero.Power
import com.littledungeon.engine.rpg.learn.ChallengeFactory
import com.littledungeon.engine.rpg.learn.Words
import com.littledungeon.engine.rpg.run.Actor
import com.littledungeon.engine.rpg.run.Beat
import com.littledungeon.engine.rpg.run.Place
import com.littledungeon.engine.rpg.run.Reply
import com.littledungeon.engine.rpg.run.Say
import com.littledungeon.engine.rpg.run.speech
import com.littledungeon.ui.art.Art
import com.littledungeon.ui.art.Character
import com.littledungeon.ui.art.DieFace
import com.littledungeon.ui.art.Mood
import com.littledungeon.ui.art.Picto
import com.littledungeon.ui.art.Rigs
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random
import com.littledungeon.engine.rpg.run.Mood as SceneMood

/** One adventure, beat by beat. The backdrop cross-fades when the place changes. */
@Composable
fun AdventureScreen(vm: GameViewModel) {
    val beat = vm.beat ?: return
    val adventure = vm.adventure ?: return
    if (beat is Beat.Finale) {
        FinaleScreen(beat, vm)
        return
    }
    val narrator = LocalNarrator.current
    val scope = rememberCoroutineScope()
    val speakingAs by narrator.speakingAs.collectAsState()
    var caption by remember { mutableStateOf<List<Speech>>(emptyList()) }
    var heroMood by remember { mutableStateOf(Mood.CALM) }
    val interactive = beat !is Beat.Tell && beat !is Beat.Found && beat !is Beat.Night && beat.scene.battle == null
    // How far the visitors have stepped back to leave room for a challenge.
    val back by animateFloatAsState(if (interactive) 1f else 0f, tween(500), label = "back")

    // Tell the feedback note where we are.
    LaunchedEffect(vm.beatNumber) {
        FeedbackLog.screen = FeedbackLog.describe(beat)
        FeedbackLog.note("beat", FeedbackLog.describe(beat))
    }

    // While this scene plays, get the words of the next ones ready, so they start without a pause.
    LaunchedEffect(vm.beatNumber) {
        beat.speech().forEach { narrator.prepare(it) }
        adventure.upcoming.forEach { next -> next.speech().forEach { narrator.prepare(it) } }
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val w = maxWidth
        val h = maxHeight
        AnimatedContent(beat.scene.place, transitionSpec = { fadeIn(tween(600)) togetherWith fadeOut(tween(600)) }, label = "place") { place ->
            // The map of the kingdom is drawn by the travel screen itself, over the sea.
            if (place == Place.WORLD_MAP) Box(Modifier.fillMaxSize().background(Color(0xFF5FA5CC))) else Backdrop(place)
        }
        if (beat.scene.place != Place.WORLD_MAP) {
            Cast(beat, interactive, speakingAs, heroMood, back, vm, w, h)
        }

        val reply: (Reply) -> Unit = { r ->
            heroMood = Mood.CALM
            vm.reply(r)
        }
        val say: (List<Speech>) -> Unit = { caption = it }
        val celebrate: () -> Unit = {
            heroMood = Mood.HAPPY
            scope.launch {
                delay(1400)
                heroMood = Mood.CALM
            }
        }

        key(vm.beatNumber) {
            when (beat) {
                is Beat.Tell -> TellBeat(beat.lines, say) { reply(Reply.Next) }
                is Beat.Found -> FoundBeat(beat, say, celebrate) { reply(Reply.Next) }
                is Beat.Choose -> ChooseBeat(beat, say) { reply(Reply.Picked(it)) }
                is Beat.Roll -> RollBeat(beat, say, celebrate) { used, tries -> reply(Reply.Rolled(used, tries)) }
                is Beat.Ask -> AskBeat(beat, vm.state.hero.heroClass.power == Power.SPARKLE_HINT, say, celebrate) { solved -> reply(solved) }
                is Beat.Travel -> TravelBeat(beat, adventure, say) { reply(Reply.Picked(it)) }
                is Beat.Shop -> ShopBeat(beat, say, { reply(Reply.Bought(it)) }) { reply(Reply.Next) }
                is Beat.Night -> NightBeat(beat, say, { reply(Reply.Next) }) { vm.home() }
                is Beat.Finale -> Unit
            }
        }

        // Magic shows when the narrator says it happens. The wizard's hat is where a bunny can appear.
        val hat = if (Actor.WIZARD in beat.scene.cast) Offset(0.66f + 0.26f * back, 0.7f - 0.4f * back - (0.41f - 0.13f * back) * 0.4f) else null
        MagicLayer(narrator, w, h, hat)

        if (caption.isNotEmpty()) {
            Caption(
                caption,
                fontSize = with(LocalDensity.current) { (h * 0.048f).toSp() },
                modifier = Modifier.align(Alignment.TopCenter).padding(top = h * 0.02f).fillMaxWidth(0.6f),
                onClick = { scope.launch { narrator.speak(caption) } },
            )
        }
        RoundButton(Picto.LISTEN, Palette.Sky, h * 0.14f, Modifier.align(Alignment.TopStart).padding(10.dp)) {
            scope.launch { narrator.speak(caption) }
        }
        FeedbackButton(h * 0.1f, Modifier.align(Alignment.TopStart).padding(start = 10.dp + h * 0.02f, top = 18.dp + h * 0.14f))
        VoiceLoading()
        Column(Modifier.align(Alignment.TopEnd).padding(10.dp), horizontalAlignment = Alignment.End) {
            BagBar(adventure.bag, h * 0.07f)
            beat.scene.battle?.let { b -> HealthBar(b.heroHp, b.heroMaxHp, h * 0.07f, Modifier.padding(top = 6.dp), label = "You") }
        }
    }
}

@Composable
private fun Column(modifier: Modifier, horizontalAlignment: Alignment.Horizontal, content: @Composable () -> Unit) {
    androidx.compose.foundation.layout.Column(modifier, horizontalAlignment = horizontalAlignment) { content() }
}

/**
 * Who stands where. During challenges the visitors step back so the pictures have room. Only
 * the character who is speaking moves their mouth: when the narrator tells the story, nobody does.
 */
@Composable
private fun Cast(beat: Beat, interactive: Boolean, who: Who?, heroMood: Mood, back: Float, vm: GameViewModel, w: Dp, h: Dp) {
    val scene = beat.scene
    val narrator = LocalNarrator.current
    val happy = scene.mood == SceneMood.HAPPY
    val heroState = when {
        heroMood == Mood.HAPPY || happy -> Mood.HAPPY
        else -> Mood.CALM
    }
    fun voice(of: Who): (() -> Float)? = if (who == of) narrator::level else null
    val ruby = Actor.RUBY in scene.cast
    Character(Rigs.hero(vm.state.hero.heroClass, vm.unlocked, vm.adventure?.hero?.worn ?: emptyMap()), heroState, Modifier.at(w * 0.11f, h * 0.7f, h * 0.52f, h * 0.52f))
    if (ruby) {
        Character(
            Rigs.ruby, if (who == Who.RUBY) Mood.TALKING else if (happy) Mood.HAPPY else Mood.CALM,
            Modifier.at(w * 0.24f, h * 0.72f, h * 0.48f, h * 0.48f), voice = voice(Who.RUBY),
        )
    }
    Character(
        Rigs.babyDragon,
        when {
            who == Who.PET -> Mood.TALKING
            heroState == Mood.HAPPY -> Mood.HAPPY
            else -> Mood.CALM
        },
        Modifier.at(w * if (ruby) 0.34f else 0.27f, h * 0.8f, h * 0.36f, h * 0.36f),
        voice = voice(Who.PET),
    )
    val npcMood = when (scene.mood) {
        SceneMood.HAPPY -> Mood.HAPPY
        SceneMood.CALM -> Mood.CALM
        else -> Mood.CALM
    }
    // During a challenge the big visitors wait out of the way, so the pictures have the room.
    val present by animateFloatAsState(if (beat is Beat.Ask) 0f else 1f, tween(400), label = "present")
    if (Actor.GOBLIN in scene.cast && !(interactive && beat !is Beat.Choose)) {
        Character(
            Rigs.goblin, if (who == Who.GOBLIN) Mood.TALKING else if (npcMood == Mood.CALM) Mood.SCARED else npcMood,
            Modifier.at(w * 0.72f, h * 0.74f, h * 0.42f, h * 0.42f), facingLeft = true, voice = voice(Who.GOBLIN),
        )
    }
    if (Actor.WIZARD in scene.cast) {
        // The wizard stays in the corner during the puzzle: the crystals are for her lantern.
        Character(
            Rigs.wizard, if (who == Who.WIZARD) Mood.TALKING else npcMood,
            Modifier.at(w * (0.66f + 0.26f * back), h * (0.7f - 0.4f * back), h * (0.38f - 0.12f * back), h * (0.41f - 0.13f * back)),
            facingLeft = true, voice = voice(Who.WIZARD),
        )
    }
    if (Actor.DRAGON in scene.cast) {
        val size = h * (0.78f - 0.36f * back)
        Character(
            Rigs.bigDragon, if (who == Who.DRAGON) Mood.TALKING else npcMood,
            Modifier.at(w * (0.7f + 0.18f * back), h * (0.6f - 0.18f * back), size, size).alpha(present), facingLeft = true, voice = voice(Who.DRAGON),
        )
    }
    if (Actor.SHADOW in scene.cast) {
        val size = h * (0.66f - 0.3f * back)
        Character(
            Rigs.shadow, if (who == Who.SHADOW) Mood.TALKING else npcMood,
            Modifier.at(w * (0.7f + 0.18f * back), h * (0.6f - 0.18f * back), size, size).alpha(present), facingLeft = true, voice = voice(Who.SHADOW),
        )
    }
    // People and monsters of the kingdom. They step back while a puzzle needs the room.
    scene.npc?.let { npc ->
        if (beat !is Beat.Ask) NpcStand(npc, who == npc.who, w, h, voice(npc.who))
    }
    scene.battle?.let { b -> BattleStage(b, who == b.foe.who, w, h, voice(b.foe.who)) }
}

// ------------------------------------------------------------------ narration

@Composable
private fun TellBeat(lines: List<Speech>, say: (List<Speech>) -> Unit, next: () -> Unit) {
    val narrator = LocalNarrator.current
    LaunchedEffect(Unit) {
        say(lines)
        delay(250)
        narrator.speak(lines)
        delay(600)
        next()
    }
}

/**
 * Night falls at the camp. The scene darkens, the stars come out and the narrator says good night. Then the child can keep
 * going (the big arrow) or put the game down at the fire (the house): the adventure is saved either way.
 */
@Composable
private fun NightBeat(beat: Beat.Night, say: (List<Speech>) -> Unit, goOn: () -> Unit, stop: () -> Unit) {
    val narrator = LocalNarrator.current
    val dark = remember { Animatable(0f) }
    var resting by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        say(beat.lines)
        dark.animateTo(1f, tween(1800))
        narrator.speak(beat.lines)
        delay(400)
        resting = true
    }
    val t = rememberInfiniteTransition(label = "stars")
    val twinkle by t.animateFloat(0.35f, 1f, infiniteRepeatable(tween(1500), RepeatMode.Reverse), label = "twinkle")
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val h = maxHeight
        val w = maxWidth
        Canvas(Modifier.fillMaxSize().graphicsLayer { alpha = dark.value }) {
            drawRect(Color(0xB30B1236))
            for ((i, star) in NIGHT_STARS.withIndex()) {
                val bright = if (i % 2 == 0) twinkle else 1.35f - twinkle
                drawCircle(Color.White.copy(alpha = bright.coerceIn(0.2f, 1f)), size.height * (0.006f + 0.004f * (i % 3)), Offset(size.width * star.first, size.height * star.second))
            }
            // The moon: a full circle with a bite taken out of it.
            val r = size.height * 0.1f
            val centre = Offset(size.width * 0.84f, size.height * 0.24f)
            val moon = Path.combine(
                PathOperation.Difference,
                Path().apply { addOval(Rect(centre, r)) },
                Path().apply { addOval(Rect(centre + Offset(r * 0.55f, -r * 0.2f), r * 0.9f)) },
            )
            drawPath(moon, Color(0xFFFFF1B8))
        }
        if (resting) {
            RoundButton(Picto.NEXT, Palette.Go, h * 0.24f, Modifier.align(Alignment.BottomEnd).padding(end = w * 0.03f, bottom = h * 0.05f), pulse = true) { goOn() }
            RoundButton(Picto.HOME, Palette.Berry, h * 0.15f, Modifier.align(Alignment.BottomStart).padding(start = w * 0.03f, bottom = h * 0.05f)) { stop() }
        }
    }
}

/** Where the stars are, as shares of the screen. */
private val NIGHT_STARS = listOf(
    0.08f to 0.12f, 0.17f to 0.3f, 0.26f to 0.1f, 0.35f to 0.22f, 0.44f to 0.08f, 0.52f to 0.28f, 0.6f to 0.14f, 0.68f to 0.06f,
    0.74f to 0.32f, 0.93f to 0.1f, 0.12f to 0.46f, 0.3f to 0.42f, 0.57f to 0.44f, 0.8f to 0.5f, 0.96f to 0.38f, 0.04f to 0.3f,
)

@Composable
private fun FoundBeat(beat: Beat.Found, say: (List<Speech>) -> Unit, celebrate: () -> Unit, next: () -> Unit) {
    val narrator = LocalNarrator.current
    val pop = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        say(beat.lines)
        celebrate()
        pop.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = 300f))
        narrator.speak(beat.lines)
        delay(500)
        next()
    }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val h = maxHeight
        val w = maxWidth
        val t = rememberInfiniteTransition(label = "rays")
        val spin by t.animateFloat(0f, 360f, infiniteRepeatable(tween(6000)), label = "spin")
        Canvas(Modifier.at(w * 0.64f, h * 0.55f, h * 0.7f, h * 0.7f).graphicsLayer { rotationZ = spin; alpha = pop.value.coerceIn(0f, 1f) }) {
            val c = Offset(size.width / 2, size.height / 2)
            for (i in 0 until 12) {
                val a = Math.toRadians(i * 30.0)
                drawLine(
                    Color(0x66FFE680), c,
                    Offset(c.x + (size.width / 2) * kotlin.math.cos(a).toFloat(), c.y + (size.height / 2) * kotlin.math.sin(a).toFloat()),
                    size.width * 0.06f, StrokeCap.Round,
                )
            }
        }
        val res = when {
            beat.loot.kind == com.littledungeon.engine.rpg.run.LootKind.ITEM && beat.loot.itemId != null -> Art.item(beat.loot.itemId!!)
            beat.loot.kind == com.littledungeon.engine.rpg.run.LootKind.POTION -> potionFor(beat.loot.words)
            // The gem is drawn in the color the narrator says.
            beat.loot.kind == com.littledungeon.engine.rpg.run.LootKind.GEM && beat.loot.hue != null -> Art.gem(beat.loot.hue!!)
            else -> Art.loot(beat.loot.kind)
        }
        Image(
            painterResource(res), null,
            Modifier.at(w * 0.64f, h * 0.55f, h * 0.34f, h * 0.34f).graphicsLayer { scaleX = pop.value; scaleY = pop.value },
        )
        if (beat.loot.count > 1) {
            Text(
                "× ${beat.loot.count}", fontSize = 40.sp, fontWeight = FontWeight.Black, color = Color.White,
                modifier = Modifier.at(w * 0.78f, h * 0.68f, h * 0.3f, h * 0.14f),
            )
        }
    }
}

private fun potionFor(title: String): Int =
    com.littledungeon.engine.rpg.learn.PotionKind.entries.firstOrNull { it.title == title }?.let { Art.potion(it) } ?: R.drawable.art_potion_glow

// ------------------------------------------------------------------ choices

@Composable
private fun ChooseBeat(beat: Beat.Choose, say: (List<Speech>) -> Unit, pick: (Int) -> Unit) {
    val narrator = LocalNarrator.current
    val sfx = LocalSfx.current
    val haptics = LocalHapticFeedback.current
    var pointing by remember { mutableIntStateOf(-1) }
    var chosen by remember { mutableIntStateOf(-1) }
    // The pictures are not there until the question has been asked; each pops in as it is named.
    var shown by remember { mutableIntStateOf(0) }
    var ready by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        say(beat.prompt)
        narrator.speak(beat.prompt)
        beat.options.forEachIndexed { i, o ->
            pointing = i
            shown = i + 1
            narrator.speak(Say.option(o.said, last = i == beat.options.lastIndex && i > 0))
            delay(150)
        }
        pointing = -1
        ready = true
    }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val h = maxHeight
        val w = maxWidth
        val n = beat.options.size
        // Up to six answers fit across: the pictures shrink to make room.
        val card = minOf(h * 0.3f, w * 0.44f / (n * 1.1f) * 1.8f)
        beat.options.forEachIndexed { i, o ->
            val lift by animateFloatAsState(if (pointing == i || chosen == i) 1.12f else 1f, spring(dampingRatio = 0.5f), label = "lift")
            val appear by animateFloatAsState(if (i < shown) 1f else 0f, tween(380, easing = OutBack), label = "appear")
            Box(
                Modifier
                    .at(w * (0.5f + 0.44f * (i + 0.5f) / n), h * 0.62f, card, card)
                    .graphicsLayer {
                        scaleX = lift * appear
                        scaleY = lift * appear
                        alpha = (if (chosen >= 0 && chosen != i) 0.4f else 1f) * appear.coerceIn(0f, 1f)
                    }
                    .clickable(NoRipple, null, enabled = ready && chosen < 0) {
                        chosen = i
                        sfx.play("tap")
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        scope.launch {
                            narrator.speak(Say.chosen(o.said))
                            pick(i)
                        }
                    },
            ) {
                Image(painterResource(Art.choice(o)), null, Modifier.fillMaxSize())
                // Badges and items are pictures the child learns; say what they mean too, for grown-ups reading along.
                Text(
                    o.said, color = Color.White, fontWeight = FontWeight.Black, maxLines = 2, lineHeight = with(LocalDensity.current) { (card * 0.13f).toSp() },
                    fontSize = with(LocalDensity.current) { (card * 0.12f).toSp() },
                    modifier = Modifier.align(Alignment.BottomCenter).offset(y = card * 0.22f).background(Color(0xCC2A1C10), RoundedCornerShape(50)).padding(horizontal = 8.dp),
                )
            }
        }
    }
}

/** A soft pulsing glow behind the right answer, the last rung of the hint ladder. */
@Composable
fun GlowRing(modifier: Modifier) {
    val t = rememberInfiniteTransition(label = "glow")
    val a by t.animateFloat(0.35f, 0.85f, infiniteRepeatable(tween(500), RepeatMode.Reverse), label = "a")
    Box(modifier.graphicsLayer { scaleX = 1.15f; scaleY = 1.15f; alpha = a }.background(Color(0xFFFFE066), CircleShape))
}

/** Shakes side to side each time [key] changes to a non-null value. */
@Composable
fun rememberShake(key: Any?): Animatable<Float, *> {
    val x = remember { Animatable(0f) }
    LaunchedEffect(key) {
        if (key != null) for (v in listOf(-16f, 14f, -10f, 6f, 0f)) x.animateTo(v, tween(60))
    }
    return x
}

// ------------------------------------------------------------------ dice

/**
 * Two dice: tap to roll, then add them up. A wrong first answer pops a heart, and the narrator
 * counts the dots together with the child, lighting each one, before they try again.
 */
@Composable
private fun RollBeat(beat: Beat.Roll, say: (List<Speech>) -> Unit, celebrate: () -> Unit, done: (Boolean, Int) -> Unit) {
    val narrator = LocalNarrator.current
    val sfx = LocalSfx.current
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    var faces by remember { mutableStateOf(listOf(Random.nextInt(1, 7), Random.nextInt(1, 7))) }
    var stage by remember { mutableStateOf("ready") } // ready, rolling, asking, reroll?, sum, counting, done
    var asked by remember { mutableStateOf(false) } // the dice can be rolled once the reason has been said
    var usedReroll by remember { mutableStateOf(false) }
    var tries by remember { mutableIntStateOf(0) }
    var keep by remember { mutableStateOf<List<Int>?>(null) }
    var glow by remember { mutableIntStateOf(-1) }
    var wrong by remember { mutableStateOf<Int?>(null) }
    var lit by remember { mutableIntStateOf(0) }
    var heartPop by remember { mutableStateOf(false) }
    val spin = remember { Animatable(0f) }
    val reroll = beat.reroll
    val final = if (usedReroll && reroll != null) listOf(beat.dice, reroll).maxBy { it.sum() } else beat.dice
    val total = final.sum()
    val options = remember(total) { ChallengeFactory.numberOptions(total, 5, Random(total)) }

    LaunchedEffect(Unit) {
        say(beat.why)
        narrator.speak(beat.why)
        asked = true
    }

    suspend fun tumbleTo(target: List<Int>) {
        stage = "rolling"
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        sfx.play("dice", 1f)
        spin.snapTo(0f)
        val job = scope.launch { spin.animateTo(720f, tween(900)) }
        repeat(9) {
            faces = listOf(Random.nextInt(1, 7), Random.nextInt(1, 7))
            delay(90)
        }
        job.join()
        faces = target
    }

    suspend fun askSum() {
        val q = Say.diceSum(final[0], final[1])
        faces = final
        say(Speech.of(q))
        // The number cards appear only after the question has been asked.
        stage = "asking"
        narrator.speak(q)
        stage = "sum"
    }

    fun roll() {
        if (stage != "ready" || !asked) return
        scope.launch {
            tumbleTo(beat.dice)
            if (reroll != null) {
                stage = "asking"
                val l = Say.ROLL_AGAIN
                say(Speech.of(l))
                narrator.speak(l)
                stage = "reroll?"
            } else {
                askSum()
            }
        }
    }

    /** Counts every dot out loud, lighting them one by one. */
    suspend fun countTogether() {
        stage = "counting"
        lit = 0
        for (k in 1..total) {
            lit = k
            narrator.speak(Say.count(k))
        }
        delay(300)
        narrator.speak(Say.diceTotal(total))
        stage = "sum"
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val h = maxHeight
        val w = maxWidth
        val t = rememberInfiniteTransition(label = "dice")
        val bob by t.animateFloat(-1f, 1f, infiniteRepeatable(tween(600), RepeatMode.Reverse), label = "bob")
        val dieSize = h * 0.26f
        faces.forEachIndexed { d, face ->
            val litHere = if (d == 0) minOf(lit, faces[0]) else (lit - faces[0]).coerceIn(0, face)
            Box(
                Modifier.at(w * (0.56f + 0.2f * d), h * 0.42f, dieSize, dieSize)
                    .graphicsLayer {
                        rotationZ = spin.value * if (d == 0) 1f else -1f
                        translationY = if (stage == "ready" || stage == "reroll?") bob * 10f * (if (d == 0) 1f else -1f) else 0f
                    }
                    .clickable(NoRipple, null) {
                        when (stage) {
                            "ready" -> roll()
                            "reroll?" -> if (reroll != null) scope.launch {
                                usedReroll = true
                                tumbleTo(reroll)
                                if (reroll.sum() < beat.dice.sum()) {
                                    narrator.speak(Say.FIRST_ROLL_BETTER)
                                }
                                askSum()
                            }
                            else -> Unit
                        }
                    },
            ) {
                DieFace(face, Modifier.fillMaxSize(), glow = stage == "ready" && asked, lit = litHere)
            }
        }
        if (stage == "reroll?") {
            RoundButton(Picto.CHECK, Palette.Go, h * 0.16f, Modifier.at(w * 0.9f, h * 0.42f, h * 0.16f, h * 0.16f)) {
                scope.launch { askSum() }
            }
        }
        if (heartPop) {
            val pop = remember { Animatable(0.5f) }
            LaunchedEffect(Unit) {
                pop.animateTo(1.6f, tween(500))
                pop.animateTo(0f, tween(400))
                heartPop = false
            }
            Text(
                "💔", fontSize = with(LocalDensity.current) { (h * 0.18f).toSp() },
                modifier = Modifier.at(w * 0.66f, h * 0.42f, h * 0.3f, h * 0.3f).graphicsLayer { scaleX = pop.value; scaleY = pop.value; alpha = pop.value.coerceIn(0f, 1f) },
            )
        }
        if (stage == "sum" || stage == "counting" || stage == "done") {
            NumberRow(
                options, h, w, keep, glow, wrong, solved = stage == "done", answer = total, enabled = stage == "sum",
                onPick = { n ->
                    tries += 1
                    if (n == total) {
                        stage = "done"
                        sfx.play("right")
                        celebrate()
                        scope.launch {
                            narrator.speak(Say.diceRight(final[0], final[1]))
                            done(usedReroll, tries)
                        }
                    } else {
                        wrong = n
                        stage = "counting"
                        sfx.play("wrong")
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        if (tries == 1) heartPop = true
                        if (tries >= 2) glow = total
                        keep = listOf(total, n).sorted()
                        scope.launch {
                            if (tries == 1) {
                                sfx.play("heart")
                                narrator.speak(Say.HEART_POPS)
                            }
                            countTogether()
                            wrong = null
                        }
                    }
                },
            )
        }
    }
}

/** A row of number cards (numeral plus dots) along the bottom right. */
@Composable
fun NumberRow(
    options: List<Int>, h: Dp, w: Dp, keep: List<Int>?, glow: Int, wrong: Int?, solved: Boolean,
    answer: Int? = null, enabled: Boolean = true, shown: Boolean = true, onPick: (Int) -> Unit,
) {
    // More cards than before: they shrink to fit beside the characters.
    val card = minOf(h * 0.2f, w * 0.56f / (options.size * 1.17f))
    Row(
        Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.Bottom,
    ) {
        Row(
            Modifier.padding(end = w * 0.04f, bottom = h * 0.04f),
            horizontalArrangement = Arrangement.spacedBy(card * 0.17f),
        ) {
            options.forEachIndexed { index, n ->
                NumberCard(
                    n, card, visible = keep?.contains(n) ?: true, wrong = wrong == n,
                    correct = solved && n == answer, glow = glow == n, enabled = enabled && !solved && shown, onClick = { onPick(n) },
                    appear = shown, index = index,
                )
            }
        }
    }
}

@Composable
fun NumberCard(
    n: Int, size: Dp, visible: Boolean, wrong: Boolean, correct: Boolean, glow: Boolean, enabled: Boolean, onClick: () -> Unit,
    appear: Boolean = true, index: Int = 0,
) {
    val shake = rememberShake(if (wrong) n else null)
    val fade by animateFloatAsState(if (visible) 1f else 0.2f, label = "fade")
    // Pops in after the question, one card after another.
    val grow = remember { Animatable(0f) }
    LaunchedEffect(appear) {
        if (appear) {
            delay(index * 120L)
            grow.animateTo(1f, tween(380, easing = OutBack))
        } else {
            grow.snapTo(0f)
        }
    }
    Box {
        if (glow) GlowRing(Modifier.size(size))
        androidx.compose.foundation.layout.Column(
            Modifier
                .graphicsLayer {
                    translationX = shake.value
                    alpha = fade * grow.value.coerceIn(0f, 1f)
                    val s = (if (correct) 1.12f else 1f) * grow.value
                    scaleX = s
                    scaleY = s
                }
                .size(size)
                .shadow(6.dp, CircleShape)
                .background(Palette.Paper, CircleShape)
                .border(5.dp, if (correct) Palette.Right else Palette.PaperEdge, CircleShape)
                .clickable(enabled = enabled && visible, onClick = onClick),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            val fs = with(LocalDensity.current) { (size * 0.4f).toSp() }
            Text(n.toString(), fontSize = fs, lineHeight = fs, fontWeight = FontWeight.Black, color = Palette.Ink)
            if (n <= 12) Dots(n, size * 0.065f)
        }
    }
}

@Composable
private fun Dots(count: Int, dot: Dp) {
    val perRow = 5
    androidx.compose.foundation.layout.Column(horizontalAlignment = Alignment.CenterHorizontally) {
        for (row in 0 until (count + perRow - 1) / perRow) {
            Row(horizontalArrangement = Arrangement.spacedBy(dot * 0.4f)) {
                repeat(minOf(perRow, count - row * perRow)) {
                    Box(Modifier.size(dot).background(Color(0xFFE5641F), CircleShape))
                }
            }
            Box(Modifier.size(dot * 0.3f))
        }
    }
}

