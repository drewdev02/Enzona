package org.andrew.enzona.ui.cards

import org.andrew.enzona.feature.cards.domain.BankCard

/** Datos INVENTADOS para previews: un ejemplar de cada skin y uno genérico. */
object CardPreviewData {
    private fun card(last4: String, bankCode: String, bankName: String, currency: String,
                     expiry: String = "05/32", primary: Boolean = false, holder: String = "TEST USER") =
        BankCard(
            id = "preview-$last4", last4 = last4, holder = holder, expiry = expiry,
            currency = currency, bankCode = bankCode, bankName = bankName,
            isPrimary = primary, createdAt = null,
        )

    val banmet = card("6856", "95", "Banco Metropolitano S.A", "CUP", "05/32", primary = true)
    val clasica = card("3130", "03", "FINCIMEX CLASICA", "USD", "01/31")
    val bandec = card("5800", "06", "Banco de Crédito y Comercio", "CUP", "02/32")
    val bandecUsd = card("4912", "06", "Banco de Crédito y Comercio", "USD", "02/32")
    val bicsa = card("1204", "04", "BICSA", "USD", "11/30")
    val bpa = card("7731", "12", "Banco Popular de Ahorro", "CUP", "09/29")
    val ais = card("9087", "02", "American International Service", "USD", "03/33")
    val generic = card("0042", "99", "Banco Desconocido", "CUP", "12/30")

    /** En el orden de la pantalla real (predeterminada primero). */
    val all = listOf(banmet, clasica, bandec, bandecUsd, bicsa, bpa, ais, generic)
}
