package org.andrew.enzona.feature.transfer.domain

import org.andrew.enzona.core.result.EzResult

interface TransferRepository {
    /** Transferencias del usuario, de más reciente a más antigua (el orden lo da el servidor). */
    suspend fun list(offset: Int, limit: Int, filter: TransferFilter = TransferFilter.None): EzResult<TransferPage>
}
