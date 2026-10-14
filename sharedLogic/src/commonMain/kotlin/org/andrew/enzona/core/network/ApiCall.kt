package org.andrew.enzona.core.network

import io.ktor.serialization.ContentConvertException
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import org.andrew.enzona.core.logging.LogLevel
import org.andrew.enzona.core.logging.logger
import org.andrew.enzona.core.result.EzResult
import org.andrew.enzona.core.result.Failure

private val log = logger("Network")

/**
 * Ejecuta una llamada y traduce las excepciones a [Failure]:
 * - respuesta que no se puede deserializar -> Parse
 * - cualquier otro fallo de E/S -> Network
 * La cancelación de la corrutina se RELANZA: no es un error de red.
 *
 * Es el punto único donde se registran los fallos sin respuesta: en Warn solo el TIPO de la
 * excepción; su mensaje (que en Ktor puede incluir la URL, y con ella el username) solo en Debug.
 */
suspend fun <T> apiCall(block: suspend () -> EzResult<T>): EzResult<T> = try {
    block()
} catch (e: CancellationException) {
    throw e
} catch (e: ContentConvertException) {
    logFailure("Respuesta ilegible", e)
    EzResult.Error(Failure.Parse(e.message))
} catch (e: SerializationException) {
    logFailure("Respuesta ilegible", e)
    EzResult.Error(Failure.Parse(e.message))
} catch (e: Exception) {
    logFailure("Fallo de red", e)
    EzResult.Error(Failure.Network(e.message))
}

private fun logFailure(what: String, e: Exception) {
    log.warn("{Que}: {Tipo}", what, e::class.simpleName)
    if (log.isEnabled(LogLevel.DEBUG)) log.debug("detalle: {Mensaje}", e.message)
}
