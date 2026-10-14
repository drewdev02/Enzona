package org.andrew.enzona.feature.account

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
import org.andrew.enzona.feature.account.data.AccountRepositoryImpl
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

private object NoRefresh : TokenProvider {
    override fun accessToken() = "tok"
    override suspend fun refreshAfterUnauthorized(failedToken: String?) = false
}

class AccountRepositoryTest {

    // Misma FORMA que la respuesta real del backend (envoltorio, flags como texto, `false`
    // en campos no aplicables, `merchants:[{}]`), con datos inventados.
    private val realShapeJson = """
        {"account":{"uuid":"abc123","username":"user1","email":"test@example.com",
        "avatar":"images/user/avatar/user1.png","name":"Test","lastname":"User",
        "phone":"55555555","gender":"M","created_at":"2019-07-24T13:21:20.000-04:00",
        "public_phone":"false","public_email":"true","offline_payment":"false",
        "payment_password":"true","merchant_account":"false","address":"Calle 1",
        "profile_level":"8","primary_currency":"cup","merchants":[{}],
        "fingerprint":"true","verified":"1","verified_phone":"1"}}
    """.trimIndent()

    private fun repo(
        status: HttpStatusCode = HttpStatusCode.OK,
        body: String = realShapeJson,
        onRequest: (io.ktor.client.request.HttpRequestData) -> Unit = {},
    ) = AccountRepositoryImpl(
        createApiHttpClient(
            NoRefresh,
            MockEngine { req ->
                onRequest(req)
                respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))
            },
        )
    )

    @Test
    fun mapsRealShapeIncludingLaxTypes() = runTest {
        val result = repo().getAccount("user1")

        val account = assertIs<EzResult.Success<*>>(result).value as org.andrew.enzona.feature.account.domain.Account
        assertEquals("user1", account.username)
        assertEquals("Test User", account.fullName)
        assertEquals("TU", account.initials)
        assertEquals("CUP", account.primaryCurrency)
        assertEquals(8, account.profileLevel)
        assertTrue(account.isVerified)
        assertTrue(account.isPhoneVerified)
        assertTrue(account.hasPaymentPassword)
        assertTrue(account.biometricEnabled)
        assertEquals(false, account.isMerchant)
        assertEquals("https://media.enzona.net/images/user/avatar/user1.png", account.avatarUrl)
    }

    @Test
    fun sendsUsernameAndBearer() = runTest {
        var url: String? = null
        var auth: String? = null
        repo { req -> url = req.url.toString(); auth = req.headers[HttpHeaders.Authorization] }
            .getAccount("user1")

        assertEquals("https://api.enzona.net/account/v1.0.0/account?username=user1", url)
        assertEquals("Bearer tok", auth)
    }

    @Test
    fun falseInTextFieldsIsTreatedAsAbsent() = runTest {
        val body = """{"account":{"username":"user1","email":false,"phone":false,"avatar":false}}"""
        val account = (repo(body = body).getAccount("user1") as EzResult.Success).value

        assertNull(account.email)
        assertNull(account.phone)
        assertNull(account.avatarUrl)
        assertEquals("user1", account.fullName) // sin nombre, cae al username
    }

    @Test
    fun httpErrorBecomesHttpFailureWithoutLeakingBody() = runTest {
        val r = repo(HttpStatusCode.NotFound, """{"fault":{"code":404,"message":"datos privados"}}""")
            .getAccount("user1")

        val failure = assertIs<EzResult.Error>(r).failure
        assertEquals(Failure.Http(404), failure) // sin message: el cuerpo no se propaga
    }

    @Test
    fun malformedBodyBecomesParseFailure() = runTest {
        val r = repo(body = "esto no es json").getAccount("user1")
        assertIs<Failure.Parse>(assertIs<EzResult.Error>(r).failure)
    }

    @Test
    fun missingEnvelopeOrUsernameBecomesParseFailure() = runTest {
        assertIs<Failure.Parse>(assertIs<EzResult.Error>(repo(body = "{}").getAccount("u")).failure)
        assertIs<Failure.Parse>(
            assertIs<EzResult.Error>(repo(body = """{"account":{"name":"X"}}""").getAccount("u")).failure
        )
    }
}
