package org.andrew.enzona.feature.transfer.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.andrew.enzona.core.logging.logger
import org.andrew.enzona.core.result.EzResult
import org.andrew.enzona.core.result.userMessage
import org.andrew.enzona.core.util.Cancellable
import org.andrew.enzona.core.util.watch
import org.andrew.enzona.feature.transfer.domain.TransferFilter
import org.andrew.enzona.feature.transfer.domain.TransferRepository
import org.andrew.enzona.feature.transfer.domain.TransferSummary

sealed interface TransferHistoryUiState {
    data object Loading : TransferHistoryUiState
    data class Error(val message: String, val filter: TransferFilter) : TransferHistoryUiState

    /**
     * [hasMore]: puede haber más páginas (llegó una llena). [isLoadingMore]: se está pidiendo la siguiente.
     * [moreError]: la siguiente página falló; lo ya cargado se conserva y se puede reintentar con `loadMore()`.
     */
    data class Content(
        val items: List<TransferSummary>,
        val skipped: Int,
        val filter: TransferFilter,
        val hasMore: Boolean,
        val isLoadingMore: Boolean = false,
        val moreError: String? = null,
    ) : TransferHistoryUiState
}

/**
 * Historial de transferencias con filtros (fechas y estado) y paginación por scroll. Como la app original:
 * páginas de 10 y `offset += 10`. Un cambio de filtro cancela lo que hubiera en vuelo para que una respuesta
 * tardía del filtro anterior no pise la lista nueva.
 */
class TransferHistoryViewModel(
    private val transfers: TransferRepository,
) : ViewModel() {
    private val log = logger<TransferHistoryViewModel>()
    private val _state = MutableStateFlow<TransferHistoryUiState>(TransferHistoryUiState.Loading)
    val state: StateFlow<TransferHistoryUiState> = _state.asStateFlow()

    private var filter = TransferFilter.None
    private var nextOffset = 0
    private var job: Job? = null

    /** Primera carga (o reintento tras un error). Con contenido ya cargado vuelve a pedir desde el principio. */
    fun load() = reload(filter)

    /** Aplica otro filtro y vuelve a la primera página. */
    fun applyFilter(newFilter: TransferFilter) = reload(newFilter)

    private fun reload(newFilter: TransferFilter) {
        job?.cancel()
        filter = newFilter
        nextOffset = 0
        _state.value = TransferHistoryUiState.Loading
        job = viewModelScope.launch {
            _state.value = when (val r = transfers.list(offset = 0, limit = PAGE_SIZE, filter = newFilter)) {
                is EzResult.Success -> {
                    nextOffset = PAGE_SIZE
                    log.info("Historial cargado: {Cantidad} transferencias", r.value.items.size)
                    TransferHistoryUiState.Content(
                        items = r.value.items.distinctBy { it.id },
                        skipped = r.value.skipped,
                        filter = newFilter,
                        hasMore = r.value.mayHaveMore,
                    )
                }
                is EzResult.Error -> {
                    log.warn("No se pudo cargar el historial: {Fallo}", r.failure::class.simpleName)
                    TransferHistoryUiState.Error(r.failure.userMessage(), newFilter)
                }
            }
        }
    }

    /** Pide la página siguiente. Se ignora si no hay más, o si ya se está pidiendo (un doble toque no duplica). */
    fun loadMore() {
        val shown = _state.value as? TransferHistoryUiState.Content ?: return
        if (!shown.hasMore || shown.isLoadingMore) return
        // Se parte de un estado SIN el error de un intento anterior: si no, un reintento correcto lo dejaría visible.
        val current = shown.copy(moreError = null)
        _state.value = current.copy(isLoadingMore = true)
        job = viewModelScope.launch {
            when (val r = transfers.list(offset = nextOffset, limit = PAGE_SIZE, filter = filter)) {
                is EzResult.Success -> {
                    nextOffset += PAGE_SIZE
                    val known = current.items.mapTo(HashSet()) { it.id }
                    // El servidor puede desplazar filas entre páginas: no se repite una que ya está.
                    val fresh = r.value.items.filter { known.add(it.id) }
                    log.info("Historial: página siguiente con {Cantidad} transferencias", fresh.size)
                    _state.value = current.copy(
                        items = current.items + fresh,
                        skipped = current.skipped + r.value.skipped,
                        hasMore = r.value.mayHaveMore,
                        isLoadingMore = false,
                    )
                }
                is EzResult.Error -> {
                    log.warn("No se pudo cargar la página siguiente: {Fallo}", r.failure::class.simpleName)
                    _state.value = current.copy(isLoadingMore = false, moreError = r.failure.userMessage())
                }
            }
        }
    }

    /** Observa el estado desde Swift (puente manual en lugar de SKIE). */
    fun watchState(onEach: (TransferHistoryUiState) -> Unit): Cancellable = state.watch(onEach)

    companion object {
        const val PAGE_SIZE = 10
    }
}
