package org.andrew.enzona.feature.cards

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import org.andrew.enzona.core.logging.LogLevel
import org.andrew.enzona.core.logging.withLogging
import org.andrew.enzona.core.network.enzonaJson
import org.andrew.enzona.core.result.EzResult
import org.andrew.enzona.feature.cards.data.CardsRepositoryImpl
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

private const val SECRET_CARD_ID = "card-uuid-secreto-xyz-98765"
private const val SECRET_BALANCE = "3395.15"
private const val SECRET_AVAILABLE = "17088.00"
private const val SECRET_AMOUNT = "950.00"
private const val SECRET_ALIAS = "Comercio Secreto S.A."
private const val SECRET_DESC = "TRF_SECRETA"
private const val SECRET_DESC_OMITTED = "DESC_INVALIDA_OMITIDA"

class CardActivityRedactionTest {

    @Test
    fun noSensitiveDataOrCardIdReachesLogsAtTraceLevel() = runTest {
        val jsonResponse = """
            {
              "status_code": "0",
              "currency": "CUP",
              "balance": "$SECRET_BALANCE",
              "balance_available": "$SECRET_AVAILABLE",
              "operation": [
                {
                  "operationDate": "14/10/26",
                  "action": "DB",
                  "channel": "ENZ",
                  "description": "$SECRET_DESC",
                  "amount": "$SECRET_AMOUNT",
                  "alias": "$SECRET_ALIAS"
                },
                {
                  "operationDate": "14/10/26",
                  "action": "DB",
                  "channel": "ENZ",
                  "description": "$SECRET_DESC_OMITTED",
                  "amount": "monto_invalido"
                }
              ]
            }
        """.trimIndent()

        val engine = MockEngine {
            respond(jsonResponse, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }

        val client = HttpClient(engine) {
            install(ContentNegotiation) { json(enzonaJson) }
        }

        withLogging(minimumLevel = LogLevel.TRACE) { sink ->
            val repo = CardsRepositoryImpl(client)
            val result = repo.getActivity(SECRET_CARD_ID)

            assertIs<EzResult.Success<*>>(result)

            val written = sink.everythingWritten()

            // Confirmar que el sink recibió logs reales del repositorio (el aviso de operaciones omitidas)
            assertTrue("Operaciones omitidas" in written, "El sink debe capturar los logs emitidos por CardsRepositoryImpl")

            // Comprobar que ningún dato sensible aparece en los logs registrados
            val secrets = mapOf(
                "id de tarjeta" to SECRET_CARD_ID,
                "saldo contable" to SECRET_BALANCE,
                "saldo disponible" to SECRET_AVAILABLE,
                "importe de operación" to SECRET_AMOUNT,
                "unidades menores (centavos)" to "95000",
                "alias del comercio" to SECRET_ALIAS,
                "descripción de operación" to SECRET_DESC,
                "descripción de operación omitida" to SECRET_DESC_OMITTED,
                "importe inválido de operación" to "monto_invalido",
            )

            for ((label, secret) in secrets) {
                assertFalse(secret in written, "$label ('$secret') apareció en los logs")
            }
        }
    }
}
