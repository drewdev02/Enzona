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
import org.andrew.enzona.feature.transfer.domain.TransferRequest
import org.andrew.enzona.feature.transfer.domain.parseAmountInput

/** Lo que está mal en el formulario tras intentar revisarlo. Cada campo se limpia al editarlo. */
data class OwnCardFormProblems(
    val amount: AmountProblem? = null,
    val destinationMissing: Boolean = false,
    /** La app original pide la descripción en este formulario ("Campo requerido"). */
    val descriptionMissing: Boolean = false,
) {
    val any: Boolean get() = amount != null || destinationMissing || descriptionMissing
}

sealed interface OwnCardTransferUiState {
    data object Loading : OwnCardTransferUiState
    data class LoadError(val message: String) : OwnCardTransferUiState

    /** Para mover dinero entre tarjetas propias hacen falta al menos dos. */
    data object NeedsTwoCards : OwnCardTransferUiState

    data class Editing(
        val cards: List<BankCard>,
        val sourceId: String,
        val destinationId: String?,
        val amountText: String,
        val description: String,
        val problems: OwnCardFormProblems = OwnCardFormProblems(),
    ) : OwnCardTransferUiState

    /**
     * Resumen para confirmar. [exchangeApplies]: las monedas difieren y el servidor aplicará su tasa de cambio.
     * [notSent]: el último intento no salió (contraseña vacía o inutilizable, u otra transferencia en curso).
     */
    data class Confirming(
        val form: Editing,
        val request: TransferRequest.ToOwnCard,
        val source: BankCard,
        val destination: BankCard,
        val exchangeApplies: Boolean,
        val notSent: NotSentReason? = null,
    ) : OwnCardTransferUiState

    /** La petición está en vuelo: no se admite otra acción hasta que termine. */
    data class Submitting(
        val request: TransferRequest.ToOwnCard,
        val source: BankCard,
        val destination: BankCard,
    ) : OwnCardTransferUiState

    /** Terminó. Solo [TransferOutcome.Rejected] deja volver a editar; un resultado desconocido pide mirar el historial. */
    data class Finished(
        val outcome: TransferOutcome,
        val request: TransferRequest.ToOwnCard,
        val source: BankCard,
        val destination: BankCard,
        val form: Editing,
    ) : OwnCardTransferUiState
}

/**
 * "A una de mis tarjetas": elegir origen y destino, importe y descripción; revisar; confirmar con la contraseña de
 * pago; ver el resultado. La contraseña no se guarda en el estado: llega a [submit] y solo vive hasta cifrarse.
 *
 * Garantías contra un doble pago: [submit] solo actúa desde `Confirming` y pasa a `Submitting` en el mismo instante,
 * y el caso de uso no admite dos transferencias a la vez. Tras un resultado desconocido no hay "reintentar".
 */
