package org.andrew.enzona.ui.transfer

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.andrew.enzona.feature.transfer.presentation.OwnCardTransferUiState
import org.andrew.enzona.feature.transfer.presentation.OwnCardTransferViewModel
import org.andrew.enzona.ui.designsystem.EnzonaTheme
import org.andrew.enzona.ui.designsystem.components.EzButton
import org.andrew.enzona.ui.designsystem.components.EzEmptyState
import org.andrew.enzona.ui.designsystem.components.EzErrorState
import org.andrew.enzona.ui.designsystem.components.EzGradientTopAppBar
import org.andrew.enzona.ui.designsystem.components.EzLoadingState
import org.andrew.enzona.ui.designsystem.components.EzTextField
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
) {
    val receipt: ReceiptActions get() = ReceiptActions(onEditAgain, onOpenHistory, onClose)
}

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
    val title = if (state is OwnCardTransferUiState.Finished) RECEIPT_TITLE else "Transferir a una de mis tarjetas"
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
                    PasswordConfirmDialog(
                        question = confirmationText(state.request.amount, state.source, state.destination, state.exchangeApplies),
                        notSent = state.notSent, onSubmit = actions.onSubmit, onCancel = actions.onBackToForm,
                    )
                }
                is OwnCardTransferUiState.Submitting -> SubmittingContent()
                is OwnCardTransferUiState.Finished -> ReceiptContent(state.toDetail(), actions.receipt)
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
