package org.andrew.enzona.ui.transfer

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.andrew.enzona.R
import org.andrew.enzona.feature.cards.domain.BankBrand
import org.andrew.enzona.feature.cards.domain.BankCard
import org.andrew.enzona.feature.cards.domain.bankBrandOf
import org.andrew.enzona.feature.transfer.domain.TransferResultKind
import org.andrew.enzona.feature.transfer.presentation.OwnCardTransferUiState
import org.andrew.enzona.feature.transfer.presentation.OwnCardTransferViewModel
import org.andrew.enzona.ui.cards.detailLabel
import org.andrew.enzona.ui.designsystem.EnzonaTheme
import org.andrew.enzona.ui.designsystem.components.EzButton
import org.andrew.enzona.ui.designsystem.components.EzButtonStyle
import org.andrew.enzona.ui.designsystem.components.EzDivider
import org.andrew.enzona.ui.designsystem.components.EzEmptyState
import org.andrew.enzona.ui.designsystem.components.EzErrorState
import org.andrew.enzona.ui.designsystem.components.EzGradientTopAppBar
import org.andrew.enzona.ui.designsystem.components.EzListItem
import org.andrew.enzona.ui.designsystem.components.EzLoadingState
import org.andrew.enzona.ui.designsystem.components.EzTextField
import org.andrew.enzona.ui.designsystem.isLargeFont
import org.koin.compose.viewmodel.koinViewModel

/** Todo lo que la pantalla puede pedir al ViewModel, para pasarlo junto y poder previsualizar sin él. */
class OwnCardTransferActions(
    val onRetryLoad: () -> Unit = {},
    val onSelectSource: (String) -> Unit = {},
    val onSelectDestination: (String) -> Unit = {},
    val onAmountChange: (String) -> Unit = {},
    val onDescriptionChange: (String) -> Unit = {},
    val onReview: () -> Unit = {},
    val onBackToForm: () -> Unit = {},
    val onSubmit: (String) -> Unit = {},
    val onEditAgain: () -> Unit = {},
    val onOpenHistory: () -> Unit = {},
    val onClose: () -> Unit = {},
)

@Composable
fun OwnCardTransferRoute(
    onBack: () -> Unit,
    onOpenHistory: () -> Unit,
    vm: OwnCardTransferViewModel = koinViewModel(),
) {
    LaunchedEffect(Unit) { vm.load() }
    val state by vm.state.collectAsStateWithLifecycle()
    OwnCardTransferScreen(
        state,
        OwnCardTransferActions(
            onRetryLoad = vm::load, onSelectSource = vm::selectSource, onSelectDestination = vm::selectDestination,
            onAmountChange = vm::onAmountChange, onDescriptionChange = vm::onDescriptionChange, onReview = vm::review,
            onBackToForm = vm::back, onSubmit = vm::submit, onEditAgain = vm::editAgain,
            onOpenHistory = onOpenHistory, onClose = onBack,
        ),
    )
}

/** "A una de mis tarjetas". Sin ViewModel: se puede previsualizar. */
@Composable
fun OwnCardTransferScreen(state: OwnCardTransferUiState, actions: OwnCardTransferActions) {
    // Con una petición en vuelo no se sale: el resultado se mostraría a nadie y se podría repetir un pago por duda.
    val sending = state is OwnCardTransferUiState.Submitting
    BackHandler(enabled = sending) {}
    val title = if (state is OwnCardTransferUiState.Finished) "Detalles de la transferencia" else "Transferir a una de mis tarjetas"
    Scaffold(
        topBar = { EzGradientTopAppBar(title, onBack = { if (!sending) actions.onClose() }) },
        containerColor = EnzonaTheme.colors.background,
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (state) {
                is OwnCardTransferUiState.Loading -> EzLoadingState()
                is OwnCardTransferUiState.LoadError -> EzErrorState(state.message, actions.onRetryLoad)
                is OwnCardTransferUiState.NeedsTwoCards -> EzEmptyState(
                    title = "Necesitas al menos dos tarjetas",
                    message = "Para transferir entre tus tarjetas, primero agrega otra.",
                )
                is OwnCardTransferUiState.Editing -> FormContent(state, actions, enabled = true)
                is OwnCardTransferUiState.Confirming -> {
                    FormContent(state.form, actions, enabled = false)
                    ConfirmDialog(state, actions)
                }
                is OwnCardTransferUiState.Submitting -> SubmittingContent()
                is OwnCardTransferUiState.Finished -> DetailContent(state.toDetail(), actions)
            }
        }
    }
}

