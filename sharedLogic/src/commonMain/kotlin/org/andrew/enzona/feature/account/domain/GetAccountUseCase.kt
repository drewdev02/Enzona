package org.andrew.enzona.feature.account.domain

import org.andrew.enzona.core.result.EzResult
import org.andrew.enzona.core.result.Failure
import org.andrew.enzona.feature.auth.domain.SessionRepository

/** Obtiene la cuenta del usuario de la sesión actual (username = claim `sub` del id_token). */
class GetAccountUseCase(
    private val accounts: AccountRepository,
    private val session: SessionRepository,
) {
    suspend operator fun invoke(): EzResult<Account> {
        val username = session.username()
            ?: return EzResult.Error(Failure.Auth("No se pudo identificar al usuario de la sesión."))
        return accounts.getAccount(username)
    }
}
