package org.andrew.enzona.feature.transfer.domain

import org.andrew.enzona.core.result.EzResult
import org.andrew.enzona.core.result.Failure
import org.andrew.enzona.feature.account.domain.AccountRepository
import org.andrew.enzona.feature.auth.domain.SessionRepository

/**
 * "Buscar una cuenta": consulta la cuenta de otro usuario (la misma consulta del perfil, ahora con el nombre
 * buscado) y la convierte en un [TransferRecipient].
 *
 * Es estricta a propósito, porque de aquí sale a quién se envía el dinero:
 * - solo se acepta lo que el servidor devuelve para ESE nombre de usuario (si devuelve otra cuenta, no se usa);
 * - una cuenta sin uuid no se puede usar para transferir;
 * - no se puede buscar a uno mismo.
 */
class FindRecipientUseCase(
    private val accounts: AccountRepository,
    private val session: SessionRepository,
) {
    suspend operator fun invoke(rawQuery: String): FindRecipientResult {
        val query = rawQuery.trim()
        if (query.isEmpty()) return FindRecipientResult.Invalid(InvalidQuery.EMPTY)
        if (query.any { it.isWhitespace() }) return FindRecipientResult.Invalid(InvalidQuery.HAS_WHITESPACE)
        if (query.equals(session.username(), ignoreCase = true)) {
            return FindRecipientResult.Invalid(InvalidQuery.YOURSELF)
        }
        return when (val r = accounts.getAccount(query)) {
            is EzResult.Error ->
                // 404 = esa cuenta no existe; cualquier otro fallo (red, sesión...) se informa tal cual.
                if ((r.failure as? Failure.Http)?.status == 404) FindRecipientResult.NotFound
                else FindRecipientResult.Failed(r.failure)
            is EzResult.Success -> {
                val account = r.value
                when {
                    !account.username.equals(query, ignoreCase = true) -> FindRecipientResult.NotFound
                    account.uuid.isBlank() ->
                        FindRecipientResult.Failed(Failure.Parse("La cuenta encontrada no trae identificador."))
                    else -> FindRecipientResult.Found(
                        TransferRecipient(
                            account.uuid, account.username, account.fullName, account.avatarUrl, account.primaryCurrency,
                        ),
                    )
                }
            }
        }
    }
}
