package org.andrew.enzona.feature.friends.domain

import org.andrew.enzona.core.result.EzResult
import org.andrew.enzona.feature.transfer.domain.TransferRecipient

/** Un amigo (contacto) del usuario. */
data class Friend(
    val uuid: String,
    val username: String,
    val name: String?,
    val avatar: String?,
    val phone: String?,
    val primaryCurrency: String?,
) {
    val displayName: String get() = name ?: username

    /**
     * El destinatario de "A un amigo". Su `uuid` es el que la app original manda como `funding_recipient_uuid`
     * (inferido: no se ha visto una respuesta real de `contacts`; ver `docs/enzona-kmp/06`).
     */
    fun asRecipient(): TransferRecipient = TransferRecipient(uuid, username, displayName, avatar, primaryCurrency)
}

interface FriendsRepository {
    /** Los amigos de la cuenta [accountUuid] (la del propio usuario). */
    suspend fun list(accountUuid: String): EzResult<List<Friend>>
}
