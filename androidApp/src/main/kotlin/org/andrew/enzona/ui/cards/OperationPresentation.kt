package org.andrew.enzona.ui.cards

import org.andrew.enzona.feature.cards.domain.OperationDirection
import org.andrew.enzona.feature.cards.domain.OperationKind

// Presentación de una operación. Los textos son de la UI (el dominio solo guarda hechos); el rediseño
// de Android puede cambiarlos sin tocar la lógica compartida. Mismos textos que la app original.

/** Título de la fila. Un pago a comercio muestra el nombre del comercio si el backend lo trae. */
fun OperationKind.title(merchantAlias: String?): String = when (this) {
    OperationKind.TRANSFER -> "Transferencia"
    OperationKind.CARD_ACTIVATION -> "Activación de Tarjeta"
    OperationKind.MERCHANT_PAYMENT -> merchantAlias ?: "Pago a comercio"
    OperationKind.POS_PAYMENT -> "Pago en POS"
    OperationKind.WITHDRAWAL -> "Extracción"
    OperationKind.CREDIT -> "Crédito"
    OperationKind.PAYROLL -> "Nómina"
    OperationKind.REVERSAL -> "Reverso"
    OperationKind.ETECSA_PAYMENT -> "Pago de servicio a ETECSA"
    OperationKind.MICRO_TOPUP -> "Micro Recarga Móvil"
    OperationKind.MOBILE_TOPUP -> "Recarga Móvil"
    OperationKind.NAUTA_TOPUP -> "Recarga Nauta"
    OperationKind.OWN_TOPUP -> "Recarga Propia"
    OperationKind.UNKNOWN -> "Operación"
}

fun OperationDirection.label(): String = when (this) {
    OperationDirection.DEBIT -> "Débito"
    OperationDirection.CREDIT -> "Crédito"
}

/** Signo que antepone el original al importe (que llega como magnitud). */
fun OperationDirection.sign(): String = when (this) {
    OperationDirection.DEBIT -> "-"
    OperationDirection.CREDIT -> "+"
}

/**
 * Unidades menores (centavos) a texto con dos decimales y sin separador de miles, como la app original
 * (`$3395.15`). Trabaja con el texto de los dígitos: `abs()` de `Long.MIN_VALUE` desbordaría y el dinero
 * nunca pasa por `Double`.
 */
fun formatMinorUnits(minorUnits: Long): String {
    val negative = minorUnits < 0
    val digits = minorUnits.toString().removePrefix("-").padStart(3, '0')
    val text = digits.dropLast(2) + "." + digits.takeLast(2)
    return if (negative) "-$text" else text
}

/** La moneda se muestra en mayúsculas aunque el backend la mande en minúsculas (`cup`). */
fun displayCurrency(currency: String): String = currency.trim().uppercase()
