package org.andrew.enzona.feature.transfer.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.andrew.enzona.core.logging.logger
import org.andrew.enzona.core.result.EzResult
import org.andrew.enzona.core.result.userMessage
import org.andrew.enzona.core.util.Cancellable
import org.andrew.enzona.core.util.watch
import org.andrew.enzona.feature.transfer.domain.TransferRepository
import org.andrew.enzona.feature.transfer.domain.TransferSummary

sealed interface TransferMenuUiState {
    data object Loading : TransferMenuUiState

    /** [recent] vacío = el usuario aún no ha hecho transferencias. [skipped]: filas que no se pudieron leer. */
    data class Content(val recent: List<TransferSummary>, val skipped: Int) : TransferMenuUiState
    data class Error(val message: String) : TransferMenuUiState
}

/** Menú "Transferir": las transferencias recientes. Vive lo que vive su pantalla. */
class TransferMenuViewModel(
    private val transfers: TransferRepository,
) : ViewModel() {
    private val log = logger<TransferMenuViewModel>()
    private val _state = MutableStateFlow<TransferMenuUiState>(TransferMenuUiState.Loading)
    val state: StateFlow<TransferMenuUiState> = _state.asStateFlow()

    /** Carga (o recarga). Mantiene el contenido previo visible hasta tener el nuevo. */
    fun load() {
        if (_state.value !is TransferMenuUiState.Content) _state.value = TransferMenuUiState.Loading
        viewModelScope.launch {
            _state.value = when (val r = transfers.list(offset = 0, limit = RECENT_COUNT)) {
                is EzResult.Success -> {
                    log.info("Transferencias recientes cargadas: {Cantidad}", r.value.items.size)
                    TransferMenuUiState.Content(r.value.items, r.value.skipped)
                }
                is EzResult.Error -> {
                    log.warn("No se pudieron cargar las transferencias recientes: {Fallo}", r.failure::class.simpleName)
                    TransferMenuUiState.Error(r.failure.userMessage())
                }
            }
        }
    }

    /** Observa el estado desde Swift (puente manual en lugar de SKIE). */
    fun watchState(onEach: (TransferMenuUiState) -> Unit): Cancellable = state.watch(onEach)

    companion object {
        /** Los que muestra la app original en "Recientes". */
        const val RECENT_COUNT = 10
    }
}
