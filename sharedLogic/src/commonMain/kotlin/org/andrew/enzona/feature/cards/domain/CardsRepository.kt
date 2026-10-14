package org.andrew.enzona.feature.cards.domain

import org.andrew.enzona.core.result.EzResult

interface CardsRepository {
    suspend fun getCards(username: String): EzResult<List<BankCard>>
}
