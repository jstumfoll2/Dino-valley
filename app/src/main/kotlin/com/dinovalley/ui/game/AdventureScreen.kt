package com.dinovalley.ui.game

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dinovalley.R
import com.dinovalley.engine.model.Speech
import com.dinovalley.engine.rpg.hero.Power
import com.dinovalley.engine.rpg.learn.Coach
import com.dinovalley.engine.rpg.learn.Words
import com.dinovalley.engine.rpg.run.Actor
import com.dinovalley.engine.rpg.run.Beat
import com.dinovalley.engine.rpg.run.Place
import com.dinovalley.engine.rpg.run.Reply
import com.dinovalley.engine.rpg.world.RoomKind
import com.dinovalley.ui.art.Art
import com.dinovalley.ui.art.Character
import com.dinovalley.ui.art.DieFace
import com.dinovalley.ui.art.Mood
import com.dinovalley.ui.art.Picto
import com.dinovalley.ui.art.Rigs
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random
import com.dinovalley.engine.rpg.run.Mood as SceneMood

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
    val speaking by narrator.speaking.collectAsState()
    var caption by remember { mutableStateOf<List<Speech>>(emptyList()) }
    var heroMood by remember { mutableStateOf(Mood.CALM) }
    val interactive = beat !is Beat.Tell && beat !is Beat.Found

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val w = maxWidth
        val h = maxHeight
        AnimatedContent(beat.scene.place, transitionSpec = { fadeIn(tween(600)) togetherWith fadeOut(tween(600)) }, label = "place") { place ->
            Backdrop(place)
        }
        if (beat.scene.place != Place.MAP) {
            Cast(beat, interactive, speaking, heroMood, vm, w, h)
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
                is Beat.Doors -> DoorsBeat(beat, adventure.map.stops.size, say) { i, tries -> reply(Reply.Picked(i, tries)) }
                is Beat.Roll -> RollBeat(beat, say, celebrate) { used, tries -> reply(Reply.Rolled(used, tries)) }
                is Beat.Ask -> AskBeat(beat, vm.state.hero.heroClass.power == Power.SPARKLE_HINT, say, celebrate) { tries, hints, ms -> reply(Reply.Solved(tries, hints, ms)) }
                is Beat.Finale -> Unit
            }
        }

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
        Column(Modifier.align(Alignment.TopEnd).padding(10.dp), horizontalAlignment = Alignment.End) {
            BagBar(adventure.bag, h * 0.07f)
            if (beat.scene.place == Place.LAIR && beat.scene.bossStars != null) {
                BossStars(adventure.bossStarsLit, h * 0.1f, Modifier.padding(top = 6.dp))
            }
        }
    }
}

@Composable
private fun Column(modifier: Modifier, horizontalAlignment: Alignment.Horizontal, content: @Composable () -> Unit) {
    androidx.compose.foundation.layout.Column(modifier, horizontalAlignment = horizontalAlignment) { content() }
}

