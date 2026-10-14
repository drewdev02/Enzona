package org.andrew.enzona.core.network

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondOk
import io.ktor.client.request.get
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

private class FakeTokens(
    var token: String?,
    private val onRefresh: (FakeTokens) -> Boolean,
) : TokenProvider {
    var refreshCalls = 0
    override fun accessToken() = token
    override suspend fun refreshAfterUnauthorized(failedToken: String?): Boolean {
        refreshCalls++
        return onRefresh(this)
    }
}

class ApiHttpClientTest {

    @Test
    fun sendsBearerToApiHost() = runTest {
        val seen = mutableListOf<String?>()
        val engine = MockEngine { req ->
            seen += req.headers[HttpHeaders.Authorization]
            respondOk()
        }
        val client = createApiHttpClient(FakeTokens("A") { true }, engine)

        client.get("/account/v1.0.0/account")

        assertEquals(listOf<String?>("Bearer A"), seen)
    }

    @Test
    fun on401RefreshesOnceAndRetriesWithNewToken() = runTest {
        val seen = mutableListOf<String?>()
        val engine = MockEngine { req ->
            val auth = req.headers[HttpHeaders.Authorization]
            seen += auth
            if (auth == "Bearer B") respondOk() else respond("", HttpStatusCode.Unauthorized)
        }
        val tokens = FakeTokens("A") { it.token = "B"; true }
        val client = createApiHttpClient(tokens, engine)

        val response = client.get("/coremz/1.0.2/card/username/x")

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(listOf<String?>("Bearer A", "Bearer B"), seen)
        assertEquals(1, tokens.refreshCalls)
    }

    @Test
    fun whenRefreshFailsReturns401WithoutRetrying() = runTest {
        var requests = 0
        val engine = MockEngine { requests++; respond("", HttpStatusCode.Unauthorized) }
        val tokens = FakeTokens("A") { false }
        val client = createApiHttpClient(tokens, engine)

        val response = client.get("/account/v1.0.0/account")

        assertEquals(HttpStatusCode.Unauthorized, response.status)
        assertEquals(1, requests)
        assertEquals(1, tokens.refreshCalls)
    }

    @Test
    fun neverRetriesMoreThanOnce() = runTest {
        var requests = 0
        val engine = MockEngine { requests++; respond("", HttpStatusCode.Unauthorized) }
        // El refresh "funciona" pero el servidor sigue rechazando: no debe haber bucle.
        val tokens = FakeTokens("A") { it.token = "B"; true }
        val client = createApiHttpClient(tokens, engine)

        val response = client.get("/account/v1.0.0/account")

        assertEquals(HttpStatusCode.Unauthorized, response.status)
        assertEquals(2, requests)
        assertEquals(1, tokens.refreshCalls)
    }

    @Test
    fun doesNotSendTokenToOtherHosts() = runTest {
        var auth: String? = "unset"
        val engine = MockEngine { req -> auth = req.headers[HttpHeaders.Authorization]; respondOk() }
        val client = createApiHttpClient(FakeTokens("A") { true }, engine)

        client.get("https://example.com/anything")

        assertNull(auth)
    }
}
