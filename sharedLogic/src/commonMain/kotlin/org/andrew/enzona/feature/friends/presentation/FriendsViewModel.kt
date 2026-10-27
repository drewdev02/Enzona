package org.andrew.enzona.feature.friends.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.andrew.enzona.core.logging.logger
import org.andrew.enzona.core.result.EzResult
import org.andrew.enzona.core.result.userMessage
import org.andrew.enzona.feature.account.domain.GetAccountUseCase
import org.andrew.enzona.feature.friends.domain.Friend
import org.andrew.enzona.feature.friends.domain.FriendsRepository

sealed interface FriendsUiState {
    data object Loading : FriendsUiState
    data class Content(val friends: List<Friend>) : FriendsUiState

    /** El usuario no tiene amigos añadidos. */
    data object Empty : FriendsUiState
    data class Error(val message: String) : FriendsUiState
}

/** "Mis amigos": la lista para elegir a quién transferir. Necesita el uuid de la cuenta del propio usuario. */
class FriendsViewModel(
    private val getAccount: GetAccountUseCase,
    private val friends: FriendsRepository,
) : ViewModel() {
    private val log = logger<FriendsViewModel>()
    private val _state = MutableStateFlow<FriendsUiState>(FriendsUiState.Loading)
    val state: StateFlow<FriendsUiState> = _state.asStateFlow()

    /** Carga (o recarga). Mantiene el contenido previo visible hasta tener el nuevo. */
    fun load() {
        if (_state.value !is FriendsUiState.Content) _state.value = FriendsUiState.Loading
        viewModelScope.launch {
            _state.value = when (val account = getAccount()) {
                is EzResult.Error -> {
                    log.warn("No se pudo obtener la cuenta para listar amigos: {Fallo}", account.failure::class.simpleName)
                    FriendsUiState.Error(account.failure.userMessage())
                }
                is EzResult.Success -> when (val r = friends.list(account.value.uuid)) {
                    is EzResult.Success -> {
                        log.info("Amigos cargados: {Cantidad}", r.value.size)
                        if (r.value.isEmpty()) FriendsUiState.Empty else FriendsUiState.Content(r.value)
                    }
                    is EzResult.Error -> {
                        log.warn("No se pudieron cargar los amigos: {Fallo}", r.failure::class.simpleName)
                        FriendsUiState.Error(r.failure.userMessage())
                    }
                }
            }
        }
    }
}
