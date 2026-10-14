package org.andrew.enzona.core.network

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import org.andrew.enzona.core.logging.LogLevel
import org.andrew.enzona.core.logging.withLogging
import org.andrew.enzona.core.result.EzResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private const val SECRET_TOKEN = "SECRET-TOKEN-eyJhbGciOi"

private object Provider : TokenProvider {
    override fun accessToken() = SECRET_TOKEN
    override suspend fun refreshAfterUnauthorized(failedToken: String?) = false
}

class NetworkLoggingTest {
    private fun client(status: HttpStatusCode = HttpStatusCode.OK) =
        createApiHttpClient(Provider, MockEngine {
            respond("{}", status, headersOf(HttpHeaders.ContentType, "application/json"))
        })

    private val path = "/coremz/1.0.2/card/username/ana-la-del-banco"

    @Test
    fun infoShowsMethodHostStatusAndDurationButNeitherThePathNorTheToken() = runTest {
        withLogging(minimumLevel = LogLevel.INFO) { sink ->
            client().get("https://api.enzona.net$path?code=QUERY-SECRET")

            val line = sink.events.single()
            assertEquals(LogLevel.INFO, line.level)
            assertTrue(Regex("""GET api\.enzona\.net → 200 en \d+ ms""").matches(line.message), line.message)

            val all = sink.everythingWritten()
            assertFalse("ana-la-del-banco" in all, "el username de la ruta no debe salir en Info")
            assertFalse("QUERY-SECRET" in all, "la query nunca se registra")
            assertFalse(SECRET_TOKEN in all, "el token nunca se registra")
            assertFalse("Bearer" in all, "las cabeceras nunca se registran")
        }
    }

    @Test
    fun debugAddsThePathButStillNeverTheQueryOrTheToken() = runTest {
        withLogging(minimumLevel = LogLevel.DEBUG) { sink ->
            client().get("https://api.enzona.net$path?code=QUERY-SECRET")

            val all = sink.everythingWritten()
            assertTrue(path in all, "en Debug se ve la ruta")
            assertFalse("QUERY-SECRET" in all)
            assertFalse(SECRET_TOKEN in all)
            assertFalse("Bearer" in all)
        }
    }

    @Test
    fun theRequestAndItsResultShareARequestId() = runTest {
        withLogging(minimumLevel = LogLevel.DEBUG) { sink ->
            client().get("https://api.enzona.net$path")

            assertEquals(2, sink.events.size)
            val ids = sink.events.map { it.context["requestId"] }
            assertTrue(ids[0] != null && ids[0] == ids[1])
        }
    }

    @Test
    fun clientAndServerErrorsAreWarnAndError() = runTest {
        withLogging { sink ->
            client(HttpStatusCode.NotFound).get("https://api.enzona.net/x")
            client(HttpStatusCode.BadGateway).get("https://api.enzona.net/x")
            assertEquals(
                listOf(LogLevel.WARN, LogLevel.ERROR),
                sink.events.filter { it.level >= LogLevel.WARN }.map { it.level },
            )
        }
    }

    @Test
    fun apiCallLogsTheExceptionTypeAtWarnAndItsMessageOnlyAtDebug() = runTest {
        val leakyMessage = "Unable to resolve https://api.enzona.net$path"

        withLogging(minimumLevel = LogLevel.INFO) { sink ->
            val r = apiCall<Unit> { throw IllegalStateException(leakyMessage) }
            assertTrue(r is EzResult.Error)
            assertTrue(sink.events.single().message.contains("IllegalStateException"))
            assertFalse("ana-la-del-banco" in sink.everythingWritten())
        }
        withLogging(minimumLevel = LogLevel.DEBUG) { sink ->
            apiCall<Unit> { throw IllegalStateException(leakyMessage) }
            assertTrue("ana-la-del-banco" in sink.everythingWritten())
        }
    }
}
