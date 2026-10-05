package com.littledungeon.ui.game

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.littledungeon.engine.model.Speech
import com.littledungeon.engine.rpg.run.BattleView
import com.littledungeon.engine.rpg.run.Beat
import com.littledungeon.engine.rpg.run.Journey
import com.littledungeon.engine.rpg.run.NpcView
import com.littledungeon.engine.rpg.world.LocationKind
import com.littledungeon.engine.rpg.world.Terrain
import com.littledungeon.ui.art.Art
import com.littledungeon.ui.art.Character
import com.littledungeon.ui.art.Mood
import com.littledungeon.ui.art.Picto
import com.littledungeon.ui.art.Rigs
import com.littledungeon.R
import kotlinx.coroutines.launch

// ------------------------------------------------------------------ the map of Whisperwood

/** Where the map picture sits on screen: below the caption, leaving room for the bag. */
private class MapBox(val left: Dp, val top: Dp, val width: Dp, val height: Dp) {
    fun x(f: Float): Dp = left + width * f
    fun y(f: Float): Dp = top + height * f
}

private fun kindColor(kind: LocationKind): Color = when (kind) {
    LocationKind.CAMP -> Color(0xFF4F8FD9)
    LocationKind.TOWN -> Color(0xFFFFC21F)
    LocationKind.DUNGEON -> Color(0xFF8E4AD8)
    LocationKind.WILD -> Color(0xFF4FAE45)
    LocationKind.LAIR -> Color(0xFFE5484D)
}

/**
 * Choosing where to go next. The painted map of the kingdom shows every place, the roads between
 * them and where the hero stands. Tapping a place down a road says what the road is like; tapping
 * it again (or the green button) sets off. Roads the hero was turned back from are crossed out.
 */
@Composable
fun TravelBeat(beat: Beat.Travel, journey: Journey, say: (List<Speech>) -> Unit, pick: (Int) -> Unit) {
    val narrator = LocalNarrator.current
    val sfx = LocalSfx.current
    val scope = rememberCoroutineScope()
    var ready by remember { mutableStateOf(false) }
    var selected by remember { mutableIntStateOf(-1) }
    LaunchedEffect(Unit) {
        say(beat.prompt)
        narrator.speak(beat.prompt)
        ready = true
    }
    val kingdom = journey.kingdom
    val pulse = rememberInfiniteTransition(label = "here")
    val ring by pulse.animateFloat(0.9f, 1.25f, infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "ring")

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val w = maxWidth
        val h = maxHeight
        // The painting is 16:9. It is no longer stretched to fill a long phone: at most a quarter wider than drawn, centred, with the
        // sea around it. (Places are sized to be read, so a map shown at its true shape needs the places moved: roadmap 4.4.)
        val areaW = w * 0.94f
        val areaH = h * 0.76f
        val mapW = minOf(areaW, areaH * (16f / 9f) * 1.25f)
        val box = MapBox(w * 0.03f + (areaW - mapW) / 2f, h * 0.2f, mapW, areaH)
        Image(
            painterResource(Art.place(com.littledungeon.engine.rpg.run.Place.WORLD_MAP)), null,
            Modifier.at(box.x(0.5f), box.y(0.5f), box.width, box.height).shadow(8.dp, RoundedCornerShape(24.dp)).clip(RoundedCornerShape(24.dp))
                .border(4.dp, Palette.PaperEdge, RoundedCornerShape(24.dp)),
            contentScale = ContentScale.FillBounds,
        )
        // Every road, faint; the ones out of here, bold.
        Canvas(Modifier.fillMaxSize()) {
            kingdom.roads.forEach { r ->
                val a = kingdom.location(r.a)
                val b = kingdom.location(r.b)
                val here = r.touches(beat.here)
                val dash = if (r.terrain == Terrain.ROAD) null else PathEffect.dashPathEffect(floatArrayOf(18f, 14f))
                drawLine(
                    if (here) Color(0xFF3B2A1A) else Color(0x663B2A1A),
                    Offset(box.x(a.x).toPx(), box.y(a.y).toPx()), Offset(box.x(b.x).toPx(), box.y(b.y).toPx()),
                    strokeWidth = if (here) 7f else 4f, cap = StrokeCap.Round, pathEffect = dash,
                )
            }
        }
        val size = h * 0.085f
        // Places that are not down a road from here.
        kingdom.locations.forEach { l ->
            if (beat.routes.any { it.to == l.id } || l.id == beat.here) return@forEach
            val seen = l.id in journey.visited
            Marker(l.name, kindColor(l.kind), size * 0.7f, Modifier.at(box.x(l.x), box.y(l.y), size * 2.6f, size * 1.6f), faded = !seen)
        }
        // The roads out of here.
        beat.routes.forEachIndexed { i, route ->
            val l = kingdom.location(route.to)
            val chosen = selected == i
            Box(
                Modifier
                    .at(box.x(l.x), box.y(l.y), size * 2.6f, size * 1.6f)
                    .graphicsLayer { val s = if (chosen) 1.18f else 1f; scaleX = s; scaleY = s }
                    .clickable(NoRipple, null, enabled = ready && !route.blocked) {
                        sfx.play("tap")
                        if (chosen) {
                            scope.launch { pick(i) }
                        } else {
                            selected = i
                            scope.launch { narrator.speak(route.said) }
                        }
                    },
            ) {
                Marker(route.name, kindColor(route.kind), size, Modifier.fillMaxSize(), faded = route.blocked, glowing = route.marked, ticks = route.danger ?: 0)
                if (route.blocked) Image(painterResource(Art.byName("art_talk_no") ?: R.drawable.art_key), null, Modifier.align(Alignment.TopEnd).size(size * 0.6f))
            }
        }
        // The hero, here.
        val here = kingdom.location(beat.here)
        Box(Modifier.at(box.x(here.x), box.y(here.y), size * 3.2f, size * 1.9f)) {
            Box(
                Modifier.align(Alignment.Center).size(size * 1.15f * ring).border(5.dp, Palette.Gold, CircleShape).background(Color(0x55FFE066), CircleShape),
            )
            Text(
                here.name, color = Palette.Ink, fontWeight = FontWeight.Black, fontSize = with(LocalDensity.current) { (size * 0.3f).toSp() },
                modifier = Modifier.align(Alignment.BottomCenter).background(Palette.Paper, RoundedCornerShape(50)).padding(horizontal = 8.dp),
                maxLines = 1,
            )
        }
        if (ready && selected >= 0) {
            RoundButton(Picto.CHECK, Palette.Go, h * 0.16f, Modifier.align(Alignment.BottomEnd).padding(end = w * 0.03f, bottom = h * 0.05f), pulse = true) {
                scope.launch { pick(selected) }
            }
        }
    }
}

