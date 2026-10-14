package org.andrew.enzona.feature.cards

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import org.andrew.enzona.core.network.TokenProvider
import org.andrew.enzona.core.network.createApiHttpClient
import org.andrew.enzona.core.result.EzResult
import org.andrew.enzona.core.result.Failure
import org.andrew.enzona.feature.cards.data.CardsRepositoryImpl
import org.andrew.enzona.feature.cards.data.formatExpiry
import org.andrew.enzona.feature.cards.domain.BankCard
import org.andrew.enzona.feature.cards.domain.CardSkin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

private object NoRefresh : TokenProvider {
    override fun accessToken() = "tok"
    override suspend fun refreshAfterUnauthorized(failedToken: String?) = false
}

class CardsRepositoryTest {

    // Misma FORMA que la respuesta real (envoltorio {"card":[...]}, `status`/`verified` como número,
    // `primary_source` como booleano SIN comillas, `create_at` sin "d"), con datos inventados. El
    // orden es el de la API (NO el de pantalla) y cubre Bandec CUP, Bandec USD, Metropolitano y Clásica.
    private val realShape = """
        {"card":[
         {"funding_source_uuid":"u-bandec-cup","last4":"5800","cardholder":"TEST USER","expdate":"0232",
          "create_at":"2022-03-01 14:42:43.0","update_at":"2022-03-01 14:42:54.0","status":1,"currency":"cup",
          "primary_source":false,"bank_name":"Banco de Crédito y Comercio (BANDEC)","bank_code":"06","verified":1,"bin":"922706999"},
         {"funding_source_uuid":"u-bandec-usd","last4":"4912","cardholder":"TEST USER","expdate":"0232",
          "create_at":"2022-03-01 14:44:13.0","status":1,"currency":"usd","primary_source":false,
          "bank_name":"Banco de Credito y Comercio (BANDEC)","bank_code":"06","verified":1,"bin":"923506999"},
         {"funding_source_uuid":"u-banmet","last4":"6856","cardholder":"TEST USER","expdate":"0532",
          "create_at":"2022-07-07 13:54:52.0","status":1,"currency":"cup","primary_source":true,
          "bank_name":"Banco Metropolitano S.A","bank_code":"95","verified":1,"bin":"922795987"},
         {"funding_source_uuid":"u-clasica","last4":"3130","cardholder":"Test User","expdate":"0131",
          "create_at":"2026-06-05 15:03:35.0","status":1,"currency":"usd","primary_source":false,
          "bank_name":"FINCIMEX CLASICA","bank_code":"03","verified":1,"bin":"976003122"}
        ]}
    """.trimIndent()

    private fun repo(status: HttpStatusCode = HttpStatusCode.OK, body: String = realShape,
                     onRequest: (io.ktor.client.request.HttpRequestData) -> Unit = {}) =
        CardsRepositoryImpl(createApiHttpClient(NoRefresh, MockEngine { req ->
            onRequest(req)
            respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))
        }))

    private suspend fun cards(r: CardsRepositoryImpl, user: String = "user1"): List<BankCard> =
        (r.getCards(user) as EzResult.Success).value

    @Test
    fun mapsTheRealShapeAndOrdersPrimaryFirstThenNewest() = runTest {
        val result = cards(repo())

        // Predeterminada primero; luego de la más nueva a la más antigua.
        assertEquals(listOf("6856", "3130", "4912", "5800"), result.map { it.last4 })
        val banmet = result.first()
        assertTrue(banmet.isPrimary)
        assertEquals("05/32", banmet.expiry)
        assertEquals("CUP", banmet.currency)
        assertEquals(CardSkin.BANMET, banmet.skin)
        assertEquals("TEST USER", result[1].holder) // "Test User" -> mayúsculas, como en la tarjeta
    }

    @Test
    fun bandecPicksTheSkinByCurrency() = runTest {
        val byLast4 = cards(repo()).associateBy { it.last4 }
        assertEquals(CardSkin.BANDEC, byLast4.getValue("5800").skin)
        assertEquals(CardSkin.BANDEC_USD, byLast4.getValue("4912").skin)
        assertEquals(CardSkin.CLASICA, byLast4.getValue("3130").skin)
    }

    @Test
    fun requestsTheVerifiedEndpointWithBearer() = runTest {
        var url: String? = null
        var auth: String? = null
        repo(onRequest = { url = it.url.toString(); auth = it.headers[HttpHeaders.Authorization] }).getCards("user1")
        assertEquals("https://api.enzona.net/coremz/1.0.2/card/username/user1", url)
        assertEquals("Bearer tok", auth)
    }

    @Test
    fun anUnreadableCardIsSkippedButTheRestSurvive() = runTest {
        val body = """{"card":[{"bank_code":"06"},{"last4":"1111","bank_code":"95","currency":"cup"}]}"""
        assertEquals(listOf("1111"), cards(repo(body = body)).map { it.last4 })
    }

    @Test
    fun ifEveryCardIsUnreadableItIsAnErrorNotAnEmptyList() = runTest {
        val r = repo(body = """{"card":[{"bank_code":"06"},{"cardholder":"X"}]}""").getCards("u")
        assertIs<Failure.Parse>(assertIs<EzResult.Error>(r).failure)
    }

    @Test
    fun noCardsIsAnEmptyListNotAnError() = runTest {
        assertEquals(emptyList(), cards(repo(body = """{"card":[]}""")))
        assertEquals(emptyList(), cards(repo(body = "{}")))
    }

    @Test
    fun falseInTextFieldsIsTreatedAsAbsentAndUnknownBankIsGeneric() = runTest {
        val body = """{"card":[{"last4":"2222","cardholder":false,"bank_name":false,"bank_code":"77","currency":"cup"}]}"""
        val c = cards(repo(body = body)).single()
        assertEquals("", c.holder)
        assertEquals("", c.bankName)
        assertEquals(CardSkin.GENERIC, c.skin)
    }

    @Test
    fun httpErrorDoesNotLeakTheBodyAndMalformedJsonIsParse() = runTest {
        val http = repo(HttpStatusCode.NotFound, """{"fault":{"message":"datos privados"}}""").getCards("u")
        assertEquals(Failure.Http(404), assertIs<EzResult.Error>(http).failure)
        val bad = repo(body = "esto no es json").getCards("u")
        assertIs<Failure.Parse>(assertIs<EzResult.Error>(bad).failure)
    }

    @Test
    fun expiryParsing() {
        assertEquals("05/32", formatExpiry("0532"))
        assertEquals("12/99", formatExpiry(" 1299 "))
        assertEquals("01/31", formatExpiry("0131"))
        assertNull(formatExpiry("0032"))   // mes 00
        assertNull(formatExpiry("1332"))   // mes 13
        assertNull(formatExpiry("532"))
        assertNull(formatExpiry("05/32"))
        assertNull(formatExpiry("abcd"))
        assertNull(formatExpiry(""))
        assertNull(formatExpiry(null))
    }

    @Test
    fun spokenDescriptionIsReadableAndSplitsTheDigits() = runTest {
        val spoken = cards(repo()).first().spokenDescription()
        assertEquals(
            "Tarjeta Banco Metropolitano S.A, CUP, terminada en 6 8 5 6, titular test user, vence 05 32, predeterminada",
            spoken,
        )
    }
}
