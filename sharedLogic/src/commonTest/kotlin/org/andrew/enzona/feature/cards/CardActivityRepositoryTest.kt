package org.andrew.enzona.feature.cards

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import org.andrew.enzona.core.money.Money
import org.andrew.enzona.core.network.TokenProvider
import org.andrew.enzona.core.network.createApiHttpClient
import org.andrew.enzona.core.result.EzResult
import org.andrew.enzona.core.result.Failure
import org.andrew.enzona.feature.cards.data.CardsRepositoryImpl
import org.andrew.enzona.feature.cards.domain.CardActivity
import org.andrew.enzona.feature.cards.domain.OperationDirection
import org.andrew.enzona.feature.cards.domain.OperationKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

private object ActivityNoRefresh : TokenProvider {
    override fun accessToken() = "tok"
    override suspend fun refreshAfterUnauthorized(failedToken: String?) = false
}

class CardActivityRepositoryTest {

    // JSON con forma real del backend (respuesta plana con operación, saldos mixtos número/string,
    // claves camelCase, alias null y operaciones de diversas categorías).
    private val realShape = """
        {
          "status_code": "0",
          "status_denom": "Operación exitosa",
          "transaction_signature": "sig-12345",
          "currency": "CUP",
          "balance": 3395.15,
          "balance_available": "17088.00",
          "bank_account": "9227069990123456",
          "fault": null,
          "operation": [
            {
              "operationDate": "14/10/26",
              "action": "DB",
              "channel": "ENZ",
              "description": "TRF",
              "amount": "950.00",
              "codigo": "1001",
              "avatar": "https://media.enzona.net/img.png",
              "alias": null
            },
            {
              "operationDate": "13/10/26",
              "action": "DB",
              "channel": "ENZ",
              "description": "RMV",
              "amount": 500.00,
              "codigo": "1002",
              "avatar": "false",
              "alias": "null"
            },
            {
              "operationDate": "10/10/26",
              "action": "CR",
              "channel": "EV",
              "description": "",
              "amount": "12000.50",
              "codigo": "1003",
              "avatar": null,
              "alias": ""
            },
            {
              "operationDate": "09/10/26",
              "action": "DB",
              "channel": "ENZ",
              "description": "GAS",
              "amount": "25.00",
              "codigo": "1004",
              "avatar": "https://media.enzona.net/gas.png",
              "alias": "Cupet Central"
            }
          ]
        }
    """.trimIndent()

