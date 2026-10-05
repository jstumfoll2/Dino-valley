package com.littledungeon.ui.art

import androidx.compose.foundation.Canvas
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.runtime.Composable

/** Simple picture symbols for buttons, drawn in code so there are no text labels to read. */
enum class Picto { NEXT, LISTEN, PENCIL, AGAIN, HOME, CHECK, BOOK, SPEECH, HEART, BULB, SMILE, SPARKLE, GIFT }

@Composable
fun PictoIcon(picto: Picto, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.minDimension
        val stroke = Stroke(width = w * 0.11f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        when (picto) {
            Picto.NEXT -> drawPath(
                Path().apply {
                    moveTo(w * 0.18f, w * 0.38f); lineTo(w * 0.52f, w * 0.38f); lineTo(w * 0.52f, w * 0.16f)
                    lineTo(w * 0.86f, w * 0.5f); lineTo(w * 0.52f, w * 0.84f); lineTo(w * 0.52f, w * 0.62f)
                    lineTo(w * 0.18f, w * 0.62f); close()
                },
                color,
            )
            Picto.LISTEN -> {
                drawPath(
                    Path().apply {
                        moveTo(w * 0.12f, w * 0.38f); lineTo(w * 0.3f, w * 0.38f); lineTo(w * 0.52f, w * 0.18f)
                        lineTo(w * 0.52f, w * 0.82f); lineTo(w * 0.3f, w * 0.62f); lineTo(w * 0.12f, w * 0.62f); close()
                    },
                    color,
                )
                arc(color, w * 0.5f, w * 0.2f, stroke)
                arc(color, w * 0.5f, w * 0.36f, stroke)
            }
            Picto.PENCIL -> {
                // A pencil, tip down-left.
                drawPath(
                    Path().apply {
                        moveTo(w * 0.62f, w * 0.12f); lineTo(w * 0.88f, w * 0.38f); lineTo(w * 0.4f, w * 0.86f)
                        lineTo(w * 0.14f, w * 0.86f); lineTo(w * 0.14f, w * 0.6f); close()
                    },
                    color,
                )
                drawLine(color.copy(alpha = 0.5f), Offset(w * 0.54f, w * 0.2f), Offset(w * 0.8f, w * 0.46f), w * 0.05f)
            }
            Picto.AGAIN -> {
                drawArc(color, -60f, 290f, false, Offset(w * 0.2f, w * 0.2f), Size(w * 0.6f, w * 0.6f), style = stroke)
                drawPath(
                    Path().apply { moveTo(w * 0.62f, w * 0.08f); lineTo(w * 0.86f, w * 0.26f); lineTo(w * 0.58f, w * 0.38f); close() },
                    color,
                )
            }
            Picto.HOME -> {
                drawPath(Path().apply { moveTo(w * 0.5f, w * 0.12f); lineTo(w * 0.9f, w * 0.48f); lineTo(w * 0.1f, w * 0.48f); close() }, color)
                drawRect(color, Offset(w * 0.22f, w * 0.46f), Size(w * 0.56f, w * 0.4f))
            }
            Picto.CHECK -> drawPath(
                Path().apply { moveTo(w * 0.18f, w * 0.52f); lineTo(w * 0.42f, w * 0.76f); lineTo(w * 0.84f, w * 0.26f) },
                color,
                style = Stroke(width = w * 0.15f, cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
            Picto.BOOK -> {
                drawPath(
                    Path().apply {
                        moveTo(w * 0.5f, w * 0.26f); quadraticTo(w * 0.3f, w * 0.14f, w * 0.08f, w * 0.2f); lineTo(w * 0.08f, w * 0.8f)
                        quadraticTo(w * 0.3f, w * 0.74f, w * 0.5f, w * 0.86f); quadraticTo(w * 0.7f, w * 0.74f, w * 0.92f, w * 0.8f)
                        lineTo(w * 0.92f, w * 0.2f); quadraticTo(w * 0.7f, w * 0.14f, w * 0.5f, w * 0.26f); close()
                    },
                    color,
                )
                drawLine(Color.White.copy(alpha = 0.7f), Offset(w * 0.5f, w * 0.3f), Offset(w * 0.5f, w * 0.8f), w * 0.04f)
            }
            Picto.SPEECH -> {
                drawRoundRect(color, Offset(w * 0.08f, w * 0.14f), Size(w * 0.84f, w * 0.56f), CornerRadius(w * 0.2f))
                drawPath(
                    Path().apply { moveTo(w * 0.28f, w * 0.64f); lineTo(w * 0.2f, w * 0.88f); lineTo(w * 0.5f, w * 0.68f); close() },
                    color,
                )
                for (i in 0 until 3) drawCircle(Color.White.copy(alpha = 0.85f), w * 0.055f, Offset(w * (0.3f + 0.2f * i), w * 0.42f))
            }
            Picto.HEART -> drawPath(
                Path().apply {
                    moveTo(w * 0.5f, w * 0.86f)
                    cubicTo(w * 0.02f, w * 0.5f, w * 0.26f, w * 0.1f, w * 0.5f, w * 0.34f)
                    cubicTo(w * 0.74f, w * 0.1f, w * 0.98f, w * 0.5f, w * 0.5f, w * 0.86f)
                    close()
                },
                color,
            )
            Picto.BULB -> {
                drawCircle(color, w * 0.3f, Offset(w * 0.5f, w * 0.4f))
                drawRoundRect(color, Offset(w * 0.38f, w * 0.66f), Size(w * 0.24f, w * 0.2f), CornerRadius(w * 0.05f))
                drawLine(Color.White.copy(alpha = 0.7f), Offset(w * 0.42f, w * 0.74f), Offset(w * 0.58f, w * 0.74f), w * 0.04f)
                drawLine(Color.White.copy(alpha = 0.7f), Offset(w * 0.5f, w * 0.52f), Offset(w * 0.5f, w * 0.34f), w * 0.04f)
            }
            Picto.SMILE -> {
                drawCircle(color, w * 0.42f, Offset(w * 0.5f, w * 0.5f))
                drawCircle(Color.White, w * 0.055f, Offset(w * 0.37f, w * 0.42f))
                drawCircle(Color.White, w * 0.055f, Offset(w * 0.63f, w * 0.42f))
                drawArc(Color.White, 25f, 130f, false, Offset(w * 0.3f, w * 0.3f), Size(w * 0.4f, w * 0.4f), style = Stroke(w * 0.07f, cap = StrokeCap.Round))
            }
            Picto.SPARKLE -> drawPath(
                Path().apply {
                    moveTo(w * 0.5f, w * 0.06f); lineTo(w * 0.6f, w * 0.4f); lineTo(w * 0.94f, w * 0.5f); lineTo(w * 0.6f, w * 0.6f)
                    lineTo(w * 0.5f, w * 0.94f); lineTo(w * 0.4f, w * 0.6f); lineTo(w * 0.06f, w * 0.5f); lineTo(w * 0.4f, w * 0.4f); close()
                },
                color,
            )
            Picto.GIFT -> {
                drawRect(color, Offset(w * 0.16f, w * 0.44f), Size(w * 0.68f, w * 0.42f))
                drawRect(color, Offset(w * 0.1f, w * 0.3f), Size(w * 0.8f, w * 0.18f))
                drawRect(Color.White.copy(alpha = 0.8f), Offset(w * 0.46f, w * 0.3f), Size(w * 0.08f, w * 0.56f))
                drawCircle(Color.White.copy(alpha = 0.8f), w * 0.1f, Offset(w * 0.4f, w * 0.22f), style = Stroke(w * 0.05f))
                drawCircle(Color.White.copy(alpha = 0.8f), w * 0.1f, Offset(w * 0.6f, w * 0.22f), style = Stroke(w * 0.05f))
            }
        }
    }
}

private fun DrawScope.arc(color: Color, center: Float, radius: Float, stroke: Stroke) {
    drawArc(
        color, -45f, 90f, false,
        Offset(center - radius, center - radius), Size(radius * 2, radius * 2),
        style = stroke,
    )
}
