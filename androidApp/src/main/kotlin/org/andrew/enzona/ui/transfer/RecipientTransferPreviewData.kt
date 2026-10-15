package org.andrew.enzona.ui.transfer

import kotlinx.datetime.LocalDateTime
import org.andrew.enzona.core.money.Money
import org.andrew.enzona.feature.transfer.domain.AmountProblem
import org.andrew.enzona.feature.transfer.domain.RejectionCause
import org.andrew.enzona.feature.transfer.domain.TransferOutcome
import org.andrew.enzona.feature.transfer.domain.TransferReceipt
import org.andrew.enzona.feature.transfer.domain.TransferRecipient
import org.andrew.enzona.feature.transfer.domain.TransferRequest
import org.andrew.enzona.feature.transfer.domain.TransferStatus
import org.andrew.enzona.feature.transfer.domain.UnknownCause
import org.andrew.enzona.feature.transfer.presentation.RecipientTransferUiState

/** Datos INVENTADOS para previews de "A una cuenta" / "A un amigo": no tocan Koin ni la red. */
object RecipientTransferPreviewData {
    private val cards = OwnCardTransferPreviewData.cards
    val recipient = TransferRecipient("acc-uuid-ana", "ana.perez", "Ana Pérez Gómez", null, "cup")

    val editing = RecipientTransferUiState.Editing(recipient, cards, "c1", amountText = "25", description = "Cena")

    val withProblem = editing.copy(amountText = "0", amountProblem = AmountProblem.ZERO)

    private fun request(r: TransferRecipient = recipient, currency: String = "CUP") =
        TransferRequest.ToAccount("c1", r, Money(2_500, currency), "Cena")

    val confirming = RecipientTransferUiState.Confirming(editing, request(), cards[0], exchangeApplies = false)

    /** La cuenta de la otra persona es en USD y la tarjeta de origen en CUP: el servidor aplicará su tasa. */
    val confirmingWithExchange = RecipientTransferUiState.Confirming(
        editing.copy(recipient = recipient.copy(currency = "usd")),
        request(recipient.copy(currency = "usd")), cards[0], exchangeApplies = true,
    )

    private fun finished(outcome: TransferOutcome) =
        RecipientTransferUiState.Finished(outcome, request(), cards[0], editing)

    val accepted = finished(
        TransferOutcome.Accepted(
            TransferReceipt(
                "kf1jldsUqZ9x", TransferStatus.ACCEPTED, "Aceptada", LocalDateTime(2026, 9, 11, 11, 30),
                "2026-09-11T11:30:00.000-04:00", "SIG-INVENTADA-0002",
            ),
        ),
    )
    val rejected = finished(TransferOutcome.Rejected(RejectionCause.REFUSED, 400, "Saldo insuficiente"))
    val unknown = finished(TransferOutcome.Unknown(UnknownCause.NO_RESPONSE))
    val submitting = RecipientTransferUiState.Submitting(request(), cards[0])
}
