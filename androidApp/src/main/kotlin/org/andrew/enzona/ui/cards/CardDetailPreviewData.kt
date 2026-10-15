package org.andrew.enzona.ui.cards

import org.andrew.enzona.core.money.Money
import org.andrew.enzona.feature.cards.domain.CardActivity
import org.andrew.enzona.feature.cards.domain.CardOperation
import org.andrew.enzona.feature.cards.domain.OperationDirection
import org.andrew.enzona.feature.cards.domain.OperationDirection.CREDIT
import org.andrew.enzona.feature.cards.domain.OperationDirection.DEBIT
import org.andrew.enzona.feature.cards.domain.OperationKind

/** Datos INVENTADOS para previews: una operación de cada tipo, con la forma de la respuesta real. */
object CardDetailPreviewData {
    private fun op(
        kind: OperationKind, direction: OperationDirection, minorUnits: Long,
        date: String, alias: String? = null,
    ) = CardOperation(kind, direction, Money(minorUnits, "CUP"), alias, date)

    val operations = listOf(
        op(OperationKind.TRANSFER, DEBIT, 95_000, "14/10/26"),
        op(OperationKind.MOBILE_TOPUP, DEBIT, 32_400, "14/10/26"),
        op(OperationKind.PAYROLL, CREDIT, 1_708_800, "08/10/26"),
        op(OperationKind.POS_PAYMENT, DEBIT, 12_550, "07/10/26"),
        op(OperationKind.WITHDRAWAL, DEBIT, 500_000, "06/10/26"),
        op(OperationKind.MERCHANT_PAYMENT, DEBIT, 8_975, "05/10/26", alias = "Tienda La Esquina"),
        op(OperationKind.MERCHANT_PAYMENT, DEBIT, 4_000, "04/10/26"),
        op(OperationKind.ETECSA_PAYMENT, DEBIT, 21_000, "03/10/26"),
        op(OperationKind.CARD_ACTIVATION, CREDIT, 0, "01/10/26"),
    )

    val activity = CardActivity(
        balance = Money(339_515, "CUP"),
        available = Money(339_515, "CUP"),
        currency = "CUP",
        operations = operations,
        skippedOperations = 0,
    )

    val noOperations = activity.copy(operations = emptyList())

    /** Saldos ilegibles y una operación omitida: la pantalla debe seguir siendo útil. */
    val partial = activity.copy(balance = null, available = null, operations = operations.take(2), skippedOperations = 2)
}