/** A place on the map: a coloured dot, with its name. Danger shows as little red marks under it. */
@Composable
private fun Marker(name: String, color: Color, dot: Dp, modifier: Modifier, faded: Boolean = false, glowing: Boolean = false, ticks: Int = 0) {
    Column(modifier.alpha(if (faded) 0.45f else 1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Box(
            Modifier.size(dot).shadow(4.dp, CircleShape).background(color, CircleShape)
                .border(if (glowing) 6.dp else 3.dp, if (glowing) Palette.Gold else Color.White, CircleShape),
        )
        Text(
            name, color = Palette.Ink, fontWeight = FontWeight.Bold, fontSize = with(LocalDensity.current) { (dot * 0.34f).toSp() },
            modifier = Modifier.padding(top = 2.dp).background(Color(0xCCFFF6E0), RoundedCornerShape(50)).padding(horizontal = 6.dp), maxLines = 1,
        )
        if (ticks > 0) {
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp), modifier = Modifier.padding(top = 2.dp)) {
                repeat(ticks.coerceAtMost(3)) { Box(Modifier.size(dot * 0.22f).background(Color(0xFFE5484D), CircleShape)) }
            }
        }
    }
}

// ------------------------------------------------------------------ shops

/**
 * A shop counter: the things for sale, each with its picture, price and how many are already owned.
 * Tapping buys; things that cost too much fade. The shopkeeper stands behind it (the scene's person).
 */