/** Who stands where. During challenges the visitors step back so the pictures have room. */
@Composable
private fun Cast(beat: Beat, interactive: Boolean, speaking: Boolean, heroMood: Mood, vm: GameViewModel, w: Dp, h: Dp) {
    val scene = beat.scene
    val happy = scene.mood == SceneMood.HAPPY
    val heroState = when {
        heroMood == Mood.HAPPY || happy -> Mood.HAPPY
        else -> Mood.CALM
    }
    val ruby = Actor.RUBY in scene.cast
    Character(Rigs.hero(vm.state.hero.heroClass, vm.unlocked), heroState, Modifier.at(w * 0.11f, h * 0.7f, h * 0.52f, h * 0.52f))
    if (ruby) Character(Rigs.ruby, if (happy) Mood.HAPPY else Mood.CALM, Modifier.at(w * 0.24f, h * 0.72f, h * 0.48f, h * 0.48f))
    Character(
        Rigs.babyDragon,
        when {
            heroState == Mood.HAPPY -> Mood.HAPPY
            speaking -> Mood.TALKING
            else -> Mood.CALM
        },
        Modifier.at(w * if (ruby) 0.34f else 0.27f, h * 0.8f, h * 0.36f, h * 0.36f),
    )
    val npcMood = when (scene.mood) {
        SceneMood.HAPPY -> Mood.HAPPY
        SceneMood.CALM -> Mood.CALM
        else -> Mood.CALM
    }
    val back by animateFloatAsState(if (interactive) 1f else 0f, tween(500), label = "back")
    // During a challenge the visitors wait out of the way, so the pictures have the room.
    val present by animateFloatAsState(if (beat is Beat.Ask) 0f else 1f, tween(400), label = "present")
    if (Actor.GOBLIN in scene.cast && !(interactive && beat !is Beat.Choose)) {
        Character(Rigs.goblin, if (npcMood == Mood.CALM) Mood.SCARED else npcMood, Modifier.at(w * 0.72f, h * 0.74f, h * 0.42f, h * 0.42f), facingLeft = true)
    }
    if (Actor.WIZARD in scene.cast) {
        Character(Rigs.wizard, npcMood, Modifier.at(w * (0.66f + 0.26f * back), h * (0.7f + 0.0f * back), h * 0.38f, h * 0.41f).alpha(present), facingLeft = true)
    }
    if (Actor.DRAGON in scene.cast) {
        val size = h * (0.78f - 0.36f * back)
        Character(Rigs.bigDragon, npcMood, Modifier.at(w * (0.7f + 0.18f * back), h * (0.6f - 0.18f * back), size, size).alpha(present), facingLeft = true)
    }
    if (Actor.SHADOW in scene.cast) {
        val size = h * (0.66f - 0.3f * back)
        Character(Rigs.shadow, npcMood, Modifier.at(w * (0.7f + 0.18f * back), h * (0.6f - 0.18f * back), size, size).alpha(present), facingLeft = true)
    }
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
        val res = if (beat.loot.kind == com.dinovalley.engine.rpg.run.LootKind.POTION) potionFor(beat.loot.words) else Art.loot(beat.loot.kind)
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
    com.dinovalley.engine.rpg.learn.PotionKind.entries.firstOrNull { it.title == title }?.let { Art.potion(it) } ?: R.drawable.art_potion_glow

// ------------------------------------------------------------------ choices

@Composable
private fun ChooseBeat(beat: Beat.Choose, say: (List<Speech>) -> Unit, pick: (Int) -> Unit) {
    val narrator = LocalNarrator.current
    val haptics = LocalHapticFeedback.current
    var pointing by remember { mutableIntStateOf(-1) }
    var chosen by remember { mutableIntStateOf(-1) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        say(beat.prompt)
        narrator.speak(beat.prompt)
        beat.options.forEachIndexed { i, o ->
            pointing = i
            narrator.speak(if (i == beat.options.lastIndex && i > 0) "Or... ${o.said}?" else "${o.said}?")
            delay(150)
        }
        pointing = -1
    }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val h = maxHeight
        val w = maxWidth
        val n = beat.options.size
        val card = h * 0.3f
        beat.options.forEachIndexed { i, o ->
            val lift by animateFloatAsState(if (pointing == i || chosen == i) 1.12f else 1f, spring(dampingRatio = 0.5f), label = "lift")
            Box(
                Modifier
                    .at(w * (0.5f + 0.44f * (i + 0.5f) / n), h * 0.62f, card, card)
                    .graphicsLayer { scaleX = lift; scaleY = lift; alpha = if (chosen >= 0 && chosen != i) 0.4f else 1f }
                    .clickable(NoRipple, null, enabled = chosen < 0) {
                        chosen = i
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        scope.launch {
                            narrator.speak(o.said + "!")
                            pick(i)
                        }
                    },
            ) {
                Image(painterResource(Art.choice(o.picture)), null, Modifier.fillMaxSize())
            }
        }
    }
}

// ------------------------------------------------------------------ the map

