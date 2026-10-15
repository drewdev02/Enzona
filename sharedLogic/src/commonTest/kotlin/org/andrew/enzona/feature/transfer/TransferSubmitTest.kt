package org.andrew.enzona.feature.transfer

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.andrew.enzona.core.crypto.PaymentPasswordEncryptor
import org.andrew.enzona.core.logging.LogLevel
import org.andrew.enzona.core.logging.withLogging
import org.andrew.enzona.core.money.Money
import org.andrew.enzona.core.network.TokenProvider
import org.andrew.enzona.core.network.createApiHttpClient
import org.andrew.enzona.core.result.EzResult
import org.andrew.enzona.core.result.Failure
import org.andrew.enzona.feature.transfer.data.TransferGatewayImpl
import org.andrew.enzona.feature.transfer.domain.NotSentReason
import org.andrew.enzona.feature.transfer.domain.PaymentPassword
import org.andrew.enzona.feature.transfer.domain.RejectionCause
import org.andrew.enzona.feature.transfer.domain.SubmitTransferUseCase
import org.andrew.enzona.feature.transfer.domain.TransferGateway
import org.andrew.enzona.feature.transfer.domain.TransferOutcome
import org.andrew.enzona.feature.transfer.domain.TransferRecipient
import org.andrew.enzona.feature.transfer.domain.TransferRequest
import org.andrew.enzona.feature.transfer.domain.TransferStatus
import org.andrew.enzona.feature.transfer.domain.UnknownCause
import org.andrew.enzona.feature.transfer.domain.notSentReason
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

private class Refresh(private val renews: Boolean) : TokenProvider {
    var token = "tok"
    var refreshes = 0
    override fun accessToken() = token
    override suspend fun refreshAfterUnauthorized(failedToken: String?): Boolean {
        refreshes++
        if (renews) token = "tok2"
        return renews
    }
}

/** Cifrador de mentira: deja ver que el envío usa lo cifrado y nunca la contraseña. */
private class FakeEncryptor(private val fails: Boolean = false) : PaymentPasswordEncryptor {
    override fun encrypt(password: String): EzResult<String> =
        if (fails) EzResult.Error(Failure.Crypto("x")) else EzResult.Success("ENC<${password.length}>")
}

private val account = TransferRequest.ToAccount(
    sourceCardId = "card-src-1",
    recipient = TransferRecipient("acc-uuid-9", "ana.perez", "Ana Pérez", null),
    amount = Money(1_050, "CUP"),
    description = "  Cena privada  ",
)
private val ownCard = TransferRequest.ToOwnCard("card-src-1", "card-dst-2", Money(250_000, "USD"), "ahorro")

