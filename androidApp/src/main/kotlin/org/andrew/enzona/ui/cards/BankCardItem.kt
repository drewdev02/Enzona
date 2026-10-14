package org.andrew.enzona.ui.cards

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import org.andrew.enzona.feature.cards.domain.BankCard
import org.andrew.enzona.feature.cards.domain.CardSkin
import org.andrew.enzona.ui.designsystem.components.EzBankCard

/** Traduce una [BankCard] del dominio a los parámetros de presentación de [EzBankCard]. */
@Composable
fun BankCardItem(card: BankCard, onMenuClick: () -> Unit, modifier: Modifier = Modifier) {
    val skin = card.skin
    EzBankCard(
        last4 = card.last4,
        holder = card.holder,
        currency = card.currency,
        expiry = card.expiry,
        isPrimary = card.isPrimary,
        numberInk = skin.number.toColor(),
        textInk = skin.text.toColor(),
        menuInk = skin.menu.toColor(),
        menuScrim = skin.menuScrim,
        description = card.spokenDescription(),
        menuDescription = "Más opciones de la tarjeta terminada en ${card.last4}",
        onMenuClick = onMenuClick,
        modifier = modifier,
        background = skin.imageRes?.let { painterResource(it) },
        fallbackTitle = card.bankName.takeIf { skin == CardSkin.GENERIC && it.isNotBlank() },
        numberInset = skin.numberInset,
    )
}
