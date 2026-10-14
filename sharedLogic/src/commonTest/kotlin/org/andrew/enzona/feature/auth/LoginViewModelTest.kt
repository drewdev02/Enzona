package org.andrew.enzona.feature.auth

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.Json
import org.andrew.enzona.core.storage.SecureStorage
import org.andrew.enzona.feature.auth.data.OAuthService
import org.andrew.enzona.feature.auth.data.TokenStore
import org.andrew.enzona.feature.auth.domain.SessionRepository
import org.andrew.enzona.feature.auth.domain.SessionState
import org.andrew.enzona.feature.auth.presentation.LoginUiState
import org.andrew.enzona.feature.auth.presentation.LoginViewModel
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

private class InMemoryTokens : SecureStorage {
    private val map = mutableMapOf<String, String>()
    override fun put(key: String, value: String) { map[key] = value }
    override fun get(key: String) = map[key]
    override fun remove(key: String) { map.remove(key) }
    override fun clear() = map.clear()
}

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {
    @BeforeTest fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @AfterTest fun tearDown() = Dispatchers.resetMain()

    private fun session(status: HttpStatusCode = HttpStatusCode.OK): SessionRepository {
        val idp = HttpClient(MockEngine {
            respond(
                if (status == HttpStatusCode.OK) """{"access_token":"at","refresh_token":"rt"}""" else """{"error":"x"}""",
                status,
                headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }) { install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) } }
        return SessionRepository(OAuthService(idp), TokenStore(InMemoryTokens()))
    }

    @Test
    fun startsIdleAndShowsAuthenticatingOnceTheBrowserIsOpened() {
        val vm = LoginViewModel(session())
        assertEquals(LoginUiState.Idle, vm.state.value)
        vm.buildAuthorizeUrl()
        assertEquals(LoginUiState.Authenticating, vm.state.value)
    }

    @Test
    fun aSuccessfulRedirectOpensTheSessionAndReturnsToIdle() = runTest {
        val s = session()
        val vm = LoginViewModel(s)
        vm.buildAuthorizeUrl()
        vm.onRedirectCode("code")
        // El canje de red corre en otro hilo: se espera al estado resultante.
        assertEquals(LoginUiState.Idle, vm.state.first { it !is LoginUiState.Authenticating })
        assertEquals(SessionState.LoggedIn, s.state.value)
    }

    @Test
    fun aRejectedExchangeShowsAnError() = runTest {
        val s = session(HttpStatusCode.BadRequest)
        val vm = LoginViewModel(s)
        vm.buildAuthorizeUrl()
        vm.onRedirectCode("bad")
        assertIs<LoginUiState.Error>(vm.state.first { it !is LoginUiState.Authenticating })
        assertEquals(SessionState.LoggedOut(), s.state.value)
    }

    @Test
    fun cancellingFromTheBrowserDiscardsThePkceAndShowsTheMessage() {
        val s = session()
        val vm = LoginViewModel(s)
        vm.buildAuthorizeUrl()
        vm.onAuthFailed("Login cancelado")
        assertEquals(LoginUiState.Error("Login cancelado"), vm.state.value)

        // Un code que llegue después ya no tiene PKCE con el que canjearse.
        vm.onRedirectCode("late")
        assertIs<LoginUiState.Error>(vm.state.value)
        assertEquals(SessionState.LoggedOut(), s.state.value)
    }

    @Test
    fun aRecreatedScreenCanStillCompleteALoginStartedByThePreviousOne() = runTest {
        val s = session()
        LoginViewModel(s).buildAuthorizeUrl() // la pantalla se destruye con el navegador abierto
        val recreated = LoginViewModel(s)
        recreated.onRedirectCode("code")
        assertEquals(SessionState.LoggedIn, s.state.first { it is SessionState.LoggedIn })
    }
}
