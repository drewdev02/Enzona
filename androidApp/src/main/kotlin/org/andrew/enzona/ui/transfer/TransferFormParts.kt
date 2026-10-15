package org.andrew.enzona.ui.transfer

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.andrew.enzona.R
import org.andrew.enzona.feature.cards.domain.BankBrand
import org.andrew.enzona.feature.cards.domain.BankCard
import org.andrew.enzona.feature.cards.domain.bankBrandOf
import org.andrew.enzona.feature.transfer.domain.NotSentReason
import org.andrew.enzona.feature.transfer.domain.TransferResultKind
import org.andrew.enzona.ui.cards.detailLabel
import org.andrew.enzona.ui.designsystem.EnzonaTheme
import org.andrew.enzona.ui.designsystem.components.EzButton
import org.andrew.enzona.ui.designsystem.components.EzButtonStyle
import org.andrew.enzona.ui.designsystem.components.EzDivider
import org.andrew.enzona.ui.designsystem.components.EzListItem
import org.andrew.enzona.ui.designsystem.components.EzTextField
import org.andrew.enzona.ui.designsystem.isLargeFont

// Piezas que comparten los formularios que mueven dinero ("A una de mis tarjetas", "A una cuenta", "A un amigo"): el
// selector de tarjeta, el diálogo de contraseña, la pantalla de "enviando" y el comprobante. Una sola versión de cada una.

/** Título de la pantalla cuando ya hay un desenlace: el del comprobante de la app original. */
const val RECEIPT_TITLE = "Detalles de la transferencia"

/** Qué puede pedir el comprobante: corregir (si no se movió dinero), mirar el historial (si no se sabe) o cerrar. */
class ReceiptActions(
    val onEditAgain: () -> Unit = {},
    val onOpenHistory: () -> Unit = {},
    val onClose: () -> Unit = {},
)

/** Logo del banco de la tarjeta, o el avatar neutro si no hay tarjeta o no se reconoce el banco. */
internal fun BankCard?.logo(): Int = this?.let { bankBrandOf(it.bankCode).logoRes } ?: R.drawable.avatar_placeholder

internal fun BankBrand.logoOrPlaceholder(): Int = logoRes ?: R.drawable.avatar_placeholder

/** Fila con borde que abre el selector de tarjeta. Sin tarjeta elegida muestra [placeholder]. */
@Composable
internal fun CardPickerRow(
    label: String,
    card: BankCard?,
    placeholder: String,
    error: String?,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val spacing = EnzonaTheme.spacing
    val colors = EnzonaTheme.colors
    val value = card?.detailLabel() ?: placeholder
    Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
        Row(
            Modifier.fillMaxWidth()
                .border(if (error != null) 2.dp else 1.dp, if (error != null) colors.error else colors.outline, EnzonaTheme.shapes.md)
                .then(if (enabled) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
                .padding(horizontal = spacing.lg, vertical = spacing.md)
                .semantics(mergeDescendants = true) { contentDescription = "$label: ${if (card != null) value else "sin elegir"}" },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            Image(painterResource(card.logo()), contentDescription = null, Modifier.size(EnzonaTheme.sizes.avatarMd))
            Column(Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
                Text(
                    value, style = MaterialTheme.typography.bodyLarge,
                    color = if (card != null) colors.onSurface else colors.onSurfaceVariant,
                )
            }
        }
        error?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = colors.error) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CardPickerSheet(title: String, cards: List<BankCard>, onPick: (BankCard) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Text(
            title, style = MaterialTheme.typography.titleMedium, color = EnzonaTheme.colors.onSurface,
            modifier = Modifier.padding(horizontal = EnzonaTheme.spacing.lg, vertical = EnzonaTheme.spacing.sm),
        )
        Column(Modifier.verticalScroll(rememberScrollState())) {
            cards.forEach { card ->
                EzListItem(
                    title = card.detailLabel(),
                    subtitle = card.currency.trim().uppercase(),
                    leading = { Image(painterResource(card.logo()), contentDescription = null, Modifier.size(EnzonaTheme.sizes.avatarMd)) },
                    onClick = { onPick(card) },
                )
                EzDivider()
            }
        }
    }
}

/**
 * Diálogo de confirmación con la contraseña de pago. La contraseña vive solo aquí, en memoria: no se guarda en el
 * estado ni sobrevive a girar la pantalla. [notSent] explica por qué no salió el último intento.
 */
@Composable
internal fun PasswordConfirmDialog(
    question: String,
    notSent: NotSentReason?,
    onSubmit: (String) -> Unit,
    onCancel: () -> Unit,
) {
    var password by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Confirmar transferencia") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(EnzonaTheme.spacing.md)) {
                Text(question)
                EzTextField(
                    value = password, onValueChange = { password = it }, label = "Contraseña de pago", isPassword = true,
                    keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done,
                    error = notSent?.message(),
                )
            }
        },
        confirmButton = { TextButton(onClick = { onSubmit(password) }) { Text("Confirmar") } },
        dismissButton = { TextButton(onClick = onCancel) { Text("Cancelar") } },
    )
}

@Composable
internal fun SubmittingContent() {
    Column(
        Modifier.fillMaxSize().padding(EnzonaTheme.spacing.xl)
            .semantics(mergeDescendants = true) { contentDescription = "Enviando la transferencia. No cierres la pantalla." },
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator(color = EnzonaTheme.colors.primary)
        Text(
            "Enviando la transferencia…", style = MaterialTheme.typography.titleMedium, color = EnzonaTheme.colors.onSurface,
            modifier = Modifier.padding(top = EnzonaTheme.spacing.lg),
        )
        Text(
            "No cierres la pantalla.", style = MaterialTheme.typography.bodyMedium, color = EnzonaTheme.colors.onSurfaceVariant,
            modifier = Modifier.padding(top = EnzonaTheme.spacing.xs),
        )
    }
}

