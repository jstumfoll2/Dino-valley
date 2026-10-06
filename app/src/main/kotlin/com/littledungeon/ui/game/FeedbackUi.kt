package com.littledungeon.ui.game

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.littledungeon.ui.art.Picto
import com.littledungeon.ui.art.PictoIcon
import com.littledungeon.feedback.FeedbackLog

/** What kind of problem it was; one tap each, so a note can be as short as a tap. */
private val KINDS = listOf(
    "Voice cut off", "Wrong picture", "Mouth moving wrong", "Magic missing", "Map confusing",
    "Too easy", "Too hard", "Sounds wrong", "Stuck or broken", "He loved this",
)

private const val ISSUES = "https://github.com/jstumfoll2/the-little-dungeon/issues/new"

/**
 * A small speech-bubble button for grown-ups: tap it at the moment something is wrong, say what
 * it was, and send the note. The note carries the exact scene, what was said, and the adventure's
 * seed, so the problem can be found again.
 */
@Composable
fun FeedbackButton(size: Dp, modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    Box(
        modifier
            .size(size)
            .alpha(0.75f)
            .shadow(4.dp, CircleShape)
            .background(Palette.Paper, CircleShape)
            .border(3.dp, Palette.PaperEdge, CircleShape)
            .clickable {
                FeedbackLog.note("feedback", "opened")
                open = true
            },
        contentAlignment = Alignment.Center,
    ) {
        PictoIcon(Picto.SPEECH, Palette.Ink, Modifier.size(size * 0.56f))
    }
    if (open) FeedbackDialog { open = false }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FeedbackDialog(close: () -> Unit) {
    val context = LocalContext.current
    var note by remember { mutableStateOf("") }
    val tags = remember { mutableStateListOf<String>() }
    val version = remember { versionOf(context) }
    Dialog(onDismissRequest = close, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier
                .fillMaxWidth(0.86f)
                .heightIn(max = 340.dp)
                .shadow(12.dp, RoundedCornerShape(24.dp))
                .background(Palette.Paper, RoundedCornerShape(24.dp))
                .border(4.dp, Palette.PaperEdge, RoundedCornerShape(24.dp))
                .padding(18.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("Tell Claude what happened", fontSize = 22.sp, fontWeight = FontWeight.Black, color = Palette.Ink)
            Text("The scene, what was said, and the adventure are added for you.", fontSize = 14.sp, color = Palette.Ink)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                KINDS.forEach { kind ->
                    val on = kind in tags
                    Text(
                        kind, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (on) Color.White else Palette.Ink,
                        modifier = Modifier
                            .background(if (on) Palette.Sky else Color(0xFFF1E3C0), RoundedCornerShape(50))
                            .clickable { if (on) tags.remove(kind) else tags.add(kind) }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                }
            }
            OutlinedTextField(
                value = note, onValueChange = { note = it }, modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("What did you see or hear? (optional)") }, minLines = 2, maxLines = 4,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Button("Cancel", Color(0xFFB9A98A), Modifier.weight(1f), close)
                Button("Share", Palette.Sky, Modifier.weight(1f)) {
                    share(context, FeedbackLog.report(note, tags, version))
                    close()
                }
                Button("GitHub issue", Palette.Go, Modifier.weight(1.3f)) {
                    openIssue(context, note, tags, version)
                    close()
                }
            }
        }
    }
}

@Composable
private fun Button(label: String, color: Color, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .background(color, RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp, maxLines = 1)
    }
}

private fun versionOf(context: Context): String = runCatching {
    context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "?"
}.getOrDefault("?")

/** Hands the whole report to any app: messages, email, a notes app. */
private fun share(context: Context, report: String) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "Little Dungeon playtest note")
        putExtra(Intent.EXTRA_TEXT, report)
    }
    context.startActivity(Intent.createChooser(send, "Send the note").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

/**
 * Opens a new GitHub issue already filled in. A web address can only hold so much, so it carries
 * a short version and the full report goes on the clipboard to paste below it.
 */
private fun openIssue(context: Context, note: String, tags: Collection<String>, version: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("Playtest note", FeedbackLog.report(note, tags, version)))
    val title = (note.lineSequence().firstOrNull()?.takeIf { it.isNotBlank() } ?: tags.firstOrNull() ?: "Playtest note").take(70)
    val body = FeedbackLog.summary(note, tags).take(1800)
    val uri = Uri.parse(ISSUES).buildUpon()
        .appendQueryParameter("labels", "playtest")
        .appendQueryParameter("title", title)
        .appendQueryParameter("body", body)
        .build()
    Toast.makeText(context, "Full report copied. Paste it under the note.", Toast.LENGTH_LONG).show()
    context.startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}
