package org.andrew.enzona.ui.transfer

import org.andrew.enzona.core.money.Money
import org.andrew.enzona.feature.cards.domain.BankCard
import org.andrew.enzona.feature.cards.domain.bankBrandOf
import org.andrew.enzona.feature.transfer.presentation.OwnCardTransferUiState
import org.andrew.enzona.ui.cards.detailLabel
import org.andrew.enzona.ui.cards.displayCurrency

// Textos propios de "A una de mis tarjetas". Lo común a todos los formularios está en TransferReceiptPresentation.kt.

/** La pregunta del diálogo de confirmación. Con monedas distintas el servidor aplica su tasa de cambio. */
fun confirmationText(amount: Money, source: BankCard, destination: BankCard, exchangeApplies: Boolean): String =
    if (exchangeApplies) {
        "¿Desea transferir ${amount.shown()} de la tarjeta ${source.detailLabel()} a una tarjeta en " +
            "${displayCurrency(destination.currency)} aplicando la tasa de cambio vigente?"
    } else {
        "¿Está seguro que desea transferir ${amount.shown()} de la tarjeta ${source.detailLabel()} " +
            "a la tarjeta ${destination.detailLabel()}?"
    }

/** El comprobante de una transferencia entre tarjetas propias: el destino es la tarjeta (banco y número). */
fun OwnCardTransferUiState.Finished.toDetail(): TransferDetail =
    buildDetail(outcome, request, source, destination.detailLabel(), bankBrandOf(destination.bankCode))
