package org.andrew.enzona.feature.transfer.data

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.json.JsonObject
import org.andrew.enzona.core.json.asObjectOrNull
import org.andrew.enzona.core.json.text
import org.andrew.enzona.core.money.Money
import org.andrew.enzona.core.money.parseMinorUnits
import org.andrew.enzona.feature.transfer.domain.TransferDirection
import org.andrew.enzona.feature.transfer.domain.TransferParty
import org.andrew.enzona.feature.transfer.domain.TransferStatus
import org.andrew.enzona.feature.transfer.domain.TransferSummary

/**
 * Una fila de `GET /transfer/v1.0.0/transfer` al dominio, o `null` si no se puede leer (sin importe legible):
 * quien llama la cuenta como omitida en vez de inventar un importe.
 *
 * El importe llega como TEXTO CON SIGNO (`-1400.00` al enviar, `+500.00` al recibir): el signo da el sentido
 * y el dominio guarda la magnitud. Todo es laxo: cualquier campo puede faltar o venir como `false`.
 */
internal fun JsonObject.toTransferSummary(fallbackId: String): TransferSummary? {
    val amountText = text("amount") ?: return null
    val signed = parseMinorUnits(amountText) ?: return null
    // La magnitud de Long.MIN_VALUE no cabe en un Long: se trata como ilegible en vez de desbordar.
    if (signed == Long.MIN_VALUE) return null
    val direction = when {
        amountText.startsWith("-") -> TransferDirection.OUTGOING
        amountText.startsWith("+") -> TransferDirection.INCOMING
        else -> TransferDirection.UNKNOWN
    }
    val statusText = text("status_denom")
    val createdAtText = text("created_at")
    return TransferSummary(
        id = text("uuid") ?: text("transaction_uuid") ?: text("id_transfer") ?: text("transaction_code") ?: fallbackId,
        direction = direction,
        amount = Money(if (signed < 0) -signed else signed, text("currency").orEmpty()),
        status = statusOf(text("status_code"), statusText),
        statusText = statusText,
        source = get("source").asObjectOrNull().toParty(),
        recipient = get("recipient").asObjectOrNull().toParty(),
        description = text("description"),
        createdAt = parseWallClock(createdAtText),
        createdAtText = createdAtText,
    )
}

private fun JsonObject?.toParty(): TransferParty {
    val name = listOfNotNull(text("name"), text("lastname")).joinToString(" ").ifEmpty { null }
    return TransferParty(
        name = name,
        username = text("username"),
        bank = text("bank"),
        last4 = text("last4"),
        avatar = text("avatar"),
    )
}

/** El `status_code` manda; si falta o no se conoce, se mira el texto (`Aceptada`, `Pendiente`, `Fallida`). */
internal fun statusOf(code: String?, text: String?): TransferStatus {
    val byCode = code?.toIntOrNull()?.let { c -> TransferStatus.entries.firstOrNull { it.code == c } }
    if (byCode != null) return byCode
    val lower = text?.lowercase().orEmpty()
    return when {
        lower.startsWith("acept") -> TransferStatus.ACCEPTED
        lower.startsWith("pend") -> TransferStatus.PENDING
        lower.startsWith("fall") || lower.startsWith("rechaz") -> TransferStatus.FAILED
        else -> TransferStatus.OTHER
    }
}

/**
 * Hora de reloj tal cual la da el servidor. La app original recorta el texto (`substring`) antes de parsearlo,
 * lo que descarta el desfase (`-04:00`) y muestra esa hora sin convertirla a la zona del teléfono. Aquí se hace
 * igual: se leen los 19 primeros caracteres (`yyyy-MM-ddTHH:mm:ss`, admitiendo un espacio en lugar de la `T`).
 */
internal fun parseWallClock(text: String?): LocalDateTime? {
    val head = text?.trim()?.take(19) ?: return null
    if (head.length < 19) return null
    return try {
        LocalDateTime.parse(head.replace(' ', 'T'))
    } catch (e: IllegalArgumentException) {
        null
    }
}