@Composable
private fun DoorsBeat(beat: Beat.Doors, stops: Int, say: (List<Speech>) -> Unit, pick: (Int, Int) -> Unit) {
    val narrator = LocalNarrator.current
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    var tries by remember { mutableIntStateOf(0) }
    var keep by remember { mutableStateOf<List<Int>?>(null) }
    var glow by remember { mutableIntStateOf(-1) }
    var wrong by remember { mutableIntStateOf(-1) }
    var opened by remember { mutableIntStateOf(-1) }
    LaunchedEffect(Unit) {
        say(beat.prompt)
        narrator.speak(beat.prompt)
    }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val h = maxHeight
        val w = maxWidth
        // The route so far, as a dotted trail across the bottom of the map.
        Canvas(Modifier.fillMaxSize()) {
            val y = size.height * 0.9f
            val x0 = size.width * 0.06f
            val x1 = size.width * 0.94f
            drawLine(Color(0xFF8A5A2A), Offset(x0, y), Offset(x1, y), size.height * 0.012f, StrokeCap.Round, PathEffect.dashPathEffect(floatArrayOf(18f, 16f)))
            for (i in 0 until stops) {
                val x = x0 + (x1 - x0) * i / (stops - 1).coerceAtLeast(1)
                val done = i < beat.stopIndex
                val here = i == beat.stopIndex
                drawCircle(if (here) Color(0xFFE5641F) else if (done) Color(0xFF8A5A2A) else Color(0x668A5A2A), size.height * if (here) 0.035f else 0.022f, Offset(x, y))
            }
        }
        // You are here: the hero's dragon on the trail.
        val n = beat.fork.doors.size
        val doorH = h * 0.5f
        val doorW = doorH * (200f / 280f)
        beat.fork.doors.forEachIndexed { i, room ->
            val visible = keep?.contains(i) ?: true
            val shake = rememberShake(if (wrong == i) tries else null)
            val open by animateFloatAsState(if (opened == i) 1f else 0f, tween(500), label = "open")
            val x = w * (0.32f + 0.6f * (i + 0.5f) / n)
            Box(
                Modifier
                    .at(x, h * 0.5f, doorW, doorH)
                    .graphicsLayer {
                        translationX = shake.value
                        alpha = if (visible) 1f else 0.25f
                        scaleX = 1f + 0.1f * open
                        scaleY = 1f + 0.1f * open
                    }
                    .clickable(NoRipple, null, enabled = opened < 0 && visible) {
                        val clue = beat.clue
                        tries += 1
                        if (clue == null) {
                            opened = i
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            scope.launch {
                                narrator.speak("The ${room.hue.word} door! Creeeak...")
                                pick(i, 1)
                            }
                            return@clickable
                        }
                        val v = Coach.judge(clue, i, tries)
                        if (v.correct) {
                            opened = i
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            scope.launch {
                                narrator.speak("Yes! The ${room.hue.word} door! Creeeak...")
                                pick(i, tries)
                            }
                        } else {
                            wrong = i
                            v.keep?.let { keep = it }
                            if (v.glow != null) glow = v.glow!!
                            scope.launch {
                                narrator.speak("Knock knock... that's the ${room.hue.word} door. Listen to the clue again!")
                                narrator.speak(clue.prompt)
                            }
                        }
                    },
            ) {
                if (glow == i) GlowRing(Modifier.fillMaxSize())
                Image(painterResource(Art.door(room.hue)), null, Modifier.fillMaxSize())
                if (beat.peek) {
                    Image(
                        painterResource(peekArt(room.kind)), null,
                        Modifier.align(Alignment.TopCenter).size(doorW * 0.42f).graphicsLayer { translationY = -(doorW * 0.5f).toPx() },
                    )
                }
            }
        }
    }
}

