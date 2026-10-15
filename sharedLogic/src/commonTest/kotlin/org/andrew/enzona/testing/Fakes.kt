package org.andrew.enzona.testing

import org.andrew.enzona.core.result.EzResult
import org.andrew.enzona.feature.account.domain.Account
import org.andrew.enzona.feature.account.domain.AccountRepository

/** Repositorio de cuentas controlable: [answer] puede suspenderse (p. ej. esperar a un `CompletableDeferred`). */
class FakeAccounts(private val answer: suspend (String) -> EzResult<Account>) : AccountRepository {
    val asked = mutableListOf<String>()
    override suspend fun getAccount(username: String): EzResult<Account> {
        asked += username
        return answer(username)
    }
}
