package org.andrew.enzona.core.network

import io.ktor.serialization.ContentConvertException
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import org.andrew.enzona.core.result.EzResult
import org.andrew.enzona.core.result.Failure

/**
 * Ejecuta una llamada y traduce las excepciones a [Failure]:
 * - respuesta que no se puede deserializar -> Parse
 * - cualquier otro fallo de E/S -> Network
 * La cancelación de la corrutina se RELANZA: no es un error de red.
 */
suspend fun <T> apiCall(block: suspend () -> EzResult<T>): EzResult<T> = try {
    block()
} catch (e: CancellationException) {
    throw e
} catch (e: ContentConvertException) {
    EzResult.Error(Failure.Parse(e.message))
} catch (e: SerializationException) {
    EzResult.Error(Failure.Parse(e.message))
} catch (e: Exception) {
    EzResult.Error(Failure.Network(e.message))
}
