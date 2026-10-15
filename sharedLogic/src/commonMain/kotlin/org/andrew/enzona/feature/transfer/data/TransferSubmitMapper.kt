package org.andrew.enzona.feature.transfer.data

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.andrew.enzona.core.json.asCleanText
import org.andrew.enzona.core.json.asObjectOrNull
import org.andrew.enzona.core.json.hasFault
import org.andrew.enzona.core.json.text
import org.andrew.enzona.core.money.toDecimalText
import org.andrew.enzona.feature.transfer.domain.RejectionCause
import org.andrew.enzona.feature.transfer.domain.TransferOutcome
import org.andrew.enzona.feature.transfer.domain.TransferReceipt
import org.andrew.enzona.feature.transfer.domain.TransferRequest
import org.andrew.enzona.feature.transfer.domain.TransferStatus
import org.andrew.enzona.feature.transfer.domain.UnknownCause

/** `POST` que usa la app original tanto para "A una cuenta" y "A un amigo" como para "A una de mis tarjetas". */
internal const val TRANSFER_ACCOUNT_PATH = "/transfer/v1.0.0/transfer/account"

/**
 * Cuerpo de la petición, con los mismos campos que arma la app original (decodificado, sin validar en vivo):
 * - Siempre: `amount` (texto decimal), `funding_source_uuid`, `description`, `currency` (la de la tarjeta de origen),
 *   `funding_recipient_uuid`, `payment_password` (cifrada) y `fingerprint` (cadena vacía: el original manda las dos
 *   claves y solo rellena la que usa).
 * - Solo a una cuenta o amigo: además `funding_recipient_username` y `type_transfer` (siempre cadena vacía).
 * - A una de mis tarjetas, `funding_recipient_uuid` es el uuid de la tarjeta destino; a una cuenta o amigo, el de la
 *   cuenta del destinatario.
 */
internal fun TransferRequest.toBody(encryptedPassword: String): JsonObject = buildJsonObject {
    put("amount", amount.toDecimalText())
    put("funding_source_uuid", sourceCardId)
    put("description", description.trim())
    put("currency", amount.currency.trim())
    when (this@toBody) {
        is TransferRequest.ToOwnCard -> put("funding_recipient_uuid", destinationCardId)
        is TransferRequest.ToAccount -> {
            put("funding_recipient_uuid", recipient.accountUuid)
            put("funding_recipient_username", recipient.username)
            put("type_transfer", "")
        }
    }
    put("payment_password", encryptedPassword)
    put("fingerprint", "")
}

/**
 * Traduce la respuesta HTTP al desenlace. [body] es `null` si no se pudo leer como objeto JSON.
 *
 * La regla de fondo: solo se dice "no se movió" cuando el servidor lo dijo (un 4xx claro, un `fault`, estado fallida);
 * solo se dice "aceptada" cuando hay un 2xx con datos de transferencia reconocibles; todo lo demás es desconocido.
 * Los 5xx son desconocidos a propósito: el servidor pudo procesar el pago y fallar al responder.
 */
internal fun mapSubmitResponse(status: Int, body: JsonObject?): TransferOutcome = when (status) {
    in 200..299 -> mapSuccessStatus(status, body)
    401 -> TransferOutcome.Rejected(RejectionCause.SESSION_EXPIRED, status, null)
    408 -> TransferOutcome.Unknown(UnknownCause.TIMEOUT, status)
    in 400..499 -> TransferOutcome.Rejected(RejectionCause.REFUSED, status, body?.get("fault").faultMessage())
    in 500..599 -> TransferOutcome.Unknown(UnknownCause.SERVER_ERROR, status)
    else -> TransferOutcome.Unknown(UnknownCause.UNEXPECTED_STATUS, status)
}

private fun mapSuccessStatus(httpStatus: Int, body: JsonObject?): TransferOutcome {
    if (body == null) return TransferOutcome.Unknown(UnknownCause.UNREADABLE_RESPONSE, httpStatus)
    if (body["fault"].hasFault()) {
        return TransferOutcome.Rejected(RejectionCause.FAILED_BY_SERVER, httpStatus, body["fault"].faultMessage())
    }
    val statusText = body.text("status_denom")
    val status = statusOf(body.text("status_code"), statusText)
    if (status == TransferStatus.FAILED) {
        return TransferOutcome.Rejected(RejectionCause.FAILED_BY_SERVER, httpStatus, null)
    }
    val operationCode = body.text("transaction_code") ?: body.text("id_transfer")
    // Sin ningún dato reconocible de una transferencia no se puede afirmar que se hizo.
    val recognizable = operationCode != null || statusText != null || status != TransferStatus.OTHER ||
        body.text("transaction_uuid") != null || body.text("uuid") != null
    if (!recognizable) return TransferOutcome.Unknown(UnknownCause.UNRECOGNIZED_RESPONSE, httpStatus)
    return TransferOutcome.Accepted(
        TransferReceipt(
            operationCode = operationCode, status = status, statusText = statusText,
            createdAtText = body.text("created_at"),
        ),
    )
}

/**
 * Texto del `fault` si lo trae. Su forma real no se ha visto: puede ser un texto o un objeto, así que se prueban las
 * claves habituales y, si no hay texto utilizable, se devuelve `null` (la UI muestra un mensaje propio).
 */
internal fun JsonElement?.faultMessage(): String? {
    if (this is JsonPrimitive) return asCleanText()?.takeUnless { it.equals("true", ignoreCase = true) }
    val fault = asObjectOrNull() ?: return null
    return listOf("message", "description", "faultstring", "detail", "error_description")
        .firstNotNullOfOrNull { fault.text(it) }
}
