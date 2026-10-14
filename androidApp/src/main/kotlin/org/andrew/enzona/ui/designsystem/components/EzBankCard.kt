package org.andrew.enzona.ui.designsystem.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.andrew.enzona.ui.designsystem.EnzonaTheme

/**
 * Geometría de la tarjeta, medida sobre el original. Si cambias estos valores, revisa también
 * `tools/check_card_skins.py` (las zonas de texto y el recorte salen de aquí).
 */
object EzBankCardDefaults {
    /** Alto / ancho: el original usa 0.55 y recorta la imagen (BoxFit.cover). */
    const val HeightRatio = 0.55f
    /** Margen izquierdo del texto, en fracción del ancho. */
    const val StartInset = 0.044f
    /** Radio de esquina, en fracción del ancho (el de los PNG: ~20px de 630). */
    const val CornerRadius = 0.032f
    /** Centro vertical de cada fila de texto, en fracción del alto. */
    const val NumberCenter = 0.51f
    const val NameCenter = 0.776f
    const val InfoCenter = 0.88f
    /** Centro vertical y horizontal de los iconos de arriba a la derecha. */
    const val IconCenterY = 0.175f
    const val MenuCenterX = 0.94f
    const val CheckCenterX = 0.855f
    /** Opacidad del velo negro tras el icono cuando el fondo es una foto. Igual a SCRIM en tools/check_card_skins.py. */
    const val MenuScrimAlpha = 0.35f
}

/**
 * Tarjeta bancaria dibujada sobre la imagen de su banco. Recibe solo datos de presentación: quien
 * la usa traduce el dominio (qué imagen y qué tintas). Los tamaños de letra salen del ANCHO de la
 * tarjeta y no de la fuente del usuario: el texto está colocado sobre arte con zonas fijas. Lo que
 * leen los lectores de pantalla es [description].
 */
@Composable
fun EzBankCard(
    last4: String,
    holder: String,
    currency: String,
    expiry: String?,
    isPrimary: Boolean,
    numberInk: Color,
    textInk: Color,
    menuInk: Color,
    menuScrim: Boolean,
    description: String,
    menuDescription: String,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier,
    background: Painter? = null,
    fallbackTitle: String? = null,
    /** Dónde empieza el número, en fracción del ancho (algunos skins traen un adorno a la izquierda). */
    numberInset: Float = EzBankCardDefaults.StartInset,
    /** Toque sobre la tarjeta (el menú tiene el suyo). `null` la deja sin acción, como en las previews. */
    onClick: (() -> Unit)? = null,
    onClickLabel: String? = null,
) {
    val d = EzBankCardDefaults
    BoxWithConstraints(modifier.fillMaxWidth().aspectRatio(1f / d.HeightRatio)) {
        val w = maxWidth
        val h = maxHeight
        val shape = RoundedCornerShape(w * d.CornerRadius)
        val density = LocalDensity.current
        fun size(fraction: Float) = with(density) { (w * fraction).toSp() }

        Box(
            Modifier.fillMaxSize().shadow(EnzonaTheme.elevation.medium, shape).clip(shape)
                .then(
                    if (onClick != null) Modifier.clickable(onClickLabel = onClickLabel, role = Role.Button, onClick = onClick)
                    else Modifier
                ),
        ) {
            if (background != null) {
                Image(background, contentDescription = null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } else {
                Box(Modifier.fillMaxSize().background(
                    Brush.linearGradient(listOf(EnzonaTheme.colors.brandBlue, EnzonaTheme.colors.brandNavy))
                ))
            }

            // Todo el texto es UN elemento para los lectores de pantalla (el menú queda aparte).
            Box(Modifier.fillMaxSize().clearAndSetSemantics { contentDescription = description }) {
                val startInset = w * d.StartInset

                fallbackTitle?.let {
                    Text(
                        it, color = textInk, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        style = TextStyle(fontSize = size(0.055f), fontWeight = FontWeight.Bold),
                        modifier = Modifier.offset(startInset, h * d.IconCenterY - 14.dp).fillMaxWidth(0.65f),
                    )
                }

                TextRow(centerY = h * d.NumberCenter, rowHeight = h * 0.14f, startInset = w * numberInset) {
                    Text("**** **** **** ", color = numberInk,
                        style = TextStyle(fontSize = size(0.07f), fontWeight = FontWeight.Normal))
                    Text(last4, color = numberInk,
                        style = TextStyle(fontSize = size(0.085f), fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum"))
                }
                if (holder.isNotBlank()) {
                    TextRow(centerY = h * d.NameCenter, rowHeight = h * 0.1f, startInset = startInset) {
                        Text(holder, color = textInk, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            style = TextStyle(fontSize = size(0.047f), fontWeight = FontWeight.Medium),
                            modifier = Modifier.fillMaxWidth(0.68f))
                    }
                }
                TextRow(centerY = h * d.InfoCenter, rowHeight = h * 0.1f, startInset = startInset) {
                    if (currency.isNotBlank()) {
                        Text(currency, color = textInk, style = TextStyle(fontSize = size(0.042f), fontWeight = FontWeight.Bold))
                    }
                    expiry?.let {
                        Text("Vence: $it", color = textInk, style = TextStyle(fontSize = size(0.042f)),
                            modifier = Modifier.offset(x = if (currency.isBlank()) 0.dp else w * 0.04f))
                    }
                }
                if (isPrimary) {
                    Icon(
                        Icons.Filled.Check, contentDescription = null, tint = menuInk,
                        modifier = Modifier.offset(w * d.CheckCenterX - 12.dp, h * d.IconCenterY - 12.dp).size(24.dp),
                    )
                }
            }

            IconButton(
                onClick = onMenuClick,
                modifier = Modifier.offset(w * d.MenuCenterX - 24.dp, h * d.IconCenterY - 24.dp).size(EnzonaTheme.sizes.minTouchTarget),
            ) {
                Box(
                    Modifier.size(32.dp).then(
                        if (menuScrim) Modifier.background(Color.Black.copy(alpha = d.MenuScrimAlpha), CircleShape) else Modifier
                    ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.MoreVert, contentDescription = menuDescription, tint = menuInk, modifier = Modifier.size(24.dp))
                }
            }
        }
    }
}

@Composable
private fun TextRow(centerY: Dp, rowHeight: Dp, startInset: Dp, content: @Composable () -> Unit) {
    Row(
        Modifier.offset(x = startInset, y = centerY - rowHeight / 2).height(rowHeight),
        verticalAlignment = Alignment.CenterVertically,
    ) { content() }
}
