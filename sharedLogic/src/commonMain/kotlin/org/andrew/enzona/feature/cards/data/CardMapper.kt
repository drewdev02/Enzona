package org.andrew.enzona.feature.cards.data

import org.andrew.enzona.core.util.asFlag
import org.andrew.enzona.feature.cards.domain.BankCard

/** `false` llega como texto en campos de texto no aplicables: se trata como ausente. */
private fun String?.textOrNull(): String? =
    this?.trim()?.takeIf { it.isNotEmpty() && !it.equals("false", ignoreCase = true) }

/** "0532" -> "05/32". Null si no son 4 dígitos o el mes no está entre 01 y 12. */
internal fun formatExpiry(raw: String?): String? {
    val s = raw?.trim() ?: return null
    if (s.length != 4 || !s.all { it.isDigit() }) return null
    val month = s.substring(0, 2).toInt()
    return if (month in 1..12) "${s.substring(0, 2)}/${s.substring(2)}" else null
}

/** Null si falta lo imprescindible para mostrarla (los últimos cuatro dígitos). */
internal fun CardDto.toDomain(): BankCard? {
    val last4 = last4.textOrNull() ?: return null
    return BankCard(
        id = fundingSourceUuid.textOrNull() ?: last4,
        last4 = last4,
        holder = cardholder.textOrNull().orEmpty().uppercase(),
        expiry = formatExpiry(expdate),
        currency = currency.textOrNull().orEmpty().uppercase(),
        bankCode = bankCode.textOrNull().orEmpty(),
        bankName = bankName.textOrNull().orEmpty(),
        isPrimary = primarySource.asFlag(),
        createdAt = createAt.textOrNull(),
    )
}

/**
 * Orden de la pantalla: la predeterminada primero y luego las más recientes. Es una INFERENCIA
 * a partir de una captura del original (las demás parecen ir de más nueva a más antigua); el
 * endpoint no documenta orden. `create_at` es "AAAA-MM-DD hh:mm:ss", ordenable como texto.
 */
internal fun List<BankCard>.sortedForDisplay(): List<BankCard> =
    sortedWith(compareByDescending<BankCard> { it.isPrimary }.thenByDescending { it.createdAt.orEmpty() })
