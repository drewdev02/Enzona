package org.andrew.enzona.core.network

import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.util.AttributeKey
import org.andrew.enzona.core.logging.LogLevel
import org.andrew.enzona.core.logging.Logger
import org.andrew.enzona.core.logging.logger
import kotlin.random.Random
import kotlin.time.TimeMark
import kotlin.time.TimeSource

private class RequestTrace(val log: Logger, val start: TimeMark)

private val TraceKey = AttributeKey<RequestTrace>("EnzonaRequestTrace")

/**
 * Registra cada petición HTTP en la categoría `Network`.
 *
 * - Info: `GET api.enzona.net → 200 en 143 ms` (método, host, estado, duración, requestId).
 * - Debug: añade la RUTA. La ruta puede llevar el username (`/card/username/{username}`), por eso
 *   no sale en Info.
 *
 * Nunca se registran cabeceras, cuerpos ni la query: llevarían tokens, códigos y contraseñas.
 * Los fallos sin respuesta (sin red, JSON ilegible) los registra [apiCall] / quien los captura.
 */
val RequestLogging = createClientPlugin("EnzonaRequestLogging") {
    val network = logger("Network")

    onRequest { request, _ ->
        val log = network.with("requestId" to Random.nextInt(0x1000000).toString(16).padStart(6, '0'))
        request.attributes.put(TraceKey, RequestTrace(log, TimeSource.Monotonic.markNow()))
        log.debug("→ {Method} {Path}", request.method.value, request.url.build().encodedPath)
    }

    onResponse { response ->
        val request = response.call.request
        val trace = request.attributes.getOrNull(TraceKey) ?: return@onResponse
        val status = response.status.value
        val level = when {
            status >= 500 -> LogLevel.ERROR
            status >= 400 -> LogLevel.WARN
            else -> LogLevel.INFO
        }
        trace.log.log(
            level, null, "{Method} {Host} → {Status} en {Ms} ms",
            request.method.value, request.url.host, status, trace.start.elapsedNow().inWholeMilliseconds,
        )    }
}
