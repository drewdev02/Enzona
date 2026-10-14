package org.andrew.enzona.ui.cards

import androidx.annotation.DrawableRes
import org.andrew.enzona.R
import org.andrew.enzona.feature.cards.domain.OperationKind

/**
 * Icono de cada tipo de operación (el que usa el original). El `when` es exhaustivo: añadir un tipo
 * sin su icono no compila, y `R.drawable.op_*` no existe si falta el PNG. `tools/check_assets.py`
 * comprueba que los recursos existen en Android e iOS con el mismo contenido.
 */
@get:DrawableRes
val OperationKind.iconRes: Int
    get() = when (this) {
        OperationKind.TRANSFER, OperationKind.REVERSAL -> R.drawable.op_transfer
        OperationKind.CARD_ACTIVATION -> R.drawable.op_ez
        OperationKind.MERCHANT_PAYMENT, OperationKind.UNKNOWN -> R.drawable.op_merchant
        OperationKind.POS_PAYMENT -> R.drawable.op_pos
        OperationKind.WITHDRAWAL -> R.drawable.op_withdrawal
        OperationKind.CREDIT, OperationKind.PAYROLL -> R.drawable.op_payroll
        OperationKind.ETECSA_PAYMENT, OperationKind.MICRO_TOPUP, OperationKind.MOBILE_TOPUP,
        OperationKind.NAUTA_TOPUP, OperationKind.OWN_TOPUP -> R.drawable.op_mobile
    }
