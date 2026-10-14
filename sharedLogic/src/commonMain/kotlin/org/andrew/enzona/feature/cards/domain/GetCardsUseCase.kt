package org.andrew.enzona.feature.cards.domain

import org.andrew.enzona.core.result.EzResult
import org.andrew.enzona.core.result.Failure
import org.andrew.enzona.feature.auth.domain.SessionRepository

/** Tarjetas del usuario de la sesión actual (username = claim `sub` del id_token). */
class GetCardsUseCase(
    private val cards: CardsRepository,
    private val session: SessionRepository,
) {
    suspend operator fun invoke(): EzResult<List<BankCard>> {
        val username = session.username()
            ?: return EzResult.Error(Failure.Auth("No se pudo identificar al usuario de la sesión."))
        return cards.getCards(username)
    }
}
