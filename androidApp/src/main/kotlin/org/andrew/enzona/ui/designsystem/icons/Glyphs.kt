package org.andrew.enzona.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.andrew.enzona.ui.theme.EnzonaColors

// Iconos dibujados con Canvas: los de material-icons-core no incluyen "escanear",
// "QR" ni "grupo", y así no se añade la librería extendida (pesada) por tres glifos.

/** Marco de visor con línea central (acción Escanear). */
@Composable
fun ScanGlyph(modifier: Modifier = Modifier, tint: Color = Color.White) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val stroke = w * 0.075f
        val half = stroke / 2
        val c = w * 0.30f
        fun line(x1: Float, y1: Float, x2: Float, y2: Float) =
            drawLine(tint, Offset(x1, y1), Offset(x2, y2), stroke, StrokeCap.Round)
        line(half, half, c, half); line(half, half, half, c)
        line(w - half, half, w - c, half); line(w - half, half, w - half, c)
        line(half, h - half, c, h - half); line(half, h - half, half, h - c)
        line(w - half, h - half, w - c, h - half); line(w - half, h - half, w - half, h - c)
        line(half, h / 2, w - half, h / 2)
    }
}

/** Tres módulos de QR y una esquina (acción Recibir). */
@Composable
fun ReceiveGlyph(modifier: Modifier = Modifier, tint: Color = Color.White) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val stroke = w * 0.075f
        val half = stroke / 2
        val sq = w * 0.40f
        val box = Size(sq - stroke, sq - stroke)
        val radius = CornerRadius(stroke * 0.6f)
        val style = Stroke(stroke)
        drawRoundRect(tint, Offset(half, half), box, radius, style)
        drawRoundRect(tint, Offset(half, h - sq + half), box, radius, style)
        drawRoundRect(tint, Offset(w - sq + half, h - sq + half), box, radius, style)
        drawLine(tint, Offset(w - sq, half), Offset(w - half, half), stroke, StrokeCap.Round)
        drawLine(tint, Offset(w - half, half), Offset(w - half, sq), stroke, StrokeCap.Round)
    }
}

/** Dos personas (pestaña Amigos). */
@Composable
fun GroupGlyph(modifier: Modifier = Modifier, tint: Color = Color.White) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val s = Stroke(w * 0.08f, cap = StrokeCap.Round)
        drawCircle(tint, w * 0.14f, Offset(w * 0.34f, h * 0.30f), style = s)
        drawArc(tint, 180f, 180f, false, Offset(w * 0.06f, h * 0.60f), Size(w * 0.56f, h * 0.46f), style = s)
        drawCircle(tint, w * 0.12f, Offset(w * 0.70f, h * 0.26f), style = s)
        drawArc(tint, 200f, 140f, false, Offset(w * 0.52f, h * 0.52f), Size(w * 0.44f, h * 0.42f), style = s)
    }
}

/**
 * Insignia circular de la marca. PLACEHOLDER neutro (tres barras): reemplazar por el
 * logotipo propio. Es el único sitio que dibuja la marca.
 */
@Composable
fun BrandBadge(modifier: Modifier = Modifier, diameter: Dp = 80.dp) {
    Box(
        modifier
            .size(diameter)
            .clip(CircleShape)
            .background(Color.White)
            .padding(4.dp)
            .clip(CircleShape)
            .background(EnzonaColors.Blue),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(diameter * 0.5f)) {
            val w = size.width
            val h = size.height
            val stroke = h * 0.13f
            fun bar(y: Float, end: Float) =
                drawLine(Color.White, Offset(stroke / 2, h * y), Offset(w * end, h * y), stroke, StrokeCap.Round)
            bar(0.25f, 0.90f); bar(0.50f, 1.00f); bar(0.75f, 0.70f)
        }
    }
}