class TransferBodyTest {
    private suspend fun bodyOf(request: TransferRequest): JsonObject {
        var captured: HttpRequestData? = null
        val client = createApiHttpClient(Refresh(false), MockEngine { req ->
            captured = req
            respond("""{"transaction_code":"EZ-1","status_code":2222}""", HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        })
        TransferGatewayImpl(client).submit(request, "CIPHERTEXT")
        assertEquals(HttpMethod.Post, captured!!.method)
        assertEquals("/transfer/v1.0.0/transfer/account", captured!!.url.encodedPath)
        return Json.parseToJsonElement(captured!!.body.toByteArray().decodeToString()).jsonObject
    }

    @Test
    fun accountBodyHasTheFieldsOfTheOriginal() = runTest {
        val body = bodyOf(account)
        assertEquals(
            setOf("amount", "funding_source_uuid", "description", "currency", "funding_recipient_uuid",
                "funding_recipient_username", "type_transfer", "payment_password", "fingerprint"),
            body.keys,
        )
        assertEquals("10.50", body["amount"]!!.jsonPrimitive.content)
        assertEquals("card-src-1", body["funding_source_uuid"]!!.jsonPrimitive.content)
        assertEquals("Cena privada", body["description"]!!.jsonPrimitive.content, "se recorta, como el original")
        assertEquals("CUP", body["currency"]!!.jsonPrimitive.content)
        assertEquals("acc-uuid-9", body["funding_recipient_uuid"]!!.jsonPrimitive.content)
        assertEquals("ana.perez", body["funding_recipient_username"]!!.jsonPrimitive.content)
        assertEquals("", body["type_transfer"]!!.jsonPrimitive.content)
        assertEquals("CIPHERTEXT", body["payment_password"]!!.jsonPrimitive.content)
        assertEquals("", body["fingerprint"]!!.jsonPrimitive.content)
    }

    @Test
    fun ownCardBodyHasNoUsernameNorTransferType() = runTest {
        val body = bodyOf(ownCard)
        assertEquals(
            setOf("amount", "funding_source_uuid", "description", "currency", "funding_recipient_uuid",
                "payment_password", "fingerprint"),
            body.keys,
        )
        assertEquals("2500.00", body["amount"]!!.jsonPrimitive.content)
        assertEquals("USD", body["currency"]!!.jsonPrimitive.content, "la moneda es la de la tarjeta de origen")
        assertEquals("card-dst-2", body["funding_recipient_uuid"]!!.jsonPrimitive.content)
    }
}

class TransferOutcomeMappingTest {
    private suspend fun outcome(
        status: HttpStatusCode, body: String, refresh: Refresh = Refresh(false), requests: MutableList<HttpRequestData>? = null,
    ): TransferOutcome {
        val client = createApiHttpClient(refresh, MockEngine { req ->
            requests?.add(req)
            respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))
        })
        return TransferGatewayImpl(client).submit(account, "C")
    }

    @Test
    fun anAcceptedTransferCarriesTheReceipt() = runTest {
        val o = outcome(
            HttpStatusCode.OK,
            """{"transaction_code":"EZ-77","status_code":2222,"status_denom":"Aceptada","created_at":"2026-10-14T16:30:00.000-04:00","transaction_signature":"SIG-1"}""",
        )
        assertIs<TransferOutcome.Accepted>(o)
        assertEquals("EZ-77", o.receipt.operationCode)
        assertEquals(TransferStatus.ACCEPTED, o.receipt.status)
        assertEquals("Aceptada", o.receipt.statusText)
        assertEquals("2026-10-14T16:30:00.000-04:00", o.receipt.createdAtText)
        assertEquals(kotlinx.datetime.LocalDateTime(2026, 10, 14, 16, 30, 0), o.receipt.createdAt, "hora de reloj, sin convertir de zona")
        assertEquals("SIG-1", o.receipt.signature)
    }

    @Test
    fun aReceiptWithoutDateOrSignatureKeepsThemEmpty() = runTest {
        val o = outcome(HttpStatusCode.OK, """{"transaction_code":"EZ-78","status_code":2222,"created_at":false,"transaction_signature":false}""")
        assertIs<TransferOutcome.Accepted>(o)
        assertNull(o.receipt.createdAt)
        assertNull(o.receipt.createdAtText)
        assertNull(o.receipt.signature, "false no es un texto: no es una firma")
    }

    @Test
    fun aPendingTransferIsAcceptedButPending() = runTest {
        val o = outcome(HttpStatusCode.OK, """{"id_transfer":"55","status_code":"2242"}""")
        assertIs<TransferOutcome.Accepted>(o)
        assertEquals(TransferStatus.PENDING, o.receipt.status)
        assertEquals("55", o.receipt.operationCode, "sin transaction_code se usa id_transfer")
    }

    @Test
    fun aSuccessWithAnUnknownStatusIsStillAcceptedWhenItLooksLikeATransfer() = runTest {
        val o = outcome(HttpStatusCode.OK, """{"transaction_code":"EZ-1","status_denom":"Procesando"}""")
        assertIs<TransferOutcome.Accepted>(o)
        assertEquals(TransferStatus.OTHER, o.receipt.status)
        assertEquals("Procesando", o.receipt.statusText)
    }

    @Test
    fun aFailedStatusOrAFaultMeansTheServerDidNotDoIt() = runTest {
        val failed = outcome(HttpStatusCode.OK, """{"status_code":2224,"transaction_code":"EZ-1"}""")
        assertEquals(TransferOutcome.Rejected(RejectionCause.FAILED_BY_SERVER, 200, null), failed)

        val fault = outcome(HttpStatusCode.OK, """{"fault":{"message":"Saldo insuficiente"}}""")
        assertEquals(TransferOutcome.Rejected(RejectionCause.FAILED_BY_SERVER, 200, "Saldo insuficiente"), fault)

        val textFault = outcome(HttpStatusCode.OK, """{"fault":"Cuenta bloqueada"}""")
        assertEquals(TransferOutcome.Rejected(RejectionCause.FAILED_BY_SERVER, 200, "Cuenta bloqueada"), textFault)

        val noText = outcome(HttpStatusCode.OK, """{"fault":{"code":1}}""")
        assertEquals(TransferOutcome.Rejected(RejectionCause.FAILED_BY_SERVER, 200, null), noText)
    }

    @Test
    fun faultFalseIsNotAFault() = runTest {
        assertIs<TransferOutcome.Accepted>(outcome(HttpStatusCode.OK, """{"fault":false,"transaction_code":"EZ-1"}"""))
    }

    @Test
    fun aSuccessThatDoesNotLookLikeATransferIsUnknownNotAccepted() = runTest {
        assertEquals(TransferOutcome.Unknown(UnknownCause.UNRECOGNIZED_RESPONSE, 200), outcome(HttpStatusCode.OK, "{}"))
        assertEquals(TransferOutcome.Unknown(UnknownCause.UNRECOGNIZED_RESPONSE, 200), outcome(HttpStatusCode.OK, """{"otra":"cosa"}"""))
    }

    @Test
    fun anUnreadableSuccessBodyIsUnknown() = runTest {
        assertEquals(TransferOutcome.Unknown(UnknownCause.UNREADABLE_RESPONSE, 200), outcome(HttpStatusCode.OK, "<html>no es json</html>"))
        assertEquals(TransferOutcome.Unknown(UnknownCause.UNREADABLE_RESPONSE, 200), outcome(HttpStatusCode.OK, "[1,2]"))
    }

    @Test
    fun clientErrorsAreRefusalsAndKeepTheServerText() = runTest {
        val o = outcome(HttpStatusCode.BadRequest, """{"fault":{"description":"Monto inválido"}}""")
        assertEquals(TransferOutcome.Rejected(RejectionCause.REFUSED, 400, "Monto inválido"), o)
        assertEquals(TransferOutcome.Rejected(RejectionCause.REFUSED, 403, null), outcome(HttpStatusCode.Forbidden, "no json"))
        assertEquals(TransferOutcome.Rejected(RejectionCause.REFUSED, 422, null), outcome(HttpStatusCode.UnprocessableEntity, "{}"))
    }

    @Test
    fun serverErrorsAndTimeoutsAreUnknownBecauseThePaymentMayHaveGoneThrough() = runTest {
        for (code in listOf(500, 502, 503, 504)) {
            val o = outcome(HttpStatusCode.fromValue(code), "{}")
            assertEquals(TransferOutcome.Unknown(UnknownCause.SERVER_ERROR, code), o, "HTTP $code")
        }
        assertEquals(TransferOutcome.Unknown(UnknownCause.TIMEOUT, 408), outcome(HttpStatusCode.RequestTimeout, "{}"))
        assertEquals(TransferOutcome.Unknown(UnknownCause.UNEXPECTED_STATUS, 302), outcome(HttpStatusCode.Found, "{}"))
    }

    @Test
    fun anExpiredSessionIsRejectedAfterOneRefreshAndOneResend() = runTest {
        val requests = mutableListOf<HttpRequestData>()
        val refresh = Refresh(renews = true)
        val o = outcome(HttpStatusCode.Unauthorized, "{}", refresh, requests)
        assertEquals(TransferOutcome.Rejected(RejectionCause.SESSION_EXPIRED, 401, null), o)
        assertEquals(1, refresh.refreshes)
        assertEquals(2, requests.size, "tras renovar, se reenvía UNA vez (el 401 no procesó nada)")
        assertEquals("Bearer tok2", requests[1].headers[HttpHeaders.Authorization])
    }

    @Test
    fun withoutRefreshA401IsOneRequest() = runTest {
        val requests = mutableListOf<HttpRequestData>()
        val o = outcome(HttpStatusCode.Unauthorized, "{}", Refresh(false), requests)
        assertEquals(TransferOutcome.Rejected(RejectionCause.SESSION_EXPIRED, 401, null), o)
        assertEquals(1, requests.size)
    }
}

