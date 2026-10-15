package org.andrew.enzona.feature.cards.domain

/** Tarjeta del usuario, ya normalizada. */
data class BankCard(
    /** `funding_source_uuid`. */
    val id: String,
    val last4: String,
    /** En mayúsculas, como se muestra en la tarjeta. */
    val holder: String,
    /** "MM/AA", o null si el vencimiento no es válido. */
    val expiry: String?,
    /** "CUP", "USD"... */
    val currency: String,
    val bankCode: String,
    val bankName: String,
    val isPrimary: Boolean,
    val createdAt: String?,
    /**
     * Los 9 primeros dígitos del número (`bin` del listado), solo dígitos. `null` si el servidor no lo trajo: entonces
     * no se sabe si es una Bandec Tropical y se trata como Bandec normal, como hace el original.
     */
    val bin: String? = null,
) {
    val skin: CardSkin get() = skinFor(bankCode, bin)

    /** "**** **** **** " que precede a los últimos cuatro dígitos. */
    val maskedPrefix: String get() = "**** **** **** "

    /** Texto para lectores de pantalla: la imagen no se lee y el número queda partido en trozos. */
    fun spokenDescription(): String = buildString {
        append("Tarjeta ").append(bankName.ifBlank { "bancaria" })
        if (currency.isNotBlank()) append(", ").append(currency)
        append(", terminada en ").append(last4.toList().joinToString(" "))
        if (holder.isNotBlank()) append(", titular ").append(holder.lowercase())
        expiry?.let { append(", vence ").append(it.replace("/", " ")) }
        if (isPrimary) append(", predeterminada")
    }
}
