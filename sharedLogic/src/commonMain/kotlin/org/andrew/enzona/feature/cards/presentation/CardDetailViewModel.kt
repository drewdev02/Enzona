package org.andrew.enzona.feature.cards.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.andrew.enzona.core.logging.logger
import org.andrew.enzona.core.result.EzResult
import org.andrew.enzona.core.result.userMessage
import org.andrew.enzona.feature.cards.domain.CardActivity
import org.andrew.enzona.feature.cards.domain.CardsRepository

sealed interface CardDetailUiState {
    data object Loading : CardDetailUiState
    data class Content(val activity: CardActivity) : CardDetailUiState
    data class Error(val message: String) : CardDetailUiState
}

/**
 * Detalle de tarjeta: saldo y últimas operaciones.
 *
 * Vive lo que vive su pantalla: `viewModelScope` cancela las cargas en vuelo al destruirse.
 * Depende directamente del repositorio porque el id de tarjeta ya viene dado por la navegación.
 */
class CardDetailViewModel(
    private val cardId: String,
    private val cards: CardsRepository,
) : ViewModel() {
    private val log = logger<CardDetailViewModel>()
    private val _state = MutableStateFlow<CardDetailUiState>(CardDetailUiState.Loading)
    val state: StateFlow<CardDetailUiState> = _state.asStateFlow()

    /** Carga (o recarga). Mantiene el contenido previo visible hasta tener el nuevo. */
    fun load() {
        if (_state.value !is CardDetailUiState.Content) _state.value = CardDetailUiState.Loading
        viewModelScope.launch {
            _state.value = when (val r = cards.getActivity(cardId)) {
                is EzResult.Success -> {
                    log.info(
                        "Actividad cargada: {Operaciones} operaciones, {Omitidas} omitidas",
                        r.value.operations.size,
                        r.value.skippedOperations,
                    )
                    CardDetailUiState.Content(r.value)
                }
                is EzResult.Error -> {
                    log.warn(
                        "No se pudo cargar la actividad de la tarjeta: {Fallo}",
                        r.failure::class.simpleName,
                    )
                    CardDetailUiState.Error(r.failure.userMessage())
                }
            }
        }
    }
}
