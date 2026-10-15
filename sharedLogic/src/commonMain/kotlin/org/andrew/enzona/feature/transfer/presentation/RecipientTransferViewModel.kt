package org.andrew.enzona.feature.transfer.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.andrew.enzona.core.logging.logger
import org.andrew.enzona.core.money.Money
import org.andrew.enzona.core.money.sameCurrency
import org.andrew.enzona.core.result.EzResult
import org.andrew.enzona.core.result.userMessage
import org.andrew.enzona.core.util.Cancellable
import org.andrew.enzona.core.util.watch
import org.andrew.enzona.feature.cards.domain.BankCard
import org.andrew.enzona.feature.cards.domain.GetCardsUseCase
import org.andrew.enzona.feature.transfer.domain.AmountInput
import org.andrew.enzona.feature.transfer.domain.AmountProblem
import org.andrew.enzona.feature.transfer.domain.NotSentReason
import org.andrew.enzona.feature.transfer.domain.PaymentPassword
import org.andrew.enzona.feature.transfer.domain.SubmitTransferUseCase
import org.andrew.enzona.feature.transfer.domain.TransferOutcome
import org.andrew.enzona.feature.transfer.domain.TransferRecipient
import org.andrew.enzona.feature.transfer.domain.TransferRequest
import org.andrew.enzona.feature.transfer.domain.TransferResultKind
import org.andrew.enzona.feature.transfer.domain.kind
import org.andrew.enzona.feature.transfer.domain.parseAmountInput

sealed interface RecipientTransferUiState {
    data object Loading : RecipientTransferUiState
    data class LoadError(val message: String) : RecipientTransferUiState

    /** Hace falta al menos una tarjeta de origen. */
    data object NoCards : RecipientTransferUiState

    /** [amountProblem]: lo que falló en el último intento de revisar; se limpia al editar el importe. */
    data class Editing(
        val recipient: TransferRecipient,
        val cards: List<BankCard>,
        val sourceId: String,
        val amountText: String,
        val description: String,
        val amountProblem: AmountProblem? = null,
    ) : RecipientTransferUiState

    /**
     * Resumen para confirmar. [exchangeApplies]: se SABE que la moneda del destinatario difiere de la de la tarjeta y el
     * servidor aplicará su tasa; si no se conoce la moneda del destinatario, no se afirma nada (queda en `false`).
     */
    data class Confirming(
        val form: Editing,
        val request: TransferRequest.ToAccount,
        val source: BankCard,
        val exchangeApplies: Boolean,
        val notSent: NotSentReason? = null,
    ) : RecipientTransferUiState

    /** La petición está en vuelo: no se admite otra acción hasta que termine. */
    data class Submitting(
        val request: TransferRequest.ToAccount,
        val source: BankCard,
    ) : RecipientTransferUiState

    /** Terminó. Solo [TransferOutcome.Rejected] deja volver a editar; un resultado desconocido pide mirar el historial. */
    data class Finished(
        val outcome: TransferOutcome,
        val request: TransferRequest.ToAccount,
        val source: BankCard,
        val form: Editing,
    ) : RecipientTransferUiState {
        /** Cómo se clasifica el desenlace; lo decide el dominio y las dos apps solo ponen las palabras. */
        val kind: TransferResultKind get() = outcome.kind
    }
}

/**
 * "A una cuenta" y "A un amigo": el destinatario ya viene verificado ([TransferRecipient]: lo que devolvió el servidor
 * al buscar la cuenta, o el amigo elegido) y es el que se envía, nunca lo que se teclee. Aquí solo se elige la tarjeta
 * de origen, el importe y una descripción opcional. La contraseña no se guarda en el estado: llega a [submit].
 *
 * Mismas garantías que el formulario de tarjetas propias: [submit] solo actúa desde `Confirming` y pasa a `Submitting`
 * en el mismo instante; el caso de uso no admite dos transferencias a la vez; tras un resultado desconocido no hay
 * "reintentar".
 */
