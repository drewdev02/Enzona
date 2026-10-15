package org.andrew.enzona.feature.transfer.domain

import kotlinx.datetime.LocalDateTime
import org.andrew.enzona.core.money.Money

/** Contraseña de pago en memoria, solo hasta cifrarla. `toString` no la revela, así que un log o un `data class` no la filtran. */
class PaymentPassword(val value: String) {
    override fun toString(): String = "PaymentPassword(•••)"
}

/**
 * Lo que el usuario quiere transferir. [amount] es la magnitud (positiva) en la moneda de la tarjeta de origen: es la
 * moneda que la app original envía como `currency`, aunque el destino use otra (el servidor aplica el cambio).
 * Ninguna variante lleva la contraseña: viaja aparte y cifrada.
 */
sealed interface TransferRequest {
    val sourceCardId: String
    val amount: Money
    val description: String

    /** "A una de mis tarjetas": el destino es otra tarjeta del propio usuario. */
    data class ToOwnCard(
        override val sourceCardId: String,
        val destinationCardId: String,
        override val amount: Money,
        override val description: String,
    ) : TransferRequest

    /** "A una cuenta" y "A un amigo": el destinatario es el que verificó el servidor ([TransferRecipient]). */
    data class ToAccount(
        override val sourceCardId: String,
        val recipient: TransferRecipient,
        override val amount: Money,
        override val description: String,
    ) : TransferRequest
}

/** Por qué una transferencia NO se envió. Nada salió del teléfono: es seguro corregir y volver a intentar. */
enum class NotSentReason {
    NON_POSITIVE_AMOUNT,
    MISSING_CURRENCY,
    MISSING_SOURCE_CARD,
    MISSING_DESTINATION,
    SAME_CARD,
    PASSWORD_EMPTY,
    PASSWORD_UNUSABLE,

    /** Ya hay una transferencia en curso: dos a la vez podrían duplicar un pago. */
    ANOTHER_TRANSFER_IN_PROGRESS,
}

/** Lo que el servidor dio a entender de una transferencia que aceptó. Hechos tal cual; sin formato de pantalla. */
data class TransferReceipt(
    /** `transaction_code` (el "No. Operación" del original), o `id_transfer` si no vino. */
    val operationCode: String?,
    val status: TransferStatus,
    val statusText: String?,
    /** Hora de reloj tal cual la dio el servidor (sin convertir de zona), como en el historial. */
    val createdAt: LocalDateTime?,
    val createdAtText: String?,
    /** `transaction_signature`: la emite el servidor; la app original la muestra como QR del comprobante. */
    val signature: String?,
)

enum class RejectionCause {
    /** 401 tras intentar renovar la sesión: el servidor no llegó a procesar nada. */
    SESSION_EXPIRED,

    /** Otro 4xx: el servidor entendió la petición y la rechazó. */
    REFUSED,

    /** 2xx con `fault`, o con estado "fallida": el servidor la procesó y falló. */
    FAILED_BY_SERVER,
}

enum class UnknownCause {
    /** La petición salió y no hubo respuesta (sin red, corte, tiempo agotado en el cliente). */
    NO_RESPONSE,

    /** Hubo respuesta pero no se pudo leer. */
    UNREADABLE_RESPONSE,

    /** 2xx sin ningún dato reconocible de transferencia: no se puede afirmar que se hizo. */
    UNRECOGNIZED_RESPONSE,

    /** 5xx: el servidor pudo haber procesado la transferencia antes de fallar. */
    SERVER_ERROR,

    /** 408: tiempo agotado en el camino; no se sabe hasta dónde llegó. */
    TIMEOUT,

    /** Cualquier otro código que no es éxito ni rechazo claro. */
    UNEXPECTED_STATUS,
}

/**
 * Desenlace de intentar una transferencia. Solo [Rejected] y [NotSent] garantizan que el dinero NO se movió. En
 * [Unknown] puede haberse movido: hay que mirar el historial ANTES de volver a enviar, y nunca reintentar solo.
 */
sealed interface TransferOutcome {
    /** No se envió nada. */
    data class NotSent(val reason: NotSentReason) : TransferOutcome

    /** El servidor la aceptó (puede quedar pendiente: ver [TransferReceipt.status]). */
    data class Accepted(val receipt: TransferReceipt) : TransferOutcome

    /**
     * El servidor la rechazó: el dinero no se movió. [serverMessage] es el texto del servidor, si lo trajo; es un dato
     * del usuario y no se registra.
     */
    data class Rejected(val cause: RejectionCause, val httpStatus: Int?, val serverMessage: String?) : TransferOutcome

    /** No se sabe si se hizo. */
    data class Unknown(val cause: UnknownCause, val httpStatus: Int? = null) : TransferOutcome
}

/** Envía una transferencia ya validada, con la contraseña ya cifrada. Un solo intento: nunca reintenta. */
interface TransferGateway {
    suspend fun submit(request: TransferRequest, encryptedPassword: String): TransferOutcome
}

/** Lo que impide enviar [this], o null si está en regla. No decide límites de importe: eso es del servidor. */
fun TransferRequest.notSentReason(): NotSentReason? = when {
    amount.minorUnits <= 0 -> NotSentReason.NON_POSITIVE_AMOUNT
    amount.currency.isBlank() -> NotSentReason.MISSING_CURRENCY
    sourceCardId.isBlank() -> NotSentReason.MISSING_SOURCE_CARD
    this is TransferRequest.ToOwnCard && destinationCardId.isBlank() -> NotSentReason.MISSING_DESTINATION
    this is TransferRequest.ToOwnCard && destinationCardId == sourceCardId -> NotSentReason.SAME_CARD
    this is TransferRequest.ToAccount && (recipient.accountUuid.isBlank() || recipient.username.isBlank()) ->
        NotSentReason.MISSING_DESTINATION
    else -> null
}