    private fun repo(
        status: HttpStatusCode = HttpStatusCode.OK,
        body: String = realShape,
        onRequest: (io.ktor.client.request.HttpRequestData) -> Unit = {},
    ) = CardsRepositoryImpl(
        createApiHttpClient(
            ActivityNoRefresh,
            MockEngine { req ->
                onRequest(req)
                respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))
            },
        ),
    )

    private suspend fun activity(r: CardsRepositoryImpl, cardId: String = "card-uuid-1"): CardActivity =
        (r.getActivity(cardId) as EzResult.Success).value

    @Test
    fun mapsMixedNumberAndStringBalancesAndOperations() = runTest {
        val result = activity(repo())

        // Saldo contable parsed from number 3395.15
        assertEquals(Money(339515L, "CUP"), result.balance)
        // Saldo disponible parsed from string "17088.00"
        assertEquals(Money(1708800L, "CUP"), result.available)
        assertEquals("CUP", result.currency)
        assertEquals(0, result.skippedOperations)
        assertEquals(4, result.operations.size)

        // 1. TRF / DB
        val op0 = result.operations[0]
        assertEquals(OperationKind.TRANSFER, op0.kind)
        assertEquals(OperationDirection.DEBIT, op0.direction)
        assertEquals(Money(95000L, "CUP"), op0.amount)
        assertNull(op0.merchantAlias)
        assertEquals("14/10/26", op0.dateText)

        // 2. RMV / DB (amount como número 500.00, alias literal "null")
        val op1 = result.operations[1]
        assertEquals(OperationKind.MOBILE_TOPUP, op1.kind)
        assertEquals(OperationDirection.DEBIT, op1.direction)
        assertEquals(Money(50000L, "CUP"), op1.amount)
        assertNull(op1.merchantAlias)
        assertEquals("13/10/26", op1.dateText)

        // 3. Vacío + EV + CR -> Nómina
        val op2 = result.operations[2]
        assertEquals(OperationKind.PAYROLL, op2.kind)
        assertEquals(OperationDirection.CREDIT, op2.direction)
        assertEquals(Money(1200050L, "CUP"), op2.amount)
        assertNull(op2.merchantAlias)
        assertEquals("10/10/26", op2.dateText)

        // 4. Pago a comercio con alias (GAS sin código telefónico)
        val op3 = result.operations[3]
        assertEquals(OperationKind.MERCHANT_PAYMENT, op3.kind)
        assertEquals(OperationDirection.DEBIT, op3.direction)
        assertEquals(Money(2500L, "CUP"), op3.amount)
        assertEquals("Cupet Central", op3.merchantAlias)
        assertEquals("09/10/26", op3.dateText)
    }

    @Test
    fun requestsVerifiedEndpointWithBearer() = runTest {
        var url: String? = null
        var auth: String? = null
        val r = repo(onRequest = {
            url = it.url.toString()
            auth = it.headers[HttpHeaders.Authorization]
        })

        r.getActivity("card-uuid-12345")
        assertEquals("https://api.enzona.net/coremz/1.0.2/api/balance/lastOperations/card-uuid-12345", url)
        assertEquals("Bearer tok", auth)
    }

    @Test
    fun absentOrEmptyOperationReturnsEmptyListAndZeroSkipped() = runTest {
        val noOpBody = """{"balance":"100.00","balance_available":"100.00","currency":"CUP"}"""
        val res1 = activity(repo(body = noOpBody))
        assertTrue(res1.operations.isEmpty())
        assertEquals(0, res1.skippedOperations)

        val emptyOpBody = """{"balance":"100.00","balance_available":"100.00","currency":"CUP","operation":[]}"""
        val res2 = activity(repo(body = emptyOpBody))
        assertTrue(res2.operations.isEmpty())
        assertEquals(0, res2.skippedOperations)
    }

    @Test
    fun unreadableAmountIsSkippedAndCounted() = runTest {
        val body = """
            {
              "currency": "CUP",
              "operation": [
                {"description":"TRF", "action":"DB", "amount":"basura"},
                {"description":"TRF", "action":"DB", "amount":""},
                {"description":"TRF", "action":"DB", "amount":false},
                {"description":"TRF", "action":"DB", "amount":"50.00"}
              ]
            }
        """.trimIndent()

        val res = activity(repo(body = body))
        assertEquals(1, res.operations.size)
        assertEquals(3, res.skippedOperations)
        assertEquals(Money(5000L, "CUP"), res.operations.first().amount)
    }

    @Test
    fun unreadableBalancesMapToNullWithoutFailing() = runTest {
        val body = """
            {
              "currency": "CUP",
              "balance": "invalido",
              "balance_available": false,
              "operation": []
            }
        """.trimIndent()

        val res = activity(repo(body = body))
        assertNull(res.balance)
        assertNull(res.available)
        assertEquals("CUP", res.currency)
    }

    @Test
    fun httpErrorDoesNotIncludeResponseBody() = runTest {
        val notFound = repo(HttpStatusCode.NotFound, """{"fault":{"message":"secreto_privado"}}""").getActivity("c1")
        val err404 = assertIs<EzResult.Error>(notFound).failure
        assertEquals(Failure.Http(404), err404)
        assertNull(err404.message)

        val serverError = repo(HttpStatusCode.InternalServerError, "Error interno en el servidor").getActivity("c1")
        val err500 = assertIs<EzResult.Error>(serverError).failure
        assertEquals(Failure.Http(500), err500)
        assertNull(err500.message)
    }

    @Test
    fun faultInBodyYieldsUnknownFailure() = runTest {
        val faultBody = """
            {
              "status_code": "500",
              "fault": { "code": "E01", "description": "Error interno del backend" }
            }
        """.trimIndent()

        val r = repo(body = faultBody).getActivity("c1")
        val err = assertIs<EzResult.Error>(r).failure
        assertIs<Failure.Unknown>(err)
        assertEquals("El servicio respondió con un error.", err.message)
    }

    @Test
    fun falseInNonApplicableFieldsIsTolerated() = runTest {
        val body = """
            {
              "balance": false,
              "balance_available": false,
              "currency": "cup",
              "bank_account": false,
              "fault": false,
              "operation": [
                {
                  "operationDate": false,
                  "action": false,
                  "channel": false,
                  "description": false,
                  "amount": "10.00",
                  "alias": false,
                  "avatar": false,
                  "codigo": false
                }
              ]
            }
        """.trimIndent()

        val res = activity(repo(body = body))
        assertNull(res.balance)
        assertNull(res.available)
        assertEquals("cup", res.currency)
        assertEquals(1, res.operations.size)
        val op = res.operations.single()
        assertEquals(Money(1000L, "cup"), op.amount)
        assertNull(op.merchantAlias)
        assertEquals("", op.dateText)
        assertEquals(OperationDirection.CREDIT, op.direction)
        assertEquals(OperationKind.CREDIT, op.kind)
    }

    @Test
    fun nonArrayOperationFieldYieldsEmptyListAndZeroSkipped() = runTest {
        val falseOp = activity(repo(body = """{"currency":"CUP","operation":false}"""))
        assertTrue(falseOp.operations.isEmpty())
        assertEquals(0, falseOp.skippedOperations)

        val nullOp = activity(repo(body = """{"currency":"CUP","operation":null}"""))
        assertTrue(nullOp.operations.isEmpty())
        assertEquals(0, nullOp.skippedOperations)

        val objectOp = activity(repo(body = """{"currency":"CUP","operation":{}}"""))
        assertTrue(objectOp.operations.isEmpty())
        assertEquals(0, objectOp.skippedOperations)
    }

    @Test
    fun arrayOperationWithNonObjectElementsSkipsThemAndCounts() = runTest {
        val mixedArray = """
            {
              "currency": "CUP",
              "operation": [
                "x",
                5,
                {"description":"TRF", "action":"DB", "amount":"50.00"}
              ]
            }
        """.trimIndent()

        val res = activity(repo(body = mixedArray))
        assertEquals(1, res.operations.size)
        assertEquals(2, res.skippedOperations)
        assertEquals(Money(5000L, "CUP"), res.operations.single().amount)
    }

    @Test
    fun aliasStringPreservesTrueAndFalseWhileBooleanFalseIsNull() = runTest {
        val body = """
            {
              "currency": "CUP",
              "operation": [
                {"amount":"10.00", "alias":"True"},
                {"amount":"20.00", "alias":"false"},
                {"amount":"30.00", "alias":false},
                {"amount":"40.00", "alias":"null"}
              ]
            }
        """.trimIndent()

        val res = activity(repo(body = body))
        assertEquals(4, res.operations.size)
        assertEquals("True", res.operations[0].merchantAlias)
        assertEquals("false", res.operations[1].merchantAlias)
        assertNull(res.operations[2].merchantAlias)
        assertNull(res.operations[3].merchantAlias)
    }
}
