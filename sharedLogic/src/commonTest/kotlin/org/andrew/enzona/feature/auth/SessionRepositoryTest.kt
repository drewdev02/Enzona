package org.andrew.enzona.feature.auth

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.andrew.enzona.core.result.EzResult
import org.andrew.enzona.core.result.Failure
import org.andrew.enzona.core.storage.SecureStorage
import org.andrew.enzona.feature.auth.data.OAuthService
import org.andrew.enzona.feature.auth.data.TokenStore
import org.andrew.enzona.feature.auth.data.dto.TokenDto
import org.andrew.enzona.feature.auth.domain.SessionRepository
import org.andrew.enzona.feature.auth.domain.SessionState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull

private class MemoryTokens : SecureStorage {
    private val map = mutableMapOf<String, String>()
    override fun put(key: String, value: String) { map[key] = value }
    override fun get(key: String) = map[key]
    override fun remove(key: String) { map.remove(key) }
    override fun clear() = map.clear()
}

/** id_token con payload `{"sub":"user1"}`; la firma no se verifica en cliente. */
private const val ID_TOKEN = "x.eyJzdWIiOiJ1c2VyMSJ9.y"

private const val TOKEN_JSON =
    """{"access_token":"new-access","refresh_token":"new-refresh","token_type":"Bearer","expires_in":3600,"id_token":"$ID_TOKEN"}"""

class SessionRepositoryTest {

    private fun idp(status: HttpStatusCode = HttpStatusCode.OK, body: String = TOKEN_JSON): HttpClient =
        HttpClient(MockEngine {
            respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))
        }) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }

    private fun repo(
        client: HttpClient = idp(),
        store: TokenStore = TokenStore(MemoryTokens()),
    ) = SessionRepository(OAuthService(client), store)

    private fun TokenStore.withSession(refresh: String? = "rt") =
        also { save(TokenDto(accessToken = "at", refreshToken = refresh, idToken = ID_TOKEN)) }

    @Test
    fun startsLoggedOutWithoutStoredTokens() {
        assertEquals(SessionState.LoggedOut(), repo().state.value)
    }

    @Test
    fun restoresTheStoredSessionAtConstruction() {
        val r = repo(store = TokenStore(MemoryTokens()).withSession())
        assertEquals(SessionState.LoggedIn, r.state.value)
        assertEquals("user1", r.username())
    }

    @Test
    fun debugExportReadsTheThreeStoredTokens() {
        val tokens = repo(store = TokenStore(MemoryTokens()).withSession()).sessionTokensForDebug()
        assertEquals("at", tokens.accessToken)
        assertEquals("rt", tokens.refreshToken)
        assertEquals(ID_TOKEN, tokens.idToken)
    }

    @Test
    fun debugExportIsEmptyWithoutASession() {
        val tokens = repo().sessionTokensForDebug()
        assertNull(tokens.accessToken)
        assertNull(tokens.refreshToken)
        assertNull(tokens.idToken)
    }

    @Test
    fun loginExchangesTheCodeSavesTokensAndOpensTheSession() = runTest {
        val store = TokenStore(MemoryTokens())
        val r = repo(store = store)
        val url = r.startLogin()
        assertNotNull(url)

        assertIs<EzResult.Success<Unit>>(r.completeLogin("the-code"))
        assertEquals(SessionState.LoggedIn, r.state.value)
        assertEquals("new-access", store.accessToken())
    }

    @Test
    fun completingWithoutAPendingLoginFailsAndStaysLoggedOut() = runTest {
        val r = repo()
        val result = r.completeLogin("code")
        assertIs<Failure.Auth>((result as EzResult.Error).failure)
        assertEquals(SessionState.LoggedOut(), r.state.value)
    }

    @Test
    fun theCodeCanOnlyBeRedeemedOnce() = runTest {
        val r = repo()
        r.startLogin()
        r.completeLogin("code")
        assertIs<EzResult.Error>(r.completeLogin("code"))
    }

    @Test
    fun cancellingLoginDiscardsThePendingPkce() = runTest {
        val r = repo()
        r.startLogin()
        r.cancelLogin()
        assertIs<EzResult.Error>(r.completeLogin("code"))
        assertEquals(SessionState.LoggedOut(), r.state.value)
    }

    @Test
    fun aRejectedExchangeKeepsTheUserLoggedOut() = runTest {
        val r = repo(client = idp(HttpStatusCode.BadRequest, """{"error":"invalid_grant"}"""))
        r.startLogin()
        assertIs<EzResult.Error>(r.completeLogin("bad"))
        assertEquals(SessionState.LoggedOut(), r.state.value)
    }

    @Test
    fun logoutClearsTokensAndState() {
        val store = TokenStore(MemoryTokens()).withSession()
        val r = repo(store = store)
        r.logout()
        assertEquals(SessionState.LoggedOut(), r.state.value)
        assertNull(store.accessToken())
        assertNull(r.username())
    }

    @Test
    fun refreshRejectedByTheServerExpiresTheSession() = runTest {
        val store = TokenStore(MemoryTokens()).withSession()
        val r = repo(client = idp(HttpStatusCode.BadRequest, """{"error":"invalid_grant"}"""), store = store)

        assertIs<EzResult.Error>(r.refresh())
        assertEquals(SessionState.LoggedOut(expired = true), r.state.value)
        assertNull(store.accessToken())
    }

    @Test
    fun refreshWithoutRefreshTokenExpiresTheSession() = runTest {
        val r = repo(store = TokenStore(MemoryTokens()).withSession(refresh = null))
        assertIs<EzResult.Error>(r.refresh())
        assertEquals(SessionState.LoggedOut(expired = true), r.state.value)
    }

    @Test
    fun aNetworkFailureDuringRefreshDoesNotEndTheSession() = runTest {
        val failing = HttpClient(MockEngine { throw RuntimeException("sin red") })
        val store = TokenStore(MemoryTokens()).withSession()
        val r = repo(client = failing, store = store)

        assertIs<Failure.Network>((r.refresh() as EzResult.Error).failure)
        assertEquals(SessionState.LoggedIn, r.state.value)
        assertEquals("at", store.accessToken())
    }

    @Test
    fun successfulRefreshReplacesTokensAndKeepsTheSession() = runTest {
        val store = TokenStore(MemoryTokens()).withSession()
        val r = repo(store = store)
        assertIs<EzResult.Success<Unit>>(r.refresh())
        assertEquals(SessionState.LoggedIn, r.state.value)
        assertEquals("new-access", store.accessToken())
    }
}
