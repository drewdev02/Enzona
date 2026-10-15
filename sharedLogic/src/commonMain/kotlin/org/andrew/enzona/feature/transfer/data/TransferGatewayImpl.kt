package org.andrew.enzona.feature.transfer.data

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.serialization.ContentConvertException
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonObject
import org.andrew.enzona.core.logging.logger
import org.andrew.enzona.core.network.apiCall
import org.andrew.enzona.core.result.EzResult
import org.andrew.enzona.core.result.Failure
import org.andrew.enzona.feature.transfer.domain.TransferGateway
import org.andrew.enzona.feature.transfer.domain.TransferOutcome
import org.andrew.enzona.feature.transfer.domain.TransferRequest
import org.andrew.enzona.feature.transfer.domain.UnknownCause

/**
 * Envía la transferencia con UN `POST` y traduce lo que ocurra a un [TransferOutcome]. El cliente de la API no
 * reintenta peticiones por errores de red (solo reenvía una vez tras un 401 con la sesión ya renovada, cuando el
 * servidor no llegó a procesar nada), así que un solo `submit` es a lo sumo un pago.
 */
class TransferGatewayImpl(private val client: HttpClient) : TransferGateway {
    private val log = logger<TransferGatewayImpl>()

    override suspend fun submit(request: TransferRequest, encryptedPassword: String): TransferOutcome {
        val result = apiCall {
            val response = client.post(TRANSFER_ACCOUNT_PATH) { setBody(request.toBody(encryptedPassword)) }
            // Un cuerpo ilegible solo importa si el servidor dijo "bien": en un 4xx el rechazo ya está claro.
            val body = try {
                response.body<JsonObject>()
            } catch (e: CancellationException) {
                throw e
            } catch (_: ContentConvertException) {
                null
            } catch (_: SerializationException) {
                null
            } catch (_: IllegalArgumentException) {
                null
            }
            EzResult.Success(mapSubmitResponse(response.status.value, body))
        }
        return when (result) {
            is EzResult.Success -> result.value
            // Con la petición ya enviada, un fallo (red cortada, lectura rota) no dice si el servidor la procesó.
            is EzResult.Error -> {
                log.warn("Transferencia sin respuesta utilizable")
                TransferOutcome.Unknown(
                    if (result.failure is Failure.Parse) UnknownCause.UNREADABLE_RESPONSE else UnknownCause.NO_RESPONSE,
                )
            }
        }
    }
}
