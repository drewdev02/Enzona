package org.andrew.enzona.feature.cards.domain

import org.andrew.enzona.core.money.Money

enum class OperationDirection {
    DEBIT,
    CREDIT,
}

enum class OperationKind {
    TRANSFER,
    CARD_ACTIVATION,
    MERCHANT_PAYMENT,
    POS_PAYMENT,
    WITHDRAWAL,
    CREDIT,
    PAYROLL,
    REVERSAL,
    ETECSA_PAYMENT,
    MICRO_TOPUP,
    MOBILE_TOPUP,
    NAUTA_TOPUP,
    OWN_TOPUP,
    UNKNOWN,
}

data class CardOperation(
    val kind: OperationKind,
    val direction: OperationDirection,
    val amount: Money,
    val merchantAlias: String?,
    val dateText: String,
)

data class CardActivity(
    val balance: Money?,
    val available: Money?,
    val currency: String,
    val operations: List<CardOperation>,
    val skippedOperations: Int,
)

private val TELEPHONY_CODES = listOf(
    "TEL", "MREC", "MRG", "RECA", "RMV", "MOVI", "NAU", "RNT", "PCU", "RPP", "PROP",
)

/**
 * Clasifica una operación a partir de su descripción, canal y acción.
 *
 * ⚠ Nota por análisis estático: las comprobaciones son sensibles a mayúsculas.
 * Las filas del original para GAS, ONAT/IMP, ELE/LUZ, AGU, MULT, CIMX, Hist, PLI, CADT
 * y CAD solo se alcanzarían si el código además contuviera uno de los 11 códigos de telefonía
 * (TEL, MREC, MRG, RECA, RMV, MOVI, NAU, RNT, PCU, RPP, PROP). Por tanto, en la práctica
 * esos pagos (incluido "CAD") no contienen códigos de telefonía y caen en MERCHANT_PAYMENT
 * (mostrando su alias de comercio), por lo que no se modelan como variantes separadas de OperationKind.
 */
fun classifyOperation(description: String, channel: OperationChannel, action: OperationAction): OperationKind {
    val d = description
    if (d.isEmpty()) {
        return when {
            channel == OperationChannel.Reversal -> OperationKind.REVERSAL
            channel == OperationChannel.Event && action == OperationAction.Credit -> OperationKind.PAYROLL
            action == OperationAction.Debit && channel == OperationChannel.Pos -> OperationKind.POS_PAYMENT
            action == OperationAction.Debit -> OperationKind.WITHDRAWAL
            else -> OperationKind.CREDIT
        }
    }

    if (d == "TRF" || d == "TRAN") return OperationKind.TRANSFER
    if (channel == OperationChannel.Enzona && d.contains("TR EZ")) return OperationKind.CARD_ACTIVATION

    if (TELEPHONY_CODES.none { d.contains(it) }) return OperationKind.MERCHANT_PAYMENT

    if (d.contains("TEL")) return OperationKind.ETECSA_PAYMENT
    if (d.contains("MREC") || d.contains("MRG")) return OperationKind.MICRO_TOPUP
    if (d.contains("RECA") || d.contains("RMV") || d == "MOVI") return OperationKind.MOBILE_TOPUP
    if (d.contains("NAU") || d.contains("RNT")) return OperationKind.NAUTA_TOPUP
    if (d.contains("PCU") || d.contains("RPP") || d.contains("PROP")) return OperationKind.OWN_TOPUP

    return OperationKind.UNKNOWN
}
