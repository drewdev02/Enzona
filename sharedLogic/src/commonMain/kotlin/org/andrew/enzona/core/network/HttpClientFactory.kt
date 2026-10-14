package org.andrew.enzona.core.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import org.andrew.enzona.core.EnzonaConfig

/** Motor HTTP por plataforma (OkHttp en Android, Darwin en iOS). */
expect fun httpClientEngine(): HttpClientEngine

/** Cliente Ktor base para api.enzona.net. El plugin de auth (Bearer+refresh) se añade en M1. */
fun createHttpClient(engine: HttpClientEngine = httpClientEngine()): HttpClient =
    HttpClient(engine) {
        expectSuccess = false
        install(ContentNegotiation) { json(enzonaJson) }
        install(Logging) { level = LogLevel.INFO }
        defaultRequest {
            url(EnzonaConfig.API_BASE)
            contentType(ContentType.Application.Json)
        }
    }
