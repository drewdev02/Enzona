package org.andrew.enzona.ui.cards

import androidx.annotation.DrawableRes
import org.andrew.enzona.R
import org.andrew.enzona.feature.cards.domain.CardSkin

/**
 * Imagen de cada skin. El `when` es exhaustivo: añadir un skin sin su imagen no compila, y
 * `R.drawable.card_skin_*` no existe si falta el PNG. `tools/check_assets.py` cubre iOS.
 * GENERIC no tiene imagen (se dibuja un fondo propio).
 */
@get:DrawableRes
val CardSkin.imageRes: Int?
    get() = when (this) {
        CardSkin.AIS -> R.drawable.card_skin_ais
        CardSkin.BANDEC -> R.drawable.card_skin_bandec
        CardSkin.BANDEC_USD -> R.drawable.card_skin_bandec_usd
        CardSkin.BANMET -> R.drawable.card_skin_banmet
        CardSkin.BICSA -> R.drawable.card_skin_bicsa
        CardSkin.BPA -> R.drawable.card_skin_bpa
        CardSkin.CLASICA -> R.drawable.card_skin_clasica
        CardSkin.GENERIC -> null
    }