class TransferNetworkFailureTest {
    @Test
    fun aNetworkFailureIsUnknownAndIsNeverRetried() = runTest {
        var calls = 0
        val client = createApiHttpClient(Refresh(false), MockEngine { calls++; throw RuntimeException("conexión cortada") })
        val o = TransferGatewayImpl(client).submit(account, "C")
        assertEquals(TransferOutcome.Unknown(UnknownCause.NO_RESPONSE), o)
        assertEquals(1, calls)
    }
}

class SubmitTransferUseCaseTest {
    private class RecordingGateway(var answer: suspend () -> TransferOutcome) : TransferGateway {
        val sent = mutableListOf<Pair<TransferRequest, String>>()
        override suspend fun submit(request: TransferRequest, encryptedPassword: String): TransferOutcome {
            sent += request to encryptedPassword
            return answer()
        }
    }

    private val accepted = TransferOutcome.Accepted(
        org.andrew.enzona.feature.transfer.domain.TransferReceipt("EZ-1", TransferStatus.ACCEPTED, "Aceptada", null, null, null),
    )

    private fun useCase(gateway: TransferGateway, encryptor: PaymentPasswordEncryptor = FakeEncryptor()) =
        SubmitTransferUseCase(gateway, encryptor)

    @Test
    fun sendsTheEncryptedPasswordNeverThePlainOne() = runTest {
        val gateway = RecordingGateway { accepted }
        val o = useCase(gateway)(account, PaymentPassword("123456"))
        assertEquals(accepted, o)
        assertEquals(1, gateway.sent.size)
        assertEquals("ENC<6>", gateway.sent[0].second)
        assertEquals(account, gateway.sent[0].first)
    }

