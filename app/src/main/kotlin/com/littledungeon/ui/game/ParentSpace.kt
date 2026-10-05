package com.littledungeon.ui.game

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.littledungeon.engine.rpg.learn.Trend
import com.littledungeon.engine.rpg.run.Settings
import kotlin.random.Random

/**
 * The grown-up space: behind a question a four-year-old cannot answer (a sum with two-digit numbers), a progress view and the settings.
 * Progress is shown in words ("getting easier", "needs time"), never as grades.
 */
@Composable
fun ParentSpace(vm: GameViewModel, close: () -> Unit) {
    var open by remember { mutableStateOf(false) }
    Dialog(onDismissRequest = close, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier
                .fillMaxWidth(0.86f)
                .heightIn(max = 340.dp)
                .background(Palette.Paper, RoundedCornerShape(24.dp))
                .border(4.dp, Palette.PaperEdge, RoundedCornerShape(24.dp))
                .padding(18.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (!open) Gate(onOpen = { open = true }, close) else Panel(vm, close)
        }
    }
}

@Composable
private fun Gate(onOpen: () -> Unit, close: () -> Unit) {
    val sum = remember { Random.nextInt(21, 49) to Random.nextInt(13, 29) }
    val answer = sum.first + sum.second
    val options = remember { listOf(answer, answer + 10, answer - 10, answer + 1).shuffled() }
    Title("For grown-ups")
    Text("What is ${sum.first} + ${sum.second}?", fontFamily = FontFamily.Serif, fontSize = 18.sp, color = Palette.Ink)
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        for (o in options) Chip("$o") { if (o == answer) onOpen() else close() }
    }
}

@Composable
private fun Panel(vm: GameViewModel, close: () -> Unit) {
    val s = vm.settings
    val progress = remember { vm.progress() }
    Title("Settings")
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Switch(s.twoTries, { vm.changeSettings(s.copy(twoTries = it)) })
        Text("A second look after a miss (one wrong answer is crossed out)", color = Palette.Ink, fontSize = 15.sp)
    }
    Stepper("Easiest puzzles: level ${s.levelFloor}", { if (s.levelFloor > 1) vm.changeSettings(s.copy(levelFloor = s.levelFloor - 1)) }) {
        if (s.levelFloor < s.levelCeiling) vm.changeSettings(s.copy(levelFloor = s.levelFloor + 1))
    }
    Stepper("Hardest puzzles: level ${s.levelCeiling}", { if (s.levelCeiling > s.levelFloor) vm.changeSettings(s.copy(levelCeiling = s.levelCeiling - 1)) }) {
        if (s.levelCeiling < 5) vm.changeSettings(s.copy(levelCeiling = s.levelCeiling + 1))
    }
    val days = Settings.DAY_CHOICES
    Chip("A day of play (then the party camps): ${s.dayMinutes} minutes, tap to change") {
        vm.changeSettings(s.copy(dayMinutes = days[(days.indexOf(s.dayMinutes) + 1) % days.size]))
    }
    Text("Changes start with the next new adventure.", color = Palette.Ink, fontSize = 13.sp)
    Title("How it is going")
    for (p in progress.filter { it.answered > 0 }) {
        val word = when (p.trend) {
            Trend.NEW -> "just started"
            Trend.GETTING_EASIER -> "getting easier"
            Trend.STEADY -> "steady"
            Trend.NEEDS_TIME -> "needs time"
        }
        Text("${p.skill.name.lowercase().replace('_', ' ')}: level ${p.level}, ${p.answered} puzzles, $word", color = Palette.Ink, fontSize = 15.sp)
    }
    if (progress.none { it.answered > 0 }) Text("Nothing yet. Play an adventure first.", color = Palette.Ink, fontSize = 15.sp)
    Chip("Done", close)
}

@Composable
private fun Title(text: String) = Text(text, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Black, fontSize = 22.sp, color = Palette.Ink)

@Composable
private fun Chip(text: String, onClick: () -> Unit) = Text(
    text, color = Palette.Ink, fontWeight = FontWeight.Bold, fontSize = 15.sp,
    modifier = Modifier.background(Color(0x33C9A46A), RoundedCornerShape(50)).border(2.dp, Palette.PaperEdge, RoundedCornerShape(50))
        .clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 8.dp),
)

@Composable
private fun Stepper(label: String, less: () -> Unit, more: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Chip("-", less)
        Text(label, color = Palette.Ink, fontSize = 15.sp, modifier = Modifier.widthIn(min = 180.dp))
        Chip("+", more)
    }
}
