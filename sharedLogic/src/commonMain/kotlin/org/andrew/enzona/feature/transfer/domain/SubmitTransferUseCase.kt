package org.andrew.enzona.feature.transfer.domain

import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import org.andrew.enzona.core.crypto.PaymentPasswordEncryptor
import org.andrew.enzona.core.logging.logger
import org.andrew.enzona.core.result.EzResult

/**
 * Valida, protege la contraseña y envía UNA transferencia.
 *
 * - Una sola a la vez en toda la app (un `Mutex` por instancia, y esta es única): un segundo intento mientras hay uno
 *   en vuelo no se envía, para que un doble toque o dos pantallas nunca dupliquen un pago.
 * - Una vez enviada la petición, se espera su respuesta aunque la pantalla se cierre ([NonCancellable]): cancelarla a
 *   medias dejaría un resultado desconocido sin necesidad.
 * - Nunca reintenta. Qué hacer ante [TransferOutcome.Unknown] lo decide quien llama, mirando el historial.
 */
class SubmitTransferUseCase(
    private val gateway: TransferGateway,
    private val encryptor: PaymentPasswordEncryptor,
) {
    private val log = logger<SubmitTransferUseCase>()
    private val inFlight = Mutex()

    suspend operator fun invoke(request: TransferRequest, password: PaymentPassword): TransferOutcome {
        request.notSentReason()?.let { return notSent(it) }
        if (password.value.isEmpty()) return notSent(NotSentReason.PASSWORD_EMPTY)
        if (!inFlight.tryLock()) return notSent(NotSentReason.ANOTHER_TRANSFER_IN_PROGRESS)
        try {
            val encrypted = when (val result = encryptor.encrypt(password.value)) {
                is EzResult.Success -> result.value
                is EzResult.Error -> return notSent(NotSentReason.PASSWORD_UNUSABLE)
            }
            val outcome = withContext(NonCancellable) { gateway.submit(request, encrypted) }
            // Solo el tipo de desenlace: nunca importes, tarjetas, nombres ni códigos de operación.
            log.info("Transferencia: {Desenlace}", outcome::class.simpleName)
            return outcome
        } finally {
            inFlight.unlock()
        }
    }

    private fun notSent(reason: NotSentReason): TransferOutcome.NotSent {
        log.info("Transferencia no enviada: {Motivo}", reason.name)
        return TransferOutcome.NotSent(reason)
    }
}