    @Test
    fun theUseCaseNeverReceivesAPlainPasswordInTheRequest() {
        // Ninguna variante de TransferRequest tiene un campo de contraseña: compilar es la garantía.
        assertEquals("PaymentPassword(•••)", PaymentPassword("123456").toString())
    }

    @Test
    fun aRequestThatBreaksARuleIsNotSent() = runTest {
        val gateway = RecordingGateway { accepted }
        val uc = useCase(gateway)
        val password = PaymentPassword("1234")
        fun cause(request: TransferRequest) = request.notSentReason()

        assertEquals(NotSentReason.NON_POSITIVE_AMOUNT, cause(account.copy(amount = Money(0, "CUP"))))
        assertEquals(NotSentReason.NON_POSITIVE_AMOUNT, cause(account.copy(amount = Money(-5, "CUP"))))
        assertEquals(NotSentReason.MISSING_CURRENCY, cause(account.copy(amount = Money(100, " "))))
        assertEquals(NotSentReason.MISSING_SOURCE_CARD, cause(account.copy(sourceCardId = "")))
        assertEquals(NotSentReason.MISSING_DESTINATION, cause(account.copy(recipient = account.recipient.copy(accountUuid = ""))))
        assertEquals(NotSentReason.MISSING_DESTINATION, cause(account.copy(recipient = account.recipient.copy(username = " "))))
        assertEquals(NotSentReason.MISSING_DESTINATION, cause(ownCard.copy(destinationCardId = "")))
        assertEquals(NotSentReason.SAME_CARD, cause(ownCard.copy(destinationCardId = ownCard.sourceCardId)))
        assertNull(cause(account))
        assertNull(cause(ownCard))

        for (bad in listOf(account.copy(amount = Money(0, "CUP")), ownCard.copy(destinationCardId = "card-src-1"))) {
            assertIs<TransferOutcome.NotSent>(uc(bad, password))
        }
        assertTrue(gateway.sent.isEmpty(), "nada se envía si una regla falla")
    }

    @Test
    fun anEmptyPasswordIsNotSent() = runTest {
        val gateway = RecordingGateway { accepted }
        assertEquals(TransferOutcome.NotSent(NotSentReason.PASSWORD_EMPTY), useCase(gateway)(account, PaymentPassword("")))
        assertTrue(gateway.sent.isEmpty())
    }

