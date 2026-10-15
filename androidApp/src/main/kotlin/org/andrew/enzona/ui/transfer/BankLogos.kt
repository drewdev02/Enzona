package org.andrew.enzona.ui.transfer

import androidx.annotation.DrawableRes
import org.andrew.enzona.R
import org.andrew.enzona.feature.cards.domain.BankBrand

/**
 * Logo de cada banco; un banco que no conocemos no lleva logo (`null`: no se adivina). El `when` es exhaustivo.
 * `tools/check_assets.py` comprueba que iOS usa la misma imagen para cada banco.
 */
@get:DrawableRes
val BankBrand.logoRes: Int?
    get() = when (this) {
        BankBrand.BANMET -> R.drawable.bank_banmet
        BankBrand.BANDEC -> R.drawable.bank_bandec
        BankBrand.BPA -> R.drawable.bank_bpa
        BankBrand.BICSA -> R.drawable.bank_bicsa
        BankBrand.CLASICA -> R.drawable.bank_clasica
        BankBrand.AIS -> R.drawable.bank_ais
        BankBrand.UNKNOWN -> null
    }
