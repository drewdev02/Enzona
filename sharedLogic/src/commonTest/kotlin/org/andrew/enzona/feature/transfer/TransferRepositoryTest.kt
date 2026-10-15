package org.andrew.enzona.feature.transfer

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import org.andrew.enzona.core.logging.LogLevel
import org.andrew.enzona.core.logging.withLogging
import org.andrew.enzona.core.money.Money
import org.andrew.enzona.core.network.TokenProvider
import org.andrew.enzona.core.network.createApiHttpClient
import org.andrew.enzona.core.result.EzResult
import org.andrew.enzona.core.result.Failure
import org.andrew.enzona.feature.transfer.data.TransferRepositoryImpl
import org.andrew.enzona.feature.transfer.data.parseWallClock
import org.andrew.enzona.feature.transfer.data.statusOf
import org.andrew.enzona.feature.transfer.domain.TransferDirection
import org.andrew.enzona.feature.transfer.domain.TransferFilter
import org.andrew.enzona.feature.transfer.domain.TransferPage
import org.andrew.enzona.feature.transfer.domain.TransferStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

private object NoRefresh : TokenProvider {
    override fun accessToken() = "tok"
    override suspend fun refreshAfterUnauthorized(failedToken: String?) = false
}

class TransferRepositoryTest {

    // Misma FORMA que la respuesta decodificada del original (`transfer[]` + `avatars{}`; importe como texto CON
    // SIGNO; `source`/`recipient` anidados; `created_at` con desfase), con datos inventados.
    private val realShape = """
        {"avatars":{"alice":"images/user/avatar/alice.png"},
         "transfer":[
          {"uuid":"t-1","transaction_code":"EZ-0001","status_code":2222,"status_denom":"Aceptada",
           "created_at":"2026-10-03T13:41:07.000-04:00","amount":"-1400.00","currency":"CUP","description":"Alquiler",
           "source":{"bank":"BANMET","last4":"6856","username":"yo","name":"Test","lastname":"User","avatar":false},
           "recipient":{"bank":"BANMET","last4":"4646","name":false,"lastname":false,"username":false,"avatar":false}},
          {"uuid":"t-2","status_code":"2242","status_denom":"Pendiente","created_at":"2026-09-27 10:31:00",
           "amount":"+500.00","currency":"CUP","description":false,
           "source":{"username":"alice","name":"Alice","lastname":"Perez","avatar":"images/user/avatar/alice.png"},
           "recipient":{"bank":"BANMET","last4":"6856","username":"yo"}},
          {"uuid":"t-3","status_denom":"Fallida","created_at":"2026-09-15T22:52:00.000-04:00","amount":"600.00","currency":"CUP",
           "source":{"bank":"BANMET","last4":"6856"},"recipient":{"bank":"BANDEC","last4":"9560"}}
         ]}
    """.trimIndent()