class OwnCardTransferViewModel(
    private val getCards: GetCardsUseCase,
    private val submitTransfer: SubmitTransferUseCase,
) : ViewModel() {
    private val log = logger<OwnCardTransferViewModel>()
    private val _state = MutableStateFlow<OwnCardTransferUiState>(OwnCardTransferUiState.Loading)
    val state: StateFlow<OwnCardTransferUiState> = _state.asStateFlow()

    /** Carga las tarjetas. Solo actúa al inicio o tras un error: no pisa lo que el usuario ya escribió ni un envío. */
    fun load() {
        val current = _state.value
        if (current !is OwnCardTransferUiState.Loading && current !is OwnCardTransferUiState.LoadError) return
        _state.value = OwnCardTransferUiState.Loading
        viewModelScope.launch {
            _state.value = when (val r = getCards()) {
                is EzResult.Success -> {
                    log.info("Tarjetas para transferir entre propias: {Cantidad}", r.value.size)
                    freshForm(r.value)
                }
                is EzResult.Error -> {
                    log.warn("No se pudieron cargar las tarjetas: {Fallo}", r.failure::class.simpleName)
                    OwnCardTransferUiState.LoadError(r.failure.userMessage())
                }
            }
        }
    }

    private fun freshForm(cards: List<BankCard>): OwnCardTransferUiState {
        if (cards.size < 2) return OwnCardTransferUiState.NeedsTwoCards
        val source = cards.firstOrNull { it.isPrimary } ?: cards.first()
        return OwnCardTransferUiState.Editing(cards, source.id, destinationId = null, amountText = "", description = "")
    }

    private inline fun editing(change: (OwnCardTransferUiState.Editing) -> OwnCardTransferUiState.Editing) {
        val current = _state.value as? OwnCardTransferUiState.Editing ?: return
        _state.value = change(current)
    }

    fun selectSource(cardId: String) = editing { form ->
        if (form.cards.none { it.id == cardId }) return@editing form
        form.copy(
            sourceId = cardId,
            // Origen y destino no pueden ser la misma tarjeta.
            destinationId = form.destinationId?.takeIf { it != cardId },
            problems = form.problems.copy(destinationMissing = false),
        )
    }

    fun selectDestination(cardId: String) = editing { form ->
        if (cardId == form.sourceId || form.cards.none { it.id == cardId }) return@editing form
        form.copy(destinationId = cardId, problems = form.problems.copy(destinationMissing = false))
    }

    fun onAmountChange(text: String) = editing { it.copy(amountText = text, problems = it.problems.copy(amount = null)) }

    fun onDescriptionChange(text: String) =
        editing { it.copy(description = text, problems = it.problems.copy(descriptionMissing = false)) }

    /** Valida el formulario: si está bien pasa a confirmar; si no, marca qué falla y se queda editando. */
    fun review() {
        val form = _state.value as? OwnCardTransferUiState.Editing ?: return
        val source = form.cards.firstOrNull { it.id == form.sourceId }
        val destination = form.cards.firstOrNull { it.id == form.destinationId }
        val amount = parseAmountInput(form.amountText)
        val problems = OwnCardFormProblems(
            amount = (amount as? AmountInput.Problem)?.problem,
            destinationMissing = destination == null,
            descriptionMissing = form.description.isBlank(),
        )
        if (problems.any || source == null || destination == null || amount !is AmountInput.Valid) {
            _state.value = form.copy(problems = problems)
            return
        }
        _state.value = OwnCardTransferUiState.Confirming(
            form = form.copy(problems = OwnCardFormProblems()),
            request = TransferRequest.ToOwnCard(
                sourceCardId = source.id,
                destinationCardId = destination.id,
                // La moneda es la de la tarjeta de origen, como la app original; el servidor convierte si difieren.
                amount = Money(amount.minorUnits, source.currency),
                description = form.description.trim(),
            ),
            source = source,
            destination = destination,
            exchangeApplies = !sameCurrency(source.currency, destination.currency),
        )
    }

    /** De confirmar vuelve a editar, con todo lo escrito. */
    fun back() {
        val confirming = _state.value as? OwnCardTransferUiState.Confirming ?: return
        _state.value = confirming.form
    }

    /**
     * Envía la transferencia. Solo desde `Confirming`; el cambio a `Submitting` es inmediato, así que un segundo
     * toque llega con el estado ya cambiado y no hace nada.
     */
    fun submit(password: String) {
        val confirming = _state.value as? OwnCardTransferUiState.Confirming ?: return
        val submitting = OwnCardTransferUiState.Submitting(confirming.request, confirming.source, confirming.destination)
        _state.value = submitting
        viewModelScope.launch {
            val outcome = submitTransfer(confirming.request, PaymentPassword(password))
            _state.value = when (outcome) {
                // Nada salió: se vuelve a confirmar para corregir (contraseña) o esperar (otra transferencia en curso).
                is TransferOutcome.NotSent -> confirming.copy(notSent = outcome.reason)
                else -> OwnCardTransferUiState.Finished(
                    outcome, confirming.request, confirming.source, confirming.destination, confirming.form,
                )
            }
        }
    }

    /** Tras un rechazo del servidor (el dinero no se movió) se puede corregir y volver a intentar. */
    fun editAgain() {
        val finished = _state.value as? OwnCardTransferUiState.Finished ?: return
        if (finished.outcome !is TransferOutcome.Rejected) return
        _state.value = finished.form
    }

    /** Formulario nuevo y vacío (tras una transferencia aceptada o de resultado desconocido). */
    fun startOver() {
        val finished = _state.value as? OwnCardTransferUiState.Finished ?: return
        _state.value = freshForm(finished.form.cards)
    }

    /** Observa el estado desde Swift (puente manual en lugar de SKIE). */
    fun watchState(onEach: (OwnCardTransferUiState) -> Unit): Cancellable = state.watch(onEach)
}
