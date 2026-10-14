package org.andrew.enzona.ui.dashboard

import androidx.annotation.DrawableRes
import org.andrew.enzona.R
import org.andrew.enzona.feature.home.domain.HomeService

/**
 * Icono de cada casilla. El `when` es exhaustivo: añadir un servicio al enum sin su icono no
 * compila, y `R.drawable.service_*` no existe si falta el PNG. `tools/check_assets.py` cubre iOS.
 */
@get:DrawableRes
val HomeService.iconRes: Int
    get() = when (this) {
        HomeService.TRANSFER -> R.drawable.service_transfer
        HomeService.BALANCE -> R.drawable.service_balance
        HomeService.BULEVAR -> R.drawable.service_bulevar
        HomeService.GIFT -> R.drawable.service_gift
        HomeService.DONATE -> R.drawable.service_donate
        HomeService.ELECTRICITY -> R.drawable.service_electricity
        HomeService.ETECSA -> R.drawable.service_etecsa
        HomeService.TAXES -> R.drawable.service_taxes
        HomeService.GAS -> R.drawable.service_gas
        HomeService.MAIL -> R.drawable.service_mail
        HomeService.EXTRA_CASH -> R.drawable.service_extra_cash
        HomeService.PURCHASES -> R.drawable.service_purchases
        HomeService.CREATE_ACCOUNT -> R.drawable.service_create_account
        HomeService.ORGANIZATIONS -> R.drawable.service_organizations
    }