    @Test
    fun aPasswordThatCannotBeProtectedIsNotSentAndDoesNotLockTheNextTransfer() = runTest {
        val gateway = RecordingGateway { accepted }
        val broken = useCase(gateway, FakeEncryptor(fails = true))
        assertEquals(TransferOutcome.NotSent(NotSentReason.PASSWORD_UNUSABLE), broken(account, PaymentPassword("1234")))
        assertTrue(gateway.sent.isEmpty())
        // El mismo caso de uso (mismo Mutex) sigue libre: el fallo no dejó el candado puesto.
        assertEquals(TransferOutcome.NotSent(NotSentReason.PASSWORD_UNUSABLE), broken(account, PaymentPassword("1234")))
    }

    @Test
    fun aSecondTransferWhileOneIsInFlightIsNotSent() = runTest(timeout = 10.seconds) {
        val release = CompletableDeferred<Unit>()
        val gateway = RecordingGateway { release.await(); accepted }
        val uc = useCase(gateway)
        val first = async(start = CoroutineStart.UNDISPATCHED) { uc(account, PaymentPassword("1234")) }
        // El primero está dentro del gateway, esperando.
        assertEquals(1, gateway.sent.size)

        val second = uc(ownCard, PaymentPassword("1234"))
        assertEquals(TransferOutcome.NotSent(NotSentReason.ANOTHER_TRANSFER_IN_PROGRESS), second)
        assertEquals(1, gateway.sent.size, "el segundo no llegó al gateway")

        release.complete(Unit)
        assertEquals(accepted, first.await())

        // Ya libre: una nueva transferencia sí se envía.
        assertEquals(accepted, uc(ownCard, PaymentPassword("1234")))
        assertEquals(2, gateway.sent.size)
    }

    @Test
    fun theLockIsReleasedEvenIfTheGatewayThrows() = runTest {
        var explode = true
        val gateway = RecordingGateway { if (explode) error("boom") else accepted }
        val uc = useCase(gateway)
        assertFailsWith<IllegalStateException> { uc(account, PaymentPassword("1234")) }
        explode = false
        assertEquals(accepted, uc(account, PaymentPassword("1234")))
    }

    @Test
    fun closingTheScreenDoesNotCancelARequestAlreadySent() = runTest(timeout = 10.seconds) {
        val release = CompletableDeferred<Unit>()
        var finished = false
        val gateway = RecordingGateway { release.await(); finished = true; accepted }
        val uc = useCase(gateway)
        val job = launch(start = CoroutineStart.UNDISPATCHED) { uc(account, PaymentPassword("1234")) }
        assertEquals(1, gateway.sent.size)

        job.cancel() // la pantalla se cerró con la petición en vuelo
        release.complete(Unit)
        job.join()
        assertTrue(finished, "la respuesta se esperó y se procesó pese a la cancelación")
    }

    @Test
    fun unknownOutcomesPassThroughUntouchedAndNothingIsRetried() = runTest {
        val unknown = TransferOutcome.Unknown(UnknownCause.NO_RESPONSE)
        val gateway = RecordingGateway { unknown }
        assertEquals(unknown, useCase(gateway)(account, PaymentPassword("1234")))
        assertEquals(1, gateway.sent.size, "un desconocido no se reintenta solo")
    }

    @Test
    fun logsNeverContainThePasswordTheCipherTheAmountOrTheRecipient() = runTest {
        val gateway = RecordingGateway { accepted }
        withLogging(minimumLevel = LogLevel.TRACE) { sink ->
            val uc = useCase(gateway)
            uc(account, PaymentPassword("clave-secreta-777"))
            uc(account.copy(amount = Money(0, "CUP")), PaymentPassword("clave-secreta-777"))
            val written = sink.everythingWritten()
            assertTrue("Transferencia" in written, "el test debe observar logs reales")
            for (secret in listOf("clave-secreta-777", "ENC<", "10.50", "1050", "card-src-1", "acc-uuid-9", "ana.perez", "Ana Pérez", "Cena privada", "EZ-1")) {
                assertFalse(secret in written, "$secret apareció en los logs")
            }
        }
    }
}
