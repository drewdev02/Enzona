package org.andrew.enzona.ui.transfer

import kotlinx.datetime.LocalDateTime
import org.andrew.enzona.core.money.Money
import org.andrew.enzona.feature.cards.domain.BankCard
import org.andrew.enzona.feature.transfer.domain.AmountProblem
import org.andrew.enzona.feature.transfer.domain.RejectionCause
import org.andrew.enzona.feature.transfer.domain.TransferOutcome
import org.andrew.enzona.feature.transfer.domain.TransferReceipt
import org.andrew.enzona.feature.transfer.domain.TransferRequest
import org.andrew.enzona.feature.transfer.domain.TransferStatus
import org.andrew.enzona.feature.transfer.domain.UnknownCause
import org.andrew.enzona.feature.transfer.presentation.OwnCardFormProblems
import org.andrew.enzona.feature.transfer.presentation.OwnCardTransferUiState

/** Datos INVENTADOS para previews del formulario "A una de mis tarjetas": no tocan Koin ni la red. */
object OwnCardTransferPreviewData {
    private fun card(id: String, code: String, name: String, last4: String, currency: String, primary: Boolean = false) =
        BankCard(id, last4, "TITULAR DE PRUEBA", "12/29", currency, code, name, primary, null)

    val cards = listOf(
        card("c1", "05", "BANMET", "6856", "CUP", primary = true),
        card("c2", "06", "BANDEC", "1234", "CUP"),
        card("c3", "06", "BANDEC", "9021", "USD"),
    )

    val editing = OwnCardTransferUiState.Editing(cards, "c1", destinationId = "c2", amountText = "10.50", description = "Ahorro")

    val withProblems = OwnCardTransferUiState.Editing(
        cards, "c1", destinationId = null, amountText = "0", description = "",
        problems = OwnCardFormProblems(AmountProblem.ZERO, destinationMissing = true, descriptionMissing = true),
    )

    private val request = TransferRequest.ToOwnCard("c1", "c2", Money(1_050, "CUP"), "Ahorro")

    val confirming = OwnCardTransferUiState.Confirming(editing, request, cards[0], cards[1], exchangeApplies = false)

    private fun finished(outcome: TransferOutcome) =
        OwnCardTransferUiState.Finished(outcome, request, cards[0], cards[1], editing)

    val accepted = finished(
        TransferOutcome.Accepted(
            TransferReceipt(
                "kf1jldsUqZ1o", TransferStatus.ACCEPTED, "Aceptada", LocalDateTime(2026, 9, 11, 11, 30),
                "2026-09-11T11:30:00.000-04:00", "SIG-INVENTADA-0001",
            ),
        ),
    )
    val pending = finished(
        TransferOutcome.Accepted(TransferReceipt("kf1jldsUqZ2p", TransferStatus.PENDING, "Pendiente", LocalDateTime(2026, 9, 11, 11, 31), null, null)),
    )
    val rejected = finished(TransferOutcome.Rejected(RejectionCause.REFUSED, 400, "Saldo insuficiente"))
    val unknown = finished(TransferOutcome.Unknown(UnknownCause.NO_RESPONSE))
    val submitting = OwnCardTransferUiState.Submitting(request, cards[0], cards[1])
}