@Composable
private fun FormContent(form: OwnCardTransferUiState.Editing, actions: OwnCardTransferActions, enabled: Boolean) {
    val spacing = EnzonaTheme.spacing
    val source = form.cards.first { it.id == form.sourceId }
    val destination = form.cards.firstOrNull { it.id == form.destinationId }
    var picking by remember { mutableStateOf<Picker?>(null) }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(spacing.lg),
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        CardPickerRow("Tarjeta origen", source, "", null, enabled) { picking = Picker.Source }
        CardPickerRow(
            "Tarjeta destino", destination, "Seleccione la tarjeta destino",
            error = "Seleccione la tarjeta destino".takeIf { form.problems.destinationMissing }, enabled = enabled,
        ) { picking = Picker.Destination }
        EzTextField(
            value = form.amountText, onValueChange = actions.onAmountChange, label = "Importe", enabled = enabled,
            error = form.problems.amount?.message(), keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next,
        )
        EzTextField(
            value = form.description, onValueChange = actions.onDescriptionChange, label = "Descripción", enabled = enabled,
            error = "Campo requerido".takeIf { form.problems.descriptionMissing }, imeAction = ImeAction.Done,
        )
        EzButton("ACEPTAR", onClick = actions.onReview, enabled = enabled)
    }
    picking?.let { which ->
        val candidates = when (which) {
            Picker.Source -> form.cards
            Picker.Destination -> form.cards.filter { it.id != form.sourceId }
        }
        CardPickerSheet(
            title = if (which == Picker.Source) "Tarjeta origen" else "Tarjeta destino",
            cards = candidates,
            onPick = { card ->
                if (which == Picker.Source) actions.onSelectSource(card.id) else actions.onSelectDestination(card.id)
                picking = null
            },
            onDismiss = { picking = null },
        )
    }
}

private enum class Picker { Source, Destination }

@Composable
private fun CardPickerRow(
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

/** Logo del banco de la tarjeta, o el avatar neutro si no hay tarjeta o no se reconoce el banco. */
private fun BankCard?.logo(): Int = this?.let { bankBrandOf(it.bankCode).logoRes } ?: R.drawable.avatar_placeholder

private fun BankBrand.logoOrPlaceholder(): Int = logoRes ?: R.drawable.avatar_placeholder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CardPickerSheet(title: String, cards: List<BankCard>, onPick: (BankCard) -> Unit, onDismiss: () -> Unit) {
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

@Composable
private fun ConfirmDialog(state: OwnCardTransferUiState.Confirming, actions: OwnCardTransferActions) {
    // La contraseña vive solo aquí, en memoria: no se guarda en el estado ni sobrevive a girar la pantalla.
    var password by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = actions.onBackToForm,
        title = { Text("Confirmar transferencia") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(EnzonaTheme.spacing.md)) {
                Text(confirmationText(state.request.amount, state.source, state.destination, state.exchangeApplies))
                EzTextField(
                    value = password, onValueChange = { password = it }, label = "Contraseña de pago", isPassword = true,
                    keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done,
                    error = state.notSent?.message(),
                )
            }
        },
        confirmButton = { TextButton(onClick = { actions.onSubmit(password) }) { Text("Confirmar") } },
        dismissButton = { TextButton(onClick = actions.onBackToForm) { Text("Cancelar") } },
    )
}

@Composable
private fun SubmittingContent() {
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
 * "Detalles de la transferencia", como el comprobante de la app original: logo del banco destino, destino, estado,
 * importe, fecha, filas, tarjeta de origen y QR. Tras un resultado que no es "confirmada" se añaden los botones que
 * hacen falta (corregir si no se movió dinero; mirar el historial si no se sabe).
 */
@Composable
private fun DetailContent(detail: TransferDetail, actions: OwnCardTransferActions) {
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

// ---- Previews (datos inventados; no tocan Koin ni la red) ----

@Preview(name = "Formulario", showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun OwnCardFormPreview() = EnzonaTheme(darkTheme = false) {
    OwnCardTransferScreen(OwnCardTransferPreviewData.editing, OwnCardTransferActions())
}

@Preview(name = "Formulario con errores", showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun OwnCardFormErrorsPreview() = EnzonaTheme(darkTheme = false) {
    OwnCardTransferScreen(OwnCardTransferPreviewData.withProblems, OwnCardTransferActions())
}

@Preview(name = "Confirmar", showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun OwnCardConfirmPreview() = EnzonaTheme(darkTheme = false) {
    OwnCardTransferScreen(OwnCardTransferPreviewData.confirming, OwnCardTransferActions())
}

@Preview(name = "Detalle: confirmada", showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun OwnCardDetailAcceptedPreview() = EnzonaTheme(darkTheme = false) {
    OwnCardTransferScreen(OwnCardTransferPreviewData.accepted, OwnCardTransferActions())
}

@Preview(name = "Detalle: fallida", showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun OwnCardDetailRejectedPreview() = EnzonaTheme(darkTheme = false) {
    OwnCardTransferScreen(OwnCardTransferPreviewData.rejected, OwnCardTransferActions())
}

@Preview(name = "Detalle: desconocida", showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun OwnCardDetailUnknownPreview() = EnzonaTheme(darkTheme = false) {
    OwnCardTransferScreen(OwnCardTransferPreviewData.unknown, OwnCardTransferActions())
}

@Preview(name = "Detalle (fuente grande)", showBackground = true, widthDp = 360, heightDp = 900, fontScale = 1.6f)
@Composable
private fun OwnCardDetailLargeFontPreview() = EnzonaTheme(darkTheme = false) {
    OwnCardTransferScreen(OwnCardTransferPreviewData.accepted, OwnCardTransferActions())
}
