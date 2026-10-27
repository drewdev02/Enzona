package org.andrew.enzona.feature.transfer.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.andrew.enzona.core.logging.logger
import org.andrew.enzona.core.result.userMessage
import org.andrew.enzona.feature.transfer.domain.FindRecipientResult
import org.andrew.enzona.feature.transfer.domain.FindRecipientUseCase
import org.andrew.enzona.feature.transfer.domain.InvalidQuery
import org.andrew.enzona.feature.transfer.domain.TransferRecipient

sealed interface FindRecipientUiState {
    data object Idle : FindRecipientUiState
    data object Searching : FindRecipientUiState
    data class Found(val recipient: TransferRecipient) : FindRecipientUiState
    data object NotFound : FindRecipientUiState
    data class Invalid(val reason: InvalidQuery) : FindRecipientUiState
    data class Error(val message: String) : FindRecipientUiState
}

/**
 * "Buscar una cuenta". Solo `Found` permite seguir a transferir, y el destinatario que lleva es el objeto que
 * devolvió el servidor. Una búsqueda nueva cancela la anterior: una respuesta tardía no puede sustituir a la
 * actual (mostrar una cuenta y enviar a otra).
 */
class FindRecipientViewModel(
    private val find: FindRecipientUseCase,
) : ViewModel() {
    private val log = logger<FindRecipientViewModel>()
    private val _state = MutableStateFlow<FindRecipientUiState>(FindRecipientUiState.Idle)
    val state: StateFlow<FindRecipientUiState> = _state.asStateFlow()

    private var job: Job? = null

    fun search(query: String) {
        job?.cancel()
        _state.value = FindRecipientUiState.Searching
        job = viewModelScope.launch {
            // El nombre buscado es un dato personal: no se registra, solo el tipo de resultado.
            _state.value = when (val r = find(query)) {
                is FindRecipientResult.Found -> FindRecipientUiState.Found(r.recipient)
                is FindRecipientResult.NotFound -> FindRecipientUiState.NotFound
                is FindRecipientResult.Invalid -> FindRecipientUiState.Invalid(r.reason)
                is FindRecipientResult.Failed -> FindRecipientUiState.Error(r.failure.userMessage())
            }
            log.info("Búsqueda de cuenta: {Resultado}", _state.value::class.simpleName)
        }
    }

    /** Vuelve al estado inicial (p. ej. al editar el texto de búsqueda). */
    fun reset() {
        job?.cancel()
        _state.value = FindRecipientUiState.Idle
    }
}
