package org.andrew.enzona.feature.cards.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.andrew.enzona.core.result.EzResult
import org.andrew.enzona.core.result.userMessage
import org.andrew.enzona.core.util.Cancellable
import org.andrew.enzona.core.util.watch
import org.andrew.enzona.feature.cards.domain.BankCard
import org.andrew.enzona.feature.cards.domain.GetCardsUseCase

sealed interface CardsUiState {
    data object Loading : CardsUiState
    data class Content(val cards: List<BankCard>) : CardsUiState
    data object Empty : CardsUiState
    data class Error(val message: String) : CardsUiState
}

/** Vive lo que vive su pantalla: `viewModelScope` cancela las cargas en vuelo al destruirse. */
class CardsViewModel(
    private val getCards: GetCardsUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow<CardsUiState>(CardsUiState.Loading)
    val state: StateFlow<CardsUiState> = _state.asStateFlow()

    /** Carga (o recarga). Mantiene el contenido previo visible hasta tener el nuevo. */
    fun load() {
        if (_state.value !is CardsUiState.Content) _state.value = CardsUiState.Loading
        viewModelScope.launch {
            _state.value = when (val r = getCards()) {
                is EzResult.Success ->
                    if (r.value.isEmpty()) CardsUiState.Empty else CardsUiState.Content(r.value)
                is EzResult.Error -> CardsUiState.Error(r.failure.userMessage())
            }
        }
    }

    /** Observa el estado desde Swift (puente manual en lugar de SKIE). */
    fun watchState(onEach: (CardsUiState) -> Unit): Cancellable = state.watch(onEach)
}
