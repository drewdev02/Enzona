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
import org.andrew.enzona.feature.transfer.domain.TransferRecipient
import org.andrew.enzona.feature.transfer.presentation.RecipientTransferUiState
import org.andrew.enzona.feature.transfer.presentation.RecipientTransferViewModel
import org.andrew.enzona.ui.designsystem.EnzonaTheme
import org.andrew.enzona.ui.designsystem.components.EzAvatar
import org.andrew.enzona.ui.designsystem.components.EzButton
import org.andrew.enzona.ui.designsystem.components.EzEmptyState
import org.andrew.enzona.ui.designsystem.components.EzErrorState
import org.andrew.enzona.ui.designsystem.components.EzGradientTopAppBar
import org.andrew.enzona.ui.designsystem.components.EzListItem
import org.andrew.enzona.ui.designsystem.components.EzLoadingState
import org.andrew.enzona.ui.designsystem.components.EzTextField
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/** Todo lo que la pantalla puede pedir al ViewModel, para pasarlo junto y poder previsualizar sin él. */
class RecipientTransferActions(
    val onRetryLoad: () -> Unit = {},
    val onSelectSource: (String) -> Unit = {},
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

/** Conecta el ViewModel (uno por pantalla, con el destinatario ya verificado como parámetro) con la pantalla. */
@Composable
fun RecipientTransferRoute(
    recipient: TransferRecipient,
    kind: RecipientKind,
    onBack: () -> Unit,
    onOpenHistory: () -> Unit,
    vm: RecipientTransferViewModel = koinViewModel(parameters = { parametersOf(recipient) }),
) {
    LaunchedEffect(Unit) { vm.load() }
    val state by vm.state.collectAsStateWithLifecycle()
    RecipientTransferScreen(
        state, kind,
        RecipientTransferActions(
            onRetryLoad = vm::load, onSelectSource = vm::selectSource, onAmountChange = vm::onAmountChange,
            onDescriptionChange = vm::onDescriptionChange, onReview = vm::review, onBackToForm = vm::back,
            onSubmit = vm::submit, onEditAgain = vm::editAgain, onOpenHistory = onOpenHistory, onClose = onBack,
        ),
    )
}

/** "A una cuenta" / "A un amigo". Sin ViewModel: se puede previsualizar. */
@Composable
fun RecipientTransferScreen(state: RecipientTransferUiState, kind: RecipientKind, actions: RecipientTransferActions) {
    // Con una petición en vuelo no se sale: el resultado se mostraría a nadie y se podría repetir un pago por duda.
    val sending = state is RecipientTransferUiState.Submitting
    BackHandler(enabled = sending) {}
    val title = if (state is RecipientTransferUiState.Finished) RECEIPT_TITLE else kind.title()
    Scaffold(
        topBar = { EzGradientTopAppBar(title, onBack = { if (!sending) actions.onClose() }) },
        containerColor = EnzonaTheme.colors.background,
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (state) {
                is RecipientTransferUiState.Loading -> EzLoadingState()
                is RecipientTransferUiState.LoadError -> EzErrorState(state.message, actions.onRetryLoad)
                is RecipientTransferUiState.NoCards -> EzEmptyState(
                    title = "No tienes tarjetas",
                    message = "Para transferir necesitas una tarjeta de origen.",
                )
                is RecipientTransferUiState.Editing -> FormContent(state, actions, enabled = true)
                is RecipientTransferUiState.Confirming -> {
                    FormContent(state.form, actions, enabled = false)
                    PasswordConfirmDialog(
                        question = recipientConfirmationText(
                            state.request.amount, state.source, state.request.recipient.displayName,
                            state.exchangeApplies, state.request.recipient.currency,
                        ),
                        notSent = state.notSent, onSubmit = actions.onSubmit, onCancel = actions.onBackToForm,
                    )
                }
                is RecipientTransferUiState.Submitting -> SubmittingContent()
                is RecipientTransferUiState.Finished -> ReceiptContent(state.toDetail(), actions.receipt)
            }
        }
    }
}

@Composable
private fun FormContent(form: RecipientTransferUiState.Editing, actions: RecipientTransferActions, enabled: Boolean) {
    val spacing = EnzonaTheme.spacing
    val source = form.cards.first { it.id == form.sourceId }
    var picking by remember { mutableStateOf(false) }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(spacing.lg),
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        // A quién se envía: el destinatario verificado por el servidor, no editable.
        EzListItem(
            title = form.recipient.displayName,
            subtitle = form.recipient.username,
            leading = { EzAvatar(initialsOf(form.recipient.displayName)) },
        )
        CardPickerRow("Tarjeta origen", source, "", null, enabled) { picking = true }
        EzTextField(
            value = form.amountText, onValueChange = actions.onAmountChange, label = "Importe", enabled = enabled,
            error = form.amountProblem?.message(), keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next,
        )
        EzTextField(
            value = form.description, onValueChange = actions.onDescriptionChange, label = "Descripción (Opcional)",
            enabled = enabled, imeAction = ImeAction.Done,
        )
        EzButton("ACEPTAR", onClick = actions.onReview, enabled = enabled)
    }
    if (picking) {
        CardPickerSheet(
            title = "Tarjeta origen", cards = form.cards,
            onPick = { card -> actions.onSelectSource(card.id); picking = false },
            onDismiss = { picking = false },
        )
    }
}

// ---- Previews (datos inventados; no tocan Koin ni la red) ----

@Preview(name = "Cuenta: formulario", showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun AccountFormPreview() = EnzonaTheme(darkTheme = false) {
    RecipientTransferScreen(RecipientTransferPreviewData.editing, RecipientKind.ACCOUNT, RecipientTransferActions())
}

@Preview(name = "Amigo: confirmar con cambio de moneda", showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun FriendConfirmExchangePreview() = EnzonaTheme(darkTheme = false) {
    RecipientTransferScreen(RecipientTransferPreviewData.confirmingWithExchange, RecipientKind.FRIEND, RecipientTransferActions())
}

@Preview(name = "Cuenta: comprobante", showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun AccountReceiptPreview() = EnzonaTheme(darkTheme = false) {
    RecipientTransferScreen(RecipientTransferPreviewData.accepted, RecipientKind.ACCOUNT, RecipientTransferActions())
}

@Preview(name = "Cuenta: comprobante (fuente grande)", showBackground = true, widthDp = 360, heightDp = 900, fontScale = 1.6f)
@Composable
private fun AccountReceiptLargeFontPreview() = EnzonaTheme(darkTheme = false) {
    RecipientTransferScreen(RecipientTransferPreviewData.accepted, RecipientKind.ACCOUNT, RecipientTransferActions())
}
