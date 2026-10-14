package org.andrew.enzona.ui.cards

import org.andrew.enzona.feature.cards.domain.BankCard
import org.andrew.enzona.feature.cards.domain.CardSkin

/**
 * Texto de la fila "Tarjeta" del detalle: banco + últimos 4 dígitos ("BANMET 6856"). Un banco que no
 * conocemos muestra su nombre real (el original mostraba "BANMET" para cualquier banco desconocido).
 */
fun BankCard.detailLabel(): String {
    val bank = when (skin) {
        CardSkin.AIS -> "AIS"
        CardSkin.BANDEC, CardSkin.BANDEC_USD -> "BANDEC"
        CardSkin.BANMET -> "BANMET"
        CardSkin.BICSA -> "BICSA"
        CardSkin.BPA -> "BPA"
        CardSkin.CLASICA -> "Clásica"
        CardSkin.GENERIC -> bankName.ifBlank { "Tarjeta" }
    }
    return "$bank $last4"
}
