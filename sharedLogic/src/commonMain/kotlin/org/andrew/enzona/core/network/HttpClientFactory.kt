package org.andrew.enzona.core.network

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpSend
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.plugin
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import org.andrew.enzona.core.EnzonaConfig
import org.andrew.enzona.core.json.enzonaJson

/** Motor HTTP por plataforma (OkHttp en Android, Darwin en iOS). */
expect fun httpClientEngine(): HttpClientEngine

/** Quién aporta (y renueva) el token que se envía a api.enzona.net. */
interface TokenProvider {
    fun accessToken(): String?

    /**
     * Llamado tras un 401. [failedToken] es el token que se envió. Devuelve true si
     * hay un token nuevo con el que reintentar (renovado aquí o por otra petición).
     */
    suspend fun refreshAfterUnauthorized(failedToken: String?): Boolean
}

private fun HttpClientConfig<*>.installBase() {
    expectSuccess = false
    install(ContentNegotiation) { json(enzonaJson) }
    install(RequestLogging)
}

/**
 * Cliente SIN auth: para hablar con identity.enzona.net (token/refresh).
 * Debe ser distinto del cliente de API para que un refresh nunca dispare otro refresh.
 */
fun createPlainHttpClient(engine: HttpClientEngine = httpClientEngine()): HttpClient =
    HttpClient(engine) { installBase() }

/**
 * Cliente de api.enzona.net: añade `Authorization: Bearer` leyendo el token en cada
 * petición (sin caché propia, así un login/logout nuevo se refleja al instante) y,
 * ante un 401, renueva una vez y reintenta.
 */
fun createApiHttpClient(
    tokens: TokenProvider,
    engine: HttpClientEngine = httpClientEngine(),
): HttpClient {
    val client = HttpClient(engine) {
        installBase()
        defaultRequest {
            url(EnzonaConfig.API_BASE)
            contentType(ContentType.Application.Json)
        }
    }
    val apiHost = Url(EnzonaConfig.API_BASE).host

    client.plugin(HttpSend).intercept { request ->
        // El token solo viaja al host de la API, nunca a otros destinos.
        if (request.url.host != apiHost) return@intercept execute(request)

        val sent = tokens.accessToken()
        sent?.let { request.headers[HttpHeaders.Authorization] = "Bearer $it" }
        var call = execute(request)

        if (call.response.status == HttpStatusCode.Unauthorized &&
            tokens.refreshAfterUnauthorized(sent)
        ) {
            tokens.accessToken()?.let { request.headers[HttpHeaders.Authorization] = "Bearer $it" }
            call = execute(request)
        }
        call
    }
    return client
}
