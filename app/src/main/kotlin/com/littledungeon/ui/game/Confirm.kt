package com.littledungeon.ui.game

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.littledungeon.ui.art.Picto

/**
 * A question for the grown-up holding the phone, so a stray swipe or tap never ends something. The words are for them to
 * read; the child can answer by the pictures: [keep] (green) goes on as before, [change] (red) does the thing asked about.
 */
@Composable
fun ConfirmDialog(title: String, note: String, keep: Picto, change: Picto, onKeep: () -> Unit, onChange: () -> Unit) {
    Dialog(onDismissRequest = onKeep) {
        Column(
            Modifier
                .widthIn(max = 420.dp)
                .background(Palette.Paper, RoundedCornerShape(24.dp))
                .border(3.dp, Palette.PaperEdge, RoundedCornerShape(24.dp))
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(title, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Black, fontSize = 22.sp, color = Palette.Ink, textAlign = TextAlign.Center)
            Text(note, fontFamily = FontFamily.Serif, fontSize = 16.sp, color = Palette.Ink, textAlign = TextAlign.Center)
            Row(horizontalArrangement = Arrangement.spacedBy(28.dp), verticalAlignment = Alignment.CenterVertically) {
                RoundButton(change, Palette.Berry, 64.dp, onClick = onChange)
                RoundButton(keep, Palette.Go, 84.dp, pulse = true, onClick = onKeep)
            }
        }
    }
}
