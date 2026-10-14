package org.andrew.enzona.feature.account.domain

import org.andrew.enzona.core.result.EzResult
import org.andrew.enzona.core.result.Failure
import org.andrew.enzona.feature.auth.domain.AuthRepository

/** Obtiene la cuenta del usuario de la sesión actual (username = claim `sub` del id_token). */
class GetAccountUseCase(
    private val accounts: AccountRepository,
    private val auth: AuthRepository,
) {
    suspend operator fun invoke(): EzResult<Account> {
        val username = auth.username()
            ?: return EzResult.Error(Failure.Auth("No se pudo identificar al usuario de la sesión."))
        return accounts.getAccount(username)
    }
}
