package org.andrew.enzona.feature.cards.data

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.isSuccess
import org.andrew.enzona.core.network.apiCall
import org.andrew.enzona.core.result.EzResult
import org.andrew.enzona.core.result.Failure
import org.andrew.enzona.feature.cards.domain.BankCard
import org.andrew.enzona.feature.cards.domain.CardsRepository

/** GET /coremz/1.0.2/card/username/{username} (verificado en vivo, HTTP 200). */
class CardsRepositoryImpl(private val client: HttpClient) : CardsRepository {

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
        if (items.isNotEmpty() && cards.isEmpty()) {
            return@apiCall EzResult.Error(Failure.Parse("Ninguna tarjeta de la respuesta es legible."))
        }
        EzResult.Success(cards.sortedForDisplay())
    }
}
