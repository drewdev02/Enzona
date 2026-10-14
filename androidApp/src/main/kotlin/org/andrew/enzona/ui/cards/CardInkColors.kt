package org.andrew.enzona.ui.cards

import androidx.compose.ui.graphics.Color
import org.andrew.enzona.feature.cards.domain.CardInk

/** Debe coincidir con INK en tools/check_card_skins.py: el verificador mide contra estos valores. */
fun CardInk.toColor(): Color = when (this) {
    CardInk.BLACK -> Color(0xFF000000)
    CardInk.SOFT_BLACK -> Color(0xFF201F1F)
    CardInk.WHITE -> Color(0xFFFFFFFF)
}