class RecipientTransferViewModel(
    private val recipient: TransferRecipient,
    private val getCards: GetCardsUseCase,
    private val submitTransfer: SubmitTransferUseCase,
) : ViewModel() {
    private val log = logger<RecipientTransferViewModel>()
    private val _state = MutableStateFlow<RecipientTransferUiState>(RecipientTransferUiState.Loading)
    val state: StateFlow<RecipientTransferUiState> = _state.asStateFlow()

    /** Carga las tarjetas. Solo actúa al inicio o tras un error: no pisa lo escrito ni un envío en curso. */
    fun load() {
        val current = _state.value
        if (current !is RecipientTransferUiState.Loading && current !is RecipientTransferUiState.LoadError) return
        _state.value = RecipientTransferUiState.Loading
        viewModelScope.launch {
            _state.value = when (val r = getCards()) {
                is EzResult.Success -> {
                    log.info("Tarjetas de origen para transferir: {Cantidad}", r.value.size)
                    freshForm(r.value)
                }
                is EzResult.Error -> {
                    log.warn("No se pudieron cargar las tarjetas: {Fallo}", r.failure::class.simpleName)
                    RecipientTransferUiState.LoadError(r.failure.userMessage())
                }
            }
        }
    }

    private fun freshForm(cards: List<BankCard>): RecipientTransferUiState {
        val source = cards.firstOrNull { it.isPrimary } ?: cards.firstOrNull() ?: return RecipientTransferUiState.NoCards
        return RecipientTransferUiState.Editing(recipient, cards, source.id, amountText = "", description = "")
    }

    private inline fun editing(change: (RecipientTransferUiState.Editing) -> RecipientTransferUiState.Editing) {
        val current = _state.value as? RecipientTransferUiState.Editing ?: return
        _state.value = change(current)
    }

    fun selectSource(cardId: String) = editing { form ->
        if (form.cards.none { it.id == cardId }) form else form.copy(sourceId = cardId)
    }

    fun onAmountChange(text: String) = editing { it.copy(amountText = text, amountProblem = null) }

    fun onDescriptionChange(text: String) = editing { it.copy(description = text) }

    /** Valida el importe: si está bien pasa a confirmar; si no, marca qué falla y se queda editando. */
    fun review() {
        val form = _state.value as? RecipientTransferUiState.Editing ?: return
        val source = form.cards.firstOrNull { it.id == form.sourceId }
        val amount = parseAmountInput(form.amountText)
        if (source == null || amount !is AmountInput.Valid) {
            _state.value = form.copy(amountProblem = (amount as? AmountInput.Problem)?.problem)
            return
        }
        val recipientCurrency = form.recipient.currency?.takeIf { it.isNotBlank() }
        _state.value = RecipientTransferUiState.Confirming(
            form = form,
            request = TransferRequest.ToAccount(
                sourceCardId = source.id,
                recipient = form.recipient,
                // La moneda es la de la tarjeta de origen, como la app original; el servidor convierte si difieren.
                amount = Money(amount.minorUnits, source.currency),
                // En este formulario la descripción es opcional ("Descripción (Opcional)" en el original).
                description = form.description.trim(),
            ),
            source = source,
            exchangeApplies = recipientCurrency != null && !sameCurrency(source.currency, recipientCurrency),
        )
    }

    /** De confirmar vuelve a editar, con todo lo escrito. */
    fun back() {
        val confirming = _state.value as? RecipientTransferUiState.Confirming ?: return
        _state.value = confirming.form
    }

    /**
     * Envía la transferencia. Solo desde `Confirming`; el cambio a `Submitting` es inmediato, así que un segundo toque
     * llega con el estado ya cambiado y no hace nada.
     */
    fun submit(password: String) {
        val confirming = _state.value as? RecipientTransferUiState.Confirming ?: return
        _state.value = RecipientTransferUiState.Submitting(confirming.request, confirming.source)
        viewModelScope.launch {
            val outcome = submitTransfer(confirming.request, PaymentPassword(password))
            _state.value = when (outcome) {
                // Nada salió: se vuelve a confirmar para corregir (contraseña) o esperar (otra transferencia en curso).
                is TransferOutcome.NotSent -> confirming.copy(notSent = outcome.reason)
                else -> RecipientTransferUiState.Finished(outcome, confirming.request, confirming.source, confirming.form)
            }
        }
    }

    /** Tras un rechazo del servidor (el dinero no se movió) se puede corregir y volver a intentar. */
    fun editAgain() {
        val finished = _state.value as? RecipientTransferUiState.Finished ?: return
        if (finished.outcome !is TransferOutcome.Rejected) return
        _state.value = finished.form
    }

    /** Observa el estado desde Swift (puente manual en lugar de SKIE). */
    fun watchState(onEach: (RecipientTransferUiState) -> Unit): Cancellable = state.watch(onEach)
}