private fun peekArt(kind: RoomKind): Int = when (kind) {
    RoomKind.RUNE_DOOR -> R.drawable.art_door_rune_frame
    RoomKind.BRIDGE -> R.drawable.art_stone
    RoomKind.CRYSTAL_CAVE -> R.drawable.art_gem_purple
    RoomKind.LIBRARY -> R.drawable.art_book
    RoomKind.TUNNEL -> R.drawable.art_lantern
    RoomKind.MIRROR_HALL -> R.drawable.art_door_blue
    RoomKind.VAULT -> R.drawable.art_treasure
    else -> R.drawable.art_coin
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

@Composable
private fun RollBeat(beat: Beat.Roll, say: (List<Speech>) -> Unit, celebrate: () -> Unit, done: (Boolean, Int) -> Unit) {
    val narrator = LocalNarrator.current
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    var face by remember { mutableIntStateOf(Random.nextInt(1, 7)) }
    var stage by remember { mutableStateOf("ready") } // ready, rolling, rolled, reroll?, sum, done
    var usedReroll by remember { mutableStateOf(false) }
    var sumTries by remember { mutableIntStateOf(0) }
    var keep by remember { mutableStateOf<List<Int>?>(null) }
    var glow by remember { mutableIntStateOf(-1) }
    var wrong by remember { mutableStateOf<Int?>(null) }
    val spin = remember { Animatable(0f) }
    val final = if (usedReroll && beat.reroll != null) maxOf(beat.value, beat.reroll) else beat.value
    val total = final + beat.bonus
    val sumOptions = remember { com.dinovalley.engine.rpg.learn.ChallengeFactory.numberOptions(beat.value + beat.bonus, 3, Random(beat.value)) }

    LaunchedEffect(Unit) {
        say(beat.why)
        narrator.speak(beat.why)
    }

    suspend fun tumbleTo(target: Int) {
        stage = "rolling"
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        narrator.blurt("Roll!")
        spin.snapTo(0f)
        val job = scope.launch { spin.animateTo(720f, tween(900)) }
        repeat(9) {
            face = Random.nextInt(1, 7)
            delay(90)
        }
        job.join()
        face = target
    }

    suspend fun finish() {
        val bonusLine = "Plus ${Words.number(beat.bonus)} for your courage!"
        if (beat.askSum) {
            val q = "You rolled ${Words.number(final)}. $bonusLine What is ${Words.number(final)} plus ${Words.number(beat.bonus)}?"
            say(Speech.of(q))
            stage = "sum"
            narrator.speak(q)
        } else {
            val l = "$bonusLine ${Words.capital(final)} plus ${Words.number(beat.bonus)} makes ${Words.number(total)}!"
            say(Speech.of(l))
            narrator.speak(l)
            stage = "done"
            done(usedReroll, 0)
        }
    }

    fun roll() {
        if (stage != "ready") return
        scope.launch {
            tumbleTo(beat.value)
            stage = "rolled"
            celebrate()
            val l = "You rolled a ${Words.number(beat.value)}!"
            say(Speech.of(l))
            narrator.speak(l)
            if (beat.reroll != null) {
                stage = "reroll?"
                narrator.speak("Knight power! Do you want to roll again? Tap the die again, or tap the check to keep it.")
            } else {
                finish()
            }
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val h = maxHeight
        val w = maxWidth
        val t = rememberInfiniteTransition(label = "die")
        val bob by t.animateFloat(-1f, 1f, infiniteRepeatable(tween(600), RepeatMode.Reverse), label = "bob")
        val dieSize = h * 0.34f
        Box(
            Modifier.at(w * 0.62f, h * 0.48f, dieSize, dieSize)
                .graphicsLayer {
                    rotationZ = spin.value
                    translationY = if (stage == "ready" || stage == "reroll?") bob * 10f else 0f
                }
                .clickable(NoRipple, null) {
                    when (stage) {
                        "ready" -> roll()
                        "reroll?" -> scope.launch {
                            usedReroll = true
                            tumbleTo(beat.reroll!!)
                            val better = maxOf(beat.value, beat.reroll)
                            val l = "You rolled a ${Words.number(beat.reroll)}!" + if (better != beat.reroll) " You keep the ${Words.number(better)}." else ""
                            face = better
                            say(Speech.of(l))
                            narrator.speak(l)
                            finish()
                        }
                        else -> Unit
                    }
                },
        ) {
            DieFace(face, Modifier.fillMaxSize(), glow = stage == "ready")
        }
        if (stage == "reroll?") {
            RoundButton(Picto.CHECK, Palette.Go, h * 0.16f, Modifier.at(w * 0.84f, h * 0.48f, h * 0.16f, h * 0.16f)) {
                scope.launch { finish() }
            }
        }
        if (stage != "ready" && stage != "rolling" && beat.bonus > 0) {
            Text(
                "+${beat.bonus}", fontSize = with(LocalDensity.current) { (h * 0.12f).toSp() }, fontWeight = FontWeight.Black,
                color = Palette.Gold, modifier = Modifier.at(w * 0.82f, h * 0.3f, h * 0.24f, h * 0.16f),
            )
        }
        if (stage == "sum") {
            val answer = final + beat.bonus
            val options = if (answer in sumOptions) sumOptions else com.dinovalley.engine.rpg.learn.ChallengeFactory.numberOptions(answer, 3, Random(answer))
            NumberRow(
                options, h, w, keep, glow, wrong, solved = false,
                onPick = { n ->
                    sumTries += 1
                    val v = Coach.judge(options.size, options.indexOf(answer), options.indexOf(n), sumTries)
                    if (v.correct) {
                        stage = "done"
                        celebrate()
                        scope.launch {
                            narrator.speak("Yes! ${Words.capital(answer)}!")
                            done(usedReroll, sumTries)
                        }
                    } else {
                        wrong = n
                        v.keep?.let { k -> keep = k.map { options[it] } }
                        v.glow?.let { glow = options[it] }
                        scope.launch {
                            narrator.speak("Hmm, let's count the dots, then ${Words.number(beat.bonus)} more!")
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
    answer: Int? = null, enabled: Boolean = true, onPick: (Int) -> Unit,
) {
    Row(
        Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.Bottom,
    ) {
        Row(
            Modifier.padding(end = w * 0.04f, bottom = h * 0.04f),
            horizontalArrangement = Arrangement.spacedBy(h * 0.035f),
        ) {
            options.forEach { n ->
                NumberCard(
                    n, h * 0.2f, visible = keep?.contains(n) ?: true, wrong = wrong == n,
                    correct = solved && n == answer, glow = glow == n, enabled = enabled && !solved, onClick = { onPick(n) },
                )
            }
        }
    }
}

@Composable
fun NumberCard(n: Int, size: Dp, visible: Boolean, wrong: Boolean, correct: Boolean, glow: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val shake = rememberShake(if (wrong) n else null)
    val fade by animateFloatAsState(if (visible) 1f else 0.2f, label = "fade")
    Box {
        if (glow) GlowRing(Modifier.size(size))
        androidx.compose.foundation.layout.Column(
            Modifier
                .graphicsLayer { translationX = shake.value; alpha = fade; val s = if (correct) 1.12f else 1f; scaleX = s; scaleY = s }
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
            Dots(n, size * 0.065f)
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

