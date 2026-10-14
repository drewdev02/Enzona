package org.andrew.enzona.feature.account.presentation

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.andrew.enzona.core.result.EzResult
import org.andrew.enzona.core.result.userMessage
import org.andrew.enzona.core.util.Cancellable
import org.andrew.enzona.core.util.watch
import org.andrew.enzona.feature.account.domain.Account
import org.andrew.enzona.feature.account.domain.GetAccountUseCase

sealed interface AccountUiState {
    data object Loading : AccountUiState
    data class Content(val account: Account) : AccountUiState
    data class Error(val message: String) : AccountUiState
}

class AccountViewModel(
    private val getAccount: GetAccountUseCase,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main),
) {
    private val _state = MutableStateFlow<AccountUiState>(AccountUiState.Loading)
    val state: StateFlow<AccountUiState> = _state.asStateFlow()

    /** Carga (o recarga) la cuenta. Mantiene el contenido previo visible hasta tener el nuevo. */
    fun load() {
        if (_state.value !is AccountUiState.Content) _state.value = AccountUiState.Loading
        scope.launch {
            _state.value = when (val r = getAccount()) {
                is EzResult.Success -> AccountUiState.Content(r.value)
                is EzResult.Error -> AccountUiState.Error(r.failure.userMessage())
            }
        }
    }

    /** Observa el estado desde Swift (puente manual en lugar de SKIE). */
    fun watchState(onEach: (AccountUiState) -> Unit): Cancellable = state.watch(onEach)
}
