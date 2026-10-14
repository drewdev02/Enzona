package org.andrew.enzona.feature.auth

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okio.ByteString.Companion.encodeUtf8
import org.andrew.enzona.core.logging.LogLevel
import org.andrew.enzona.core.logging.withLogging
import org.andrew.enzona.core.network.RequestLogging
import org.andrew.enzona.core.result.EzResult
import org.andrew.enzona.core.result.Failure
import org.andrew.enzona.core.storage.SecureStorage
import org.andrew.enzona.feature.auth.data.OAuthService
import org.andrew.enzona.feature.auth.data.TokenStore
import org.andrew.enzona.feature.auth.domain.SessionRepository
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

private class Vault : SecureStorage {
    private val map = mutableMapOf<String, String>()
    override fun put(key: String, value: String) { map[key] = value }
    override fun get(key: String) = map[key]
    override fun remove(key: String) { map.remove(key) }
    override fun clear() = map.clear()
}

private const val CODE = "CODE-SECRET-123"
private const val ACCESS = "ACCESS-SECRET-aaa"
private const val REFRESH = "REFRESH-SECRET-bbb"
private const val USERNAME = "usuario-secreto"

/**
 * El test que protege de verdad: recorre login, uso de la sesión, refresh correcto y refresh
 * rechazado con el nivel de log MÁS verboso, y comprueba que ningún secreto aparece en nada
 * que un destino real pudiera imprimir (mensaje, plantilla, propiedades, contexto, excepción).
 */
class SessionLogRedactionTest {
    private val idToken = "x." + """{"sub":"$USERNAME"}""".encodeUtf8().base64Url().trimEnd('=') + ".y"

    private fun tokenJson() =
        """{"access_token":"$ACCESS","refresh_token":"$REFRESH","token_type":"Bearer","expires_in":3600,"id_token":"$idToken"}"""

    @Test
    fun noSecretReachesTheLogsAcrossLoginAndRefresh() = runTest {
        var capturedVerifier: String? = null
        var failNextRefresh = false
        val engine = MockEngine { req ->
            val form = (req.body as? OutgoingContent.ByteArrayContent)?.bytes()?.decodeToString().orEmpty()
            Regex("""code_verifier=([^&]+)""").find(form)?.let { capturedVerifier = it.groupValues[1] }
            if (failNextRefresh) {
                // Un IdP que repite la solicitud en su error: lo peor que podría pasar.
                respond(
                    """{"error":"invalid_grant","error_description":"code $CODE refresh $REFRESH expired"}""",
                    HttpStatusCode.BadRequest,
                    headersOf(HttpHeaders.ContentType, "application/json"),
                )
            } else {
                respond(tokenJson(), HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
            }
        }
        val idp = HttpClient(engine) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
            install(RequestLogging)
        }

        withLogging(minimumLevel = LogLevel.TRACE) { sink ->
            val session = SessionRepository(OAuthService(idp), TokenStore(Vault()))

            val authorizeUrl = session.startLogin()
            assertIs<EzResult.Success<Unit>>(session.completeLogin(CODE))
            assertTrue(session.username() == USERNAME) // se usa el dato, pero no se registra
            assertIs<EzResult.Success<Unit>>(session.refresh())

            failNextRefresh = true
            val rejected = session.refresh()
            assertIs<EzResult.Error>(rejected)
            val message = (rejected.failure as Failure.Auth).message.orEmpty()
            assertFalse(CODE in message || REFRESH in message, "el mensaje de fallo que ve la UI no debe repetir el cuerpo del IdP")

            val written = sink.everythingWritten()
            assertTrue("Sesión abierta" in written, "el test debe estar observando logs reales")

            val verifier = checkNotNull(capturedVerifier) { "el canje no envió code_verifier" }
            val challenge = Regex("""code_challenge=([^&]+)""").find(authorizeUrl)!!.groupValues[1]
            for ((what, secret) in mapOf(
                "code" to CODE, "access_token" to ACCESS, "refresh_token" to REFRESH,
                "id_token" to idToken, "username" to USERNAME,
                "code_verifier" to verifier, "code_challenge" to challenge,
            )) {
                assertFalse(secret in written, "$what apareció en los logs")
            }
        }
    }
}
