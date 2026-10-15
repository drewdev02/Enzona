package org.andrew.enzona.feature.cards.data

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.isSuccess
import org.andrew.enzona.core.logging.logger
import org.andrew.enzona.core.network.apiCall
import org.andrew.enzona.core.result.EzResult
import org.andrew.enzona.core.result.Failure
import org.andrew.enzona.feature.cards.domain.BankCard
import org.andrew.enzona.feature.cards.domain.CardActivity
import org.andrew.enzona.feature.cards.domain.CardsRepository

/** GET /coremz/1.0.2/card/username/{username} (verificado en vivo, HTTP 200). */
class CardsRepositoryImpl(private val client: HttpClient) : CardsRepository {
    private val log = logger<CardsRepositoryImpl>()

    override suspend fun getCards(username: String): EzResult<List<BankCard>> = apiCall {
        val response = client.get("/coremz/1.0.2/card/username/$username")
        if (!response.status.isSuccess()) {
            // No se incluye el cuerpo en el error: puede contener datos personales.
            return@apiCall EzResult.Error(Failure.Http(response.status.value))
        }
        val items = response.body<CardsEnvelopeDto>().card
        // Una tarjeta ilegible no tumba la pantalla entera: se omite. Pero si TODAS lo son, la
        // respuesta no es lo que esperamos y se informa como error en vez de mostrar "sin tarjetas".
        val cards = items.mapNotNull { it.toDomain() }
        // Solo cuántas; nunca qué tarjeta ni su contenido.
        if (cards.size < items.size) log.warn("Tarjetas ilegibles omitidas: {Omitidas} de {Total}", items.size - cards.size, items.size)
        if (items.isNotEmpty() && cards.isEmpty()) {
            return@apiCall EzResult.Error(Failure.Parse("Ninguna tarjeta de la respuesta es legible."))
        }
        EzResult.Success(cards.sortedForDisplay())
    }

    override suspend fun getActivity(cardId: String): EzResult<CardActivity> = apiCall {
        val response = client.get("/coremz/1.0.2/api/balance/lastOperations/$cardId")
        if (!response.status.isSuccess()) {
            // No se incluye el cuerpo en el error para evitar exponer datos de la respuesta.
            return@apiCall EzResult.Error(Failure.Http(response.status.value))
        }
        val dto = response.body<CardActivityDto>()
        if (dto.fault.hasFault()) {
            return@apiCall EzResult.Error(Failure.Unknown("El servicio respondió con un error."))
        }
        val activity = dto.toDomain()
        if (activity.skippedOperations > 0) {
            // Solo el número de operaciones omitidas, sin datos de tarjeta ni importes.
            log.warn("Operaciones omitidas: {Omitidas}", activity.skippedOperations)
        }
        EzResult.Success(activity)
    }
}
