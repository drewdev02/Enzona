package org.andrew.enzona.feature.account.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.andrew.enzona.core.logging.logger
import org.andrew.enzona.core.result.EzResult
import org.andrew.enzona.core.result.userMessage
import org.andrew.enzona.feature.account.domain.Account
import org.andrew.enzona.feature.account.domain.GetAccountUseCase

sealed interface AccountUiState {
    data object Loading : AccountUiState
    data class Content(val account: Account) : AccountUiState
    data class Error(val message: String) : AccountUiState
}

/** Vive lo que vive su pantalla: `viewModelScope` cancela las cargas en vuelo al destruirse. */
class AccountViewModel(
    private val getAccount: GetAccountUseCase,
) : ViewModel() {
    private val log = logger<AccountViewModel>()
    private val _state = MutableStateFlow<AccountUiState>(AccountUiState.Loading)
    val state: StateFlow<AccountUiState> = _state.asStateFlow()

    /** Carga (o recarga) la cuenta. Mantiene el contenido previo visible hasta tener el nuevo. */
    fun load() {
        if (_state.value !is AccountUiState.Content) _state.value = AccountUiState.Loading
        viewModelScope.launch {
            _state.value = when (val r = getAccount()) {
                is EzResult.Success -> {
                    log.info("Cuenta cargada")
                    AccountUiState.Content(r.value)
                }
                is EzResult.Error -> {
                    log.warn("No se pudo cargar la cuenta: {Fallo}", r.failure::class.simpleName)
                    AccountUiState.Error(r.failure.userMessage())
                }
            }
        }
    }
}