    private fun repo(
        status: HttpStatusCode = HttpStatusCode.OK,
        body: String = realShape,
        onRequest: (io.ktor.client.request.HttpRequestData) -> Unit = {},
    ) = TransferRepositoryImpl(createApiHttpClient(NoRefresh, MockEngine { req ->
        onRequest(req)
        respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))
    }))

    private suspend fun page(r: TransferRepositoryImpl, offset: Int = 0, limit: Int = 10, filter: TransferFilter = TransferFilter.None): TransferPage =
        (r.list(offset, limit, filter) as EzResult.Success).value

    @Test
    fun mapsTheRealShape() = runTest {
        val p = page(repo())
        assertEquals(3, p.items.size)
        assertEquals(0, p.skipped)

        val out = p.items[0]
        assertEquals("t-1", out.id)
        assertEquals(TransferDirection.OUTGOING, out.direction)
        assertEquals(Money(140_000, "CUP"), out.amount) // magnitud; el sentido va aparte
        assertEquals(TransferStatus.ACCEPTED, out.status)
        assertEquals("Aceptada", out.statusText)
        assertEquals("BANMET", out.recipient.bank)
        assertEquals("4646", out.recipient.last4)
        assertEquals("BANMET", out.source.bank)
        assertEquals("6856", out.source.last4)
        assertEquals("Test User", out.source.name)
        assertNull(out.recipient.name, "name:false es ausente, no el texto 'false'")
        assertEquals("Alquiler", out.description)
        assertEquals(LocalDateTime(2026, 10, 3, 13, 41, 7), out.createdAt)
        assertEquals("2026-10-03T13:41:07.000-04:00", out.createdAtText)

        val incoming = p.items[1]
        assertEquals(TransferDirection.INCOMING, incoming.direction)
        assertEquals(Money(50_000, "CUP"), incoming.amount)
        assertEquals(TransferStatus.PENDING, incoming.status) // status_code llega como TEXTO
        assertEquals("Alice Perez", incoming.source.name)
        assertEquals("images/user/avatar/alice.png", incoming.source.avatar)
        assertNull(incoming.description)
        assertEquals(LocalDateTime(2026, 9, 27, 10, 31, 0), incoming.createdAt, "con espacio en lugar de T")

        val unsigned = p.items[2]
        assertEquals(TransferDirection.UNKNOWN, unsigned.direction, "sin signo no se adivina el sentido")
        assertEquals(TransferStatus.FAILED, unsigned.status, "sin status_code se usa el texto")
    }

    @Test
    fun requestsTheVerifiedEndpointWithPagingAndFilters() = runTest {
        var url: String? = null
        val filter = TransferFilter(LocalDate(2026, 10, 1), LocalDate(2026, 10, 31), TransferStatus.ACCEPTED)
        page(repo(onRequest = { url = it.url.toString() }), offset = 20, limit = 10, filter = filter)
        val u = checkNotNull(url)
        assertTrue(u.startsWith("https://api.enzona.net/transfer/v1.0.0/transfer?"), u)
        for (expected in listOf("limit=10", "offset=20", "status_filter=2222", "start_date=2026-10-01", "end_date=2026-10-31")) {
            assertTrue(expected in u, "falta $expected en $u")
        }
    }

    @Test
    fun withoutFiltersOnlyPagingIsSent() = runTest {
        var url: String? = null
        page(repo(onRequest = { url = it.url.toString() }))
        val u = checkNotNull(url)
        assertFalse("status_filter" in u || "start_date" in u || "end_date" in u, u)
    }

    @Test
    fun anAbsentOrNonArrayTransferListIsEmptyNotAFailure() = runTest {
        for (body in listOf("{}", """{"transfer":false}""", """{"transfer":null}""", """{"transfer":{}}""", """{"avatars":{}}""")) {
            val p = page(repo(body = body))
            assertTrue(p.items.isEmpty(), body)
            assertEquals(0, p.skipped, body)
        }
    }

    @Test
    fun unreadableRowsAreSkippedAndCounted() = runTest {
        val body = """
            {"transfer":[
              "x", 5, false,
              {"uuid":"a","amount":"basura","currency":"CUP"},
              {"uuid":"b","currency":"CUP"},
              {"uuid":"c","amount":false,"currency":"CUP"},
              {"uuid":"d","amount":"-92233720368547758.08","currency":"CUP"},
              {"uuid":"ok","amount":"-10.00","currency":"CUP"}
            ]}
        """.trimIndent()
        val p = page(repo(body = body))
        assertEquals(listOf("ok"), p.items.map { it.id })
        assertEquals(7, p.skipped)
    }

    @Test
    fun theIdFallsBackAndStaysUnique() = runTest {
        val body = """{"transfer":[
            {"transaction_uuid":"tu","amount":"-1.00"},
            {"id_transfer":77,"amount":"-1.00"},
            {"transaction_code":"EZ-9","amount":"-1.00"},
            {"amount":"-1.00"}]}""".trimIndent()
        val ids = page(repo(body = body), offset = 30).items.map { it.id }
        assertEquals(listOf("tu", "77", "EZ-9", "row-33"), ids)
    }

    @Test
    fun httpErrorsCarryNoResponseBody() = runTest {
        val r = repo(HttpStatusCode.NotFound, """{"secret":"datos personales"}""").list(0, 10)
        assertEquals(Failure.Http(404), (r as EzResult.Error).failure)
        assertEquals(Failure.Http(500), ((repo(HttpStatusCode.InternalServerError).list(0, 10)) as EzResult.Error).failure)
    }

    @Test
    fun aFaultInTheBodyIsAnError() = runTest {
        val r = repo(body = """{"fault":{"code":"900901","message":"Invalid Credentials"}}""").list(0, 10)
        assertIs<Failure.Unknown>((r as EzResult.Error).failure)
        // `fault:false` no es un error
        assertIs<EzResult.Success<*>>(repo(body = """{"fault":false,"transfer":[]}""").list(0, 10))
    }

    @Test
    fun aBodyThatIsNotAnObjectIsAParseFailureNotACrash() = runTest {
        val r = repo(body = """["no","soy","un","objeto"]""").list(0, 10)
        assertIs<Failure.Parse>((r as EzResult.Error).failure)
    }

    @Test
    fun morePagesMayExistOnlyWhenAFullPageArrived() = runTest {
        val three = page(repo(), limit = 3)
        assertTrue(three.mayHaveMore)
        val ten = page(repo(), limit = 10)
        assertFalse(ten.mayHaveMore)
        // las filas omitidas también cuentan: la página sí estaba llena
        val withSkipped = page(repo(body = """{"transfer":["x",{"amount":"-1.00"}]}"""), limit = 2)
        assertTrue(withSkipped.mayHaveMore)
    }

    @Test
    fun statusFromCodeThenFromText() {
        assertEquals(TransferStatus.ACCEPTED, statusOf("2222", null))
        assertEquals(TransferStatus.PENDING, statusOf("2242", "Aceptada"), "el código manda sobre el texto")
        assertEquals(TransferStatus.FAILED, statusOf("2224", null))
        assertEquals(TransferStatus.ACCEPTED, statusOf(null, "ACEPTADA"))
        assertEquals(TransferStatus.PENDING, statusOf("999", "Pendiente de pago"))
        assertEquals(TransferStatus.FAILED, statusOf(null, "Rechazada"))
        assertEquals(TransferStatus.OTHER, statusOf("999", "Algo nuevo"))
        assertEquals(TransferStatus.OTHER, statusOf(null, null))
    }

    @Test
    fun wallClockIgnoresTheOffsetLikeTheOriginalApp() {
        val expected = LocalDateTime(2026, 10, 3, 13, 41, 7)
        assertEquals(expected, parseWallClock("2026-10-03T13:41:07.000-04:00"))
        assertEquals(expected, parseWallClock("2026-10-03T13:41:07Z"))
        assertEquals(expected, parseWallClock("2026-10-03 13:41:07"))
        assertEquals(expected, parseWallClock("  2026-10-03T13:41:07  "))
        assertNull(parseWallClock("2026-10-03"))
        assertNull(parseWallClock("no es una fecha!!!!!"))
        assertNull(parseWallClock(""))
        assertNull(parseWallClock(null))
        assertNull(parseWallClock("2026-13-45T25:61:61"))
    }

    @Test
    fun theFilterOnlyAcceptsStatusesWithAKnownCode() {
        TransferFilter(status = TransferStatus.PENDING)
        assertFailsWith<IllegalArgumentException> { TransferFilter(status = TransferStatus.OTHER) }
    }

    @Test
    fun noPersonalDataReachesTheLogs() = runTest {
        val body = """{"transfer":[
            {"uuid":"u-SECRETO-1","amount":"-1400.00","currency":"CUP","description":"nota privada",
             "source":{"name":"Nombre","lastname":"Secreto","last4":"6856","username":"usuario-secreto"},
             "recipient":{"last4":"4646","bank":"BANMET"}},
            "fila rota"]}""".trimIndent()
        withLogging(minimumLevel = LogLevel.TRACE) { sink ->
            repo(body = body).list(0, 10)
            val written = sink.everythingWritten()
            assertTrue("omitidas" in written, "el test debe observar logs reales")
            for (secret in listOf("u-SECRETO-1", "1400", "nota privada", "Nombre", "Secreto", "6856", "4646", "usuario-secreto")) {
                assertFalse(secret in written, "$secret apareció en los logs")
            }
        }
    }
}
