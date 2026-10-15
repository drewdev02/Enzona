package org.andrew.enzona.ui.transfer

import kotlinx.serialization.Serializable
import org.andrew.enzona.core.money.Money
import org.andrew.enzona.feature.cards.domain.BankBrand
import org.andrew.enzona.feature.cards.domain.BankCard
import org.andrew.enzona.feature.transfer.presentation.RecipientTransferUiState
import org.andrew.enzona.ui.cards.detailLabel
import org.andrew.enzona.ui.cards.displayCurrency

// Textos de "A una cuenta" y "A un amigo". Lo común a todos los formularios está en TransferReceiptPresentation.kt.

/** Cuál de los dos formularios es. Solo cambia el título: el envío es el mismo. */
@Serializable
enum class RecipientKind { ACCOUNT, FRIEND }

fun RecipientKind.title(): String = when (this) {
    RecipientKind.ACCOUNT -> "Transferir a una cuenta"
    RecipientKind.FRIEND -> "Transferir a un amigo"
}

/**
 * La pregunta del diálogo de confirmación. Solo avisa del cambio de moneda cuando se SABE que difiere
 * ([exchangeApplies] viene del dominio): con la moneda del destinatario desconocida no se afirma nada.
 */
fun recipientConfirmationText(amount: Money, source: BankCard, recipientName: String, exchangeApplies: Boolean, recipientCurrency: String?): String =
    if (exchangeApplies && !recipientCurrency.isNullOrBlank()) {
        "¿Desea transferir ${amount.shown()} de la tarjeta ${source.detailLabel()} a una tarjeta en " +
            "${displayCurrency(recipientCurrency)} aplicando la tasa de cambio vigente?"
    } else {
        "¿Está seguro que desea transferir ${amount.shown()} de la tarjeta ${source.detailLabel()} " +
            "a la cuenta de $recipientName?"
    }

/** El comprobante de una transferencia a una persona: el destino es su nombre, con el avatar neutro. */
fun RecipientTransferUiState.Finished.toDetail(): TransferDetail =
    buildDetail(outcome, request, source, request.recipient.displayName, BankBrand.UNKNOWN)
