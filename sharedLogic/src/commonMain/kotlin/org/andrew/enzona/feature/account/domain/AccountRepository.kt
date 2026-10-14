package org.andrew.enzona.feature.account.domain

import org.andrew.enzona.core.result.EzResult

interface AccountRepository {
    suspend fun getAccount(username: String): EzResult<Account>
}
