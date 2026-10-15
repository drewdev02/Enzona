package org.andrew.enzona.ui.transfer

import kotlinx.datetime.LocalDateTime
import org.andrew.enzona.feature.transfer.domain.TransferDirection
import org.andrew.enzona.feature.transfer.domain.TransferParty
import org.andrew.enzona.feature.transfer.domain.TransferStatus
import org.andrew.enzona.feature.transfer.domain.TransferSummary
import org.andrew.enzona.ui.cards.displayCurrency
import org.andrew.enzona.ui.cards.formatMinorUnits
import org.andrew.enzona.ui.designsystem.components.EzTone

// Presentación de una transferencia. El dominio guarda hechos; los textos y formatos son de la UI.
// Mismas reglas que la app original: título = destinatario, subtítulo = origen, `dd/MM/yyyy hh:mm a`.

/** `BANMET(6856)`; sin banco o sin últimos dígitos, lo que haya (nombre de usuario, nombre...). Nunca vacío. */
fun TransferParty.label(): String {
    val bankText = bank?.takeIf { it.isNotBlank() }
    val digits = last4?.takeIf { it.isNotBlank() }
    return when {
        bankText != null && digits != null -> "$bankText($digits)"
        digits != null -> "Tarjeta ($digits)"
        else -> listOfNotNull(name, username, bankText).firstOrNull { it.isNotBlank() } ?: "—"
    }
}

fun TransferSummary.title(): String = recipient.label()

fun TransferSummary.subtitle(): String = source.label()

/** El importe llega como magnitud; el signo sale del sentido. Sin sentido conocido no se inventa signo. */
fun TransferSummary.amountText(): String {
    val sign = when (direction) {
        TransferDirection.OUTGOING -> "-"
        TransferDirection.INCOMING -> "+"
        TransferDirection.UNKNOWN -> ""
    }
    return sign + formatMinorUnits(amount.minorUnits).removePrefix("-")
}

fun TransferSummary.currencyText(): String = displayCurrency(amount.currency)

/** Hora de reloj tal cual la dio el servidor (no se convierte a la zona del teléfono). */
fun TransferSummary.dateText(): String = createdAt?.let(::formatWallClock) ?: createdAtText.orEmpty()

internal fun formatWallClock(at: LocalDateTime): String {
    fun two(n: Int) = n.toString().padStart(2, '0')
    val hour12 = (at.hour % 12).let { if (it == 0) 12 else it }
    val suffix = if (at.hour < 12) "AM" else "PM"
    return "${two(at.dayOfMonth)}/${two(at.monthNumber)}/${at.year.toString().padStart(4, '0')} ${two(hour12)}:${two(at.minute)} $suffix"
}

fun TransferSummary.statusLabel(): String = when (status) {
    TransferStatus.ACCEPTED -> "Aceptada"
    TransferStatus.PENDING -> "Pendiente"
    TransferStatus.FAILED -> "Fallida"
    TransferStatus.OTHER -> statusText?.takeIf { it.isNotBlank() } ?: "Sin estado"
}

fun TransferSummary.statusTone(): EzTone = when (status) {
    TransferStatus.ACCEPTED -> EzTone.Success
    TransferStatus.PENDING -> EzTone.Warning
    TransferStatus.FAILED -> EzTone.Error
    TransferStatus.OTHER -> EzTone.Neutral
}

/** Texto para lectores de pantalla: toda la fila en una frase. */
fun TransferSummary.spoken(): String = listOf(
    title(), "desde ${subtitle()}", "${amountText()} ${currencyText()}", statusLabel(), dateText(),
).filter { it.isNotBlank() }.joinToString(", ")

/** Iniciales para el avatar de una persona (sin foto): hasta dos palabras. */
fun initialsOf(displayName: String): String =
    displayName.split(Regex("\\s+")).filter { it.isNotEmpty() }.take(2)
        .joinToString("") { it.first().uppercase() }.ifEmpty { "?" }
