package org.andrew.enzona.feature.transfer.domain

import org.andrew.enzona.core.result.Failure

/**
 * Destinatario de una transferencia a una persona, VERIFICADO por el servidor: el `accountUuid` y el
 * `username` que se enviarán salen del MISMO objeto que se le muestra al usuario (nombre y avatar), nunca
 * de lo que tecleó. Es lo que evita mostrar un destinatario y enviar a otro.
 *
 * `accountUuid` es el uuid de la CUENTA del destinatario: es lo que la app original manda como
 * `funding_recipient_uuid` en las transferencias a una cuenta y a un amigo (decodificado, sin validar en vivo).
 */
data class TransferRecipient(
    val accountUuid: String,
    val username: String,
    val displayName: String,
    val avatar: String?,
)

/** Resultado de buscar una cuenta por su nombre de usuario. */
sealed interface FindRecipientResult {
    data class Found(val recipient: TransferRecipient) : FindRecipientResult

    /** El servidor no conoce esa cuenta (o devolvió otra distinta de la pedida: no se confía en ella). */
    data object NotFound : FindRecipientResult

    /** La búsqueda no se llegó a hacer. */
    data class Invalid(val reason: InvalidQuery) : FindRecipientResult

    data class Failed(val failure: Failure) : FindRecipientResult
}

enum class InvalidQuery {
    EMPTY,
    HAS_WHITESPACE,

    /** Es el propio usuario: para mover dinero entre sus tarjetas existe "A una de mis tarjetas". */
    YOURSELF,
}