@Composable
fun ShopBeat(beat: Beat.Shop, say: (List<Speech>) -> Unit, buy: (String) -> Unit, leave: () -> Unit) {
    val narrator = LocalNarrator.current
    val sfx = LocalSfx.current
    val scope = rememberCoroutineScope()
    var ready by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        say(beat.prompt)
        narrator.speak(beat.prompt)
        ready = true
    }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val w = maxWidth
        val h = maxHeight
        // The counter sits between the hero and the shopkeeper (who stands at the far edge), not over them.
        val panelW = w * 0.4f
        val panelH = h * 0.7f
        Column(
            Modifier.at(w * 0.6f, h * 0.58f, panelW, panelH).shadow(8.dp, RoundedCornerShape(24.dp))
                .background(Palette.Paper, RoundedCornerShape(24.dp)).border(4.dp, Palette.PaperEdge, RoundedCornerShape(24.dp)).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(beat.shopName, color = Palette.Ink, fontWeight = FontWeight.Black, fontSize = with(LocalDensity.current) { (h * 0.05f).toSp() })
                Image(painterResource(R.drawable.art_coin), null, Modifier.size(h * 0.07f))
                Text("${beat.coins}", color = Palette.Ink, fontWeight = FontWeight.Black, fontSize = with(LocalDensity.current) { (h * 0.05f).toSp() })
            }
            val perRow = 3
            val tile = minOf(h * 0.22f, (panelW - 24.dp) / (perRow * 1.1f))
            beat.stock.chunked(perRow).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(tile * 0.1f)) {
                    row.forEach { item ->
                        val can = item.canAfford && ready
                        Column(
                            Modifier.size(tile, tile * 1.15f).alpha(if (item.canAfford) 1f else 0.4f).clip(RoundedCornerShape(16.dp))
                                .background(Color(0x22C9A46A)).clickable(NoRipple, null, enabled = can) {
                                    sfx.play("tap")
                                    scope.launch {
                                        narrator.speak(Speech.of(item.name))
                                        buy(item.itemId)
                                    }
                                },
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Image(painterResource(Art.item(item.itemId)), null, Modifier.size(tile * 0.62f))
                            Text(
                                item.name, color = Palette.Ink, fontWeight = FontWeight.Bold, maxLines = 1,
                                fontSize = with(LocalDensity.current) { (tile * 0.13f).toSp() },
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Image(painterResource(R.drawable.art_coin), null, Modifier.size(tile * 0.18f))
                                Text(
                                    "${item.price}" + if (item.owned > 0) "  x${item.owned}" else "", color = Palette.Ink, fontWeight = FontWeight.Black,
                                    fontSize = with(LocalDensity.current) { (tile * 0.15f).toSp() },
                                )
                            }
                        }
                    }
                }
            }
        }
        RoundButton(Picto.CHECK, Palette.Go, h * 0.16f, Modifier.align(Alignment.BottomEnd).padding(end = w * 0.03f, bottom = h * 0.04f), pulse = ready) {
            if (ready) leave()
        }
    }
}

// ------------------------------------------------------------------ people and monsters

/**
 * A person (or a monster) from the kingdom, standing in the scene and moving their mouth while they speak. At a shop they
 * stand at the far edge, smaller, so the counter does not hide them.
 */
@Composable
fun NpcStand(npc: NpcView, speaking: Boolean, w: Dp, h: Dp, voice: (() -> Float)?, behindCounter: Boolean = false) {
    Character(
        Rigs.byArt(npc.art), if (speaking) Mood.TALKING else Mood.CALM,
        if (behindCounter) Modifier.at(w * 0.91f, h * 0.76f, h * 0.44f, h * 0.44f) else Modifier.at(w * 0.78f, h * 0.7f, h * 0.55f, h * 0.55f),
        facingLeft = true, voice = voice,
    )
}

/**
 * A fight: the foe with its name above it. Both health bars (the hero's and the foe's) are in the corner HUD, so the child
 * can see how the rounds are going without the plate crowding the top of the stage. While a puzzle is up ([portrait]) the foe
 * shrinks to a portrait between the hero and the answers, so it is never behind the answer tiles.
 */
@Composable
fun BattleStage(battle: BattleView, speaking: Boolean, w: Dp, h: Dp, voice: (() -> Float)?, portrait: Boolean = false) {
    val foe = battle.foe
    if (portrait) {
        Character(
            Rigs.byArt(foe.art), if (speaking) Mood.TALKING else Mood.CALM,
            Modifier.at(w * 0.31f, h * 0.4f, h * 0.2f, h * 0.2f), facingLeft = true, voice = voice,
        )
        return
    }
    val size = h * if (foe.boss) 0.7f else 0.5f
    Character(
        Rigs.byArt(foe.art), if (speaking) Mood.TALKING else Mood.CALM,
        Modifier.at(w * 0.76f, h * 0.68f, size, size), facingLeft = true, voice = voice,
    )
    // The name plate sits just above the foe's head.
    Box(Modifier.at(w * 0.76f, h * (0.68f - size.value / h.value / 2f - 0.04f), w * 0.34f, h * 0.07f), contentAlignment = Alignment.Center) {
        Text(
            foe.name, color = Color.White, fontWeight = FontWeight.Black, maxLines = 1,
            fontSize = with(LocalDensity.current) { (h * 0.05f).toSp() },
            modifier = Modifier.background(Color(0xCC2A1C10), RoundedCornerShape(50)).padding(horizontal = 12.dp),
        )
    }
}