/**
 * "Detalles de la transferencia", como el comprobante de la app original: logo, destino, estado, importe, fecha, filas,
 * tarjeta de origen y QR. Tras un resultado que no es "confirmada" se añaden los botones que hacen falta (corregir si
 * no se movió dinero; mirar el historial si no se sabe).
 */
@Composable
internal fun ReceiptContent(detail: TransferDetail, actions: ReceiptActions) {
    val spacing = EnzonaTheme.spacing
    val colors = EnzonaTheme.colors
    val statusColor = when (detail.kind) {
        TransferResultKind.SUCCESS -> colors.success
        TransferResultKind.FAILED, TransferResultKind.NOT_SENT -> colors.error
        TransferResultKind.PENDING, TransferResultKind.UNKNOWN -> colors.onWarningContainer
    }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = spacing.lg, vertical = spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing.lg),
    ) {
        Image(painterResource(detail.destinationBank.logoOrPlaceholder()), contentDescription = null, Modifier.size(112.dp))
        Text(
            detail.mainText, style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = colors.onSurface, textAlign = TextAlign.Center,
        )
        Text(detail.statusTitle, style = MaterialTheme.typography.titleLarge, color = statusColor, textAlign = TextAlign.Center)
        Text(detail.amountText, style = MaterialTheme.typography.titleLarge, color = colors.onSurface, textAlign = TextAlign.Center)
        detail.dateText?.let {
            Text(it, style = MaterialTheme.typography.titleMedium, color = colors.onSurfaceVariant, textAlign = TextAlign.Center)
        }
        detail.note?.let {
            Text(it, style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant, textAlign = TextAlign.Center)
        }
        Column(Modifier.fillMaxWidth()) {
            detail.rows.forEach { (label, value) ->
                DetailRow(label, value)
                EzDivider()
            }
        }
        SourceCardRow(detail)
        detail.qrContent?.let { QrBlock(it) }
        when (detail.kind) {
            TransferResultKind.SUCCESS -> Unit
            // El dinero no se movió: se puede corregir y volver a intentar.
            TransferResultKind.FAILED -> {
                EzButton("Corregir", onClick = actions.onEditAgain)
                EzButton("Cerrar", onClick = actions.onClose, style = EzButtonStyle.Tertiary)
            }
            // Nunca se muestra aquí (un intento que no salió vuelve a confirmar), pero el `when` es exhaustivo.
            TransferResultKind.NOT_SENT -> EzButton("Cerrar", onClick = actions.onClose, style = EzButtonStyle.Tertiary)
            // Pendiente o desconocido: puede que sí se movió, así que nada de reintentar; primero el historial.
            TransferResultKind.PENDING, TransferResultKind.UNKNOWN -> {
                EzButton("Ver historial", onClick = actions.onOpenHistory)
                EzButton("Cerrar", onClick = actions.onClose, style = EzButtonStyle.Tertiary)
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    val spacing = EnzonaTheme.spacing
    val modifier = Modifier.fillMaxWidth().padding(vertical = spacing.md)
        .semantics(mergeDescendants = true) { contentDescription = "$label: $value" }
    val labelText: @Composable () -> Unit = {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = EnzonaTheme.colors.onSurfaceVariant)
    }
    if (isLargeFont()) {
        Column(modifier) {
            labelText()
            Text(value, style = MaterialTheme.typography.bodyLarge, color = EnzonaTheme.colors.onSurface)
        }
    } else {
        Row(modifier, horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            labelText()
            Text(
                value, style = MaterialTheme.typography.bodyLarge, color = EnzonaTheme.colors.onSurface,
                textAlign = TextAlign.End, modifier = Modifier.padding(start = spacing.md),
            )
        }
    }
}

@Composable
private fun SourceCardRow(detail: TransferDetail) {
    val spacing = EnzonaTheme.spacing
    val colors = EnzonaTheme.colors
    val spoken = "Tarjeta de origen: ${detail.sourceHolder}, ${detail.sourceSuffix}"
    Surface(
        Modifier.fillMaxWidth().semantics(mergeDescendants = true) { contentDescription = spoken },
        shape = EnzonaTheme.shapes.sm, color = colors.surface, shadowElevation = EnzonaTheme.elevation.low,
    ) {
        val logo: @Composable () -> Unit = {
            Image(painterResource(detail.sourceBank.logoOrPlaceholder()), contentDescription = null, Modifier.size(EnzonaTheme.sizes.avatarMd))
        }
        val holder: @Composable (Modifier) -> Unit = { m ->
            Text(detail.sourceHolder.uppercase(), style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant, modifier = m)
        }
        val suffix: @Composable () -> Unit = {
            Text(detail.sourceSuffix, style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant)
        }
        if (isLargeFont()) {
            Column(Modifier.padding(spacing.md), verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                logo(); holder(Modifier); suffix()
            }
        } else {
            Row(
                Modifier.padding(spacing.md), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.md),
            ) {
                logo(); holder(Modifier.weight(1f)); suffix()
            }
        }
    }
}

@Composable
private fun QrBlock(content: String) {
    val sizeDp = 200.dp
    val sizePx = with(LocalDensity.current) { sizeDp.roundToPx() }
    val image = remember(content, sizePx) { qrImage(content, sizePx) } ?: return
    Image(
        image, contentDescription = "Código QR del comprobante", modifier = Modifier.size(sizeDp),
        filterQuality = FilterQuality.None,
    )
}
