package org.andrew.enzona.ui.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.andrew.enzona.ui.designsystem.components.EzAmount
import org.andrew.enzona.ui.designsystem.components.EzAmountTone
import org.andrew.enzona.ui.designsystem.components.EzAvatar
import org.andrew.enzona.ui.designsystem.components.EzButton
import org.andrew.enzona.ui.designsystem.components.EzButtonStyle
import org.andrew.enzona.ui.designsystem.components.EzCard
import org.andrew.enzona.ui.designsystem.components.EzDivider
import org.andrew.enzona.ui.designsystem.components.EzListItem
import org.andrew.enzona.ui.designsystem.components.EzSectionHeader
import org.andrew.enzona.ui.designsystem.components.EzStatusPill
import org.andrew.enzona.ui.designsystem.components.EzTextField
import org.andrew.enzona.ui.designsystem.components.EzTone

/**
 * Galería del sistema de diseño: cada token y cada componente en una pantalla. Se abre desde
 * los @Preview de abajo (Android Studio). Si añades un componente, añádelo aquí.
 */
@Composable
fun DesignSystemGallery() {
    val s = EnzonaTheme.spacing
    Column(
        Modifier.background(EnzonaTheme.colors.background).verticalScroll(rememberScrollState())
            .padding(vertical = s.lg),
        verticalArrangement = Arrangement.spacedBy(s.lg),
    ) {
        Section("Colores") { ColorSwatches() }
        Section("Tipografía") { TypeScale() }
        Section("Botones") {
            EzButton("Primario", {})
            EzButton("Secundario", {}, style = EzButtonStyle.Secondary)
            EzButton("Terciario", {}, style = EzButtonStyle.Tertiary)
            EzButton("Destructivo", {}, style = EzButtonStyle.Destructive)
            EzButton("Cargando", {}, loading = true)
            EzButton("Deshabilitado", {}, enabled = false)
        }
        Section("Campos de texto") {
            var a by remember { mutableStateOf("") }
            var b by remember { mutableStateOf("12345") }
            var c by remember { mutableStateOf("secreto") }
            EzTextField(a, { a = it }, label = "Usuario", helper = "Tu nombre de usuario de EnZona")
            EzTextField(b, { b = it }, label = "Monto", error = "Monto inválido", keyboardType = KeyboardType.Number)
            EzTextField(c, { c = it }, label = "Contraseña de pago", isPassword = true)
        }
        Section("Estados y avatares") {
            Row(horizontalArrangement = Arrangement.spacedBy(s.sm)) {
                EzStatusPill("Aceptada", EzTone.Success)
                EzStatusPill("Pendiente", EzTone.Warning)
                EzStatusPill("Rechazada", EzTone.Error)
                EzStatusPill("Info", EzTone.Info)
                EzStatusPill("Neutral", EzTone.Neutral)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(s.md)) {
                EzAvatar("TU", size = EnzonaTheme.sizes.avatarSm)
                EzAvatar("TU", size = EnzonaTheme.sizes.avatarMd)
                EzAvatar("TU", size = EnzonaTheme.sizes.avatarLg)
            }
        }
        Section("Importes") {
            EzAmount("1,400.00", currency = "CUP", large = true)
            EzAmount("350.00", currency = "CUP", tone = EzAmountTone.Positive)
            EzAmount("-90.50", currency = "CUP", tone = EzAmountTone.Negative)
        }
        Section("Tarjeta y lista") {
            EzCard {
                Text("Tarjeta", style = MaterialTheme.typography.titleMedium)
                Text("Contenedor agrupado con esquinas lg.", style = MaterialTheme.typography.bodyMedium,
                    color = EnzonaTheme.colors.onSurfaceVariant)
            }
        }
        EzSectionHeader("Movimientos", actionText = "Ver todo", onAction = {})
        EzListItem(
            title = "Transferencia a user2", subtitle = "Aceptada · 03 oct",
            leading = { EzAvatar("U2") },
            trailing = { EzAmount("-1,400.00", currency = "CUP", tone = EzAmountTone.Negative) },
            onClick = {},
        )
        EzDivider()
        EzListItem(
            title = "Pago ETECSA", subtitle = "Aceptada · 27 sep",
            leading = { EzAvatar("ET") },
            trailing = { EzAmount("-250.00", currency = "CUP") },
            onClick = {},
        )
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    val s = EnzonaTheme.spacing
    Column(Modifier.padding(horizontal = s.lg), verticalArrangement = Arrangement.spacedBy(s.sm)) {
        Text(title, style = MaterialTheme.typography.titleSmall, color = EnzonaTheme.colors.onSurfaceVariant)
        content()
    }
}

@Composable
private fun ColorSwatches() {
    val c = EnzonaTheme.colors
    val items = listOf(
        "brandBlue" to c.brandBlue, "brandNavy" to c.brandNavy, "primary" to c.primary,
        "primaryContainer" to c.primaryContainer, "surface" to c.surface, "surfaceVariant" to c.surfaceVariant,
        "outline" to c.outline, "divider" to c.divider, "success" to c.success, "warning" to c.warning,
        "error" to c.error, "info" to c.info,
    )
    Column(verticalArrangement = Arrangement.spacedBy(EnzonaTheme.spacing.xs)) {
        items.chunked(4).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(EnzonaTheme.spacing.xs)) {
                row.forEach { (name, color) ->
                    Column(Modifier.weight(1f)) {
                        Box(Modifier.fillMaxWidth().height(36.dp).background(color, EnzonaTheme.shapes.sm))
                        Text(name, style = MaterialTheme.typography.labelSmall, color = EnzonaTheme.colors.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun TypeScale() {
    val t = MaterialTheme.typography
    val onSurface = EnzonaTheme.colors.onSurface
    Column {
        Text("displayMedium 36", style = t.displayMedium, color = onSurface)
        Text("headlineLarge 28", style = t.headlineLarge, color = onSurface)
        Text("headlineSmall 20", style = t.headlineSmall, color = onSurface)
        Text("titleMedium 16", style = t.titleMedium, color = onSurface)
        Text("bodyLarge 16 — texto de lectura", style = t.bodyLarge, color = onSurface)
        Text("bodyMedium 14 — texto secundario", style = t.bodyMedium, color = onSurface)
        Text("labelSmall 11 — etiqueta", style = t.labelSmall, color = onSurface)
    }
}

@Preview(name = "Galería (claro)", showBackground = true, widthDp = 380, heightDp = 2300)
@Composable
private fun GalleryLightPreview() = EnzonaTheme(darkTheme = false) { DesignSystemGallery() }

@Preview(name = "Galería (oscuro)", showBackground = true, widthDp = 380, heightDp = 2300)
@Composable
private fun GalleryDarkPreview() = EnzonaTheme(darkTheme = true) { DesignSystemGallery() }
