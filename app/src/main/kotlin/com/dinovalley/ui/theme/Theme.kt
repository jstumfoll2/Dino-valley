package com.dinovalley.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object DinoColors {
    val SkyTop = Color(0xFFBFE6FF)
    val SkyBottom = Color(0xFFFFF6DC)
    val Grass = Color(0xFF8FD16A)
    val GrassDark = Color(0xFF5FAE45)
    val HillFar = Color(0xFFB7E3A0)
    val Volcano = Color(0xFFB98A6E)
    val VolcanoDark = Color(0xFF9A6F57)
    val Lava = Color(0xFFFF8A3D)
    val Ink = Color(0xFF2C3A2E)
    val Card = Color(0xFFFFFDF6)
    val Highlight = Color(0xFFFFB627)
    val Correct = Color(0xFF6CC24A)
    val Nest = Color(0xFFC79A64)
    val NestDark = Color(0xFF9C7448)

    val eggs = mapOf(
        "egg_blue" to Color(0xFF7CC8F0),
        "egg_green" to Color(0xFFA6DB7E),
        "egg_orange" to Color(0xFFFFB27A),
    )
}

@Composable
fun DinoValleyTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = DinoColors.Correct,
            background = DinoColors.SkyBottom,
            surface = DinoColors.Card,
            onSurface = DinoColors.Ink,
        ),
        content = content,
    )
}
