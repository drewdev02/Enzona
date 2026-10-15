package org.andrew.enzona.ui.transfer

import kotlinx.datetime.LocalDateTime
import org.andrew.enzona.core.money.Money
import org.andrew.enzona.feature.cards.domain.BankBrand
import org.andrew.enzona.feature.cards.domain.BankCard
import org.andrew.enzona.feature.transfer.domain.AmountProblem
import org.andrew.enzona.feature.transfer.domain.NotSentReason
import org.andrew.enzona.feature.transfer.domain.RejectionCause
import org.andrew.enzona.feature.transfer.domain.TransferOutcome
import org.andrew.enzona.feature.transfer.domain.TransferReceipt
import org.andrew.enzona.feature.transfer.domain.TransferRequest
import org.andrew.enzona.feature.transfer.domain.TransferStatus
import org.andrew.enzona.feature.transfer.domain.UnknownCause
import org.andrew.enzona.feature.transfer.presentation.OwnCardTransferUiState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class OwnCardTransferPresentationTest {
    private fun card(code: String, currency: String, last4: String) = BankCard(
        id = "id-$last4", last4 = last4, holder = "T", expiry = null, currency = currency, bankCode = code,
        bankName = "x", isPrimary = false, createdAt = null,
    )

    private val banmet = card("05", "CUP", "6856")
    private val bandec = card("06", "CUP", "1234")
    private val bandecUsd = card("06", "USD", "9021")
    private val amount = Money(1_050, "cup")

    @Test
    fun showsMoneyWithSymbolDecimalsAndUpperCaseCurrency() {
        assertEquals("\$10.50 CUP", amount.shown())
        assertEquals("\$0.05 USD", Money(5, "usd").shown())
    }

    @Test
    fun amountProblemsUseTheOriginalWording() {
        assertEquals("El monto no puede ser \$0.00", AmountProblem.ZERO.message())
        assertEquals("El monto no puede contener espacios en blanco", AmountProblem.HAS_WHITESPACE.message())
        assertEquals("Introduzca un monto válido", AmountProblem.INVALID.message())
        assertEquals("Introduzca un monto válido", AmountProblem.EMPTY.message())
    }

    @Test
    fun confirmationNamesBothCards() {
        assertEquals(
            "¿Está seguro que desea transferir \$10.50 CUP de la tarjeta BANMET 6856 a la tarjeta BANDEC 1234?",
            confirmationText(amount, banmet, bandec, exchangeApplies = false),
        )
    }

    @Test
    fun confirmationWarnsAboutTheExchangeRateWhenCurrenciesDiffer() {
        assertEquals(
            "¿Desea transferir \$10.50 CUP de la tarjeta BANMET 6856 a una tarjeta en USD aplicando la tasa de cambio vigente?",
            confirmationText(amount, banmet, bandecUsd, exchangeApplies = true),
        )
    }

    private fun accepted(status: TransferStatus, text: String? = null) =
        TransferOutcome.Accepted(TransferReceipt("EZ-1", status, text, null, null, null))

    @Test
    fun anAcceptedTransferIsDoneAPendingOneIsNot() {
        assertEquals(ResultKind.Success, accepted(TransferStatus.ACCEPTED).resultText().kind)
        assertEquals(ResultKind.Pending, accepted(TransferStatus.PENDING).resultText().kind)
        val other = accepted(TransferStatus.OTHER, "Procesando").resultText()
        assertEquals(ResultKind.Pending, other.kind)
        assertTrue("Procesando" in other.detail!!)
    }

    @Test
    fun aRejectionSaysMoneyDidNotMoveAndKeepsTheServerText() {
        val withText = TransferOutcome.Rejected(RejectionCause.REFUSED, 400, "Saldo insuficiente").resultText()
        assertEquals(ResultKind.Failed, withText.kind)
        assertTrue("Saldo insuficiente" in withText.detail!!)
        assertTrue("No se movió dinero" in withText.detail!!)

        val bare = TransferOutcome.Rejected(RejectionCause.REFUSED, 403, null).resultText()
        assertTrue("403" in bare.detail!!)
        assertTrue("No se movió dinero" in bare.detail!!)

        val expired = TransferOutcome.Rejected(RejectionCause.SESSION_EXPIRED, 401, null).resultText()
        assertTrue("sesión" in expired.detail!!)
    }

    @Test
    fun anUnknownOutcomeNeverClaimsFailureAndTellsNotToRepeat() {
        for (cause in UnknownCause.entries) {
            val text = TransferOutcome.Unknown(cause).resultText()
            assertEquals(ResultKind.Unknown, text.kind)
            assertFalse("fallida" in text.title.lowercase())
            assertTrue("No repitas" in text.detail!!)
        }
    }

    // ---- Comprobante "Detalles de la transferencia" ----

    private val form = OwnCardTransferUiState.Editing(
        listOf(banmet, bandec), banmet.id, bandec.id, "10.50", "Ahorro mensual",
    )
    private val request = TransferRequest.ToOwnCard(banmet.id, bandec.id, Money(1_050, "cup"), "Ahorro mensual")

    private fun finished(outcome: TransferOutcome) =
        OwnCardTransferUiState.Finished(outcome, request, banmet, bandec, form)

    @Test
    fun aConfirmedTransferFillsEveryPartOfTheReceipt() {
        val receipt = TransferReceipt(
            "kf1jldsUqZ1o", TransferStatus.ACCEPTED, "Aceptada", LocalDateTime(2026, 9, 11, 11, 30),
            "2026-09-11T11:30:00.000-04:00", "SIG-1",
        )
        val detail = finished(TransferOutcome.Accepted(receipt)).toDetail()
        assertEquals(ResultKind.Success, detail.kind)
        assertEquals("Transferencia confirmada", detail.statusTitle)
        assertNull(detail.note)
        assertEquals("BANDEC 1234", detail.mainText)
        assertEquals(BankBrand.BANDEC, detail.destinationBank)
        assertEquals("10.50 CUP", detail.amountText)
        assertEquals("11/09/2026 11:30 AM", detail.dateText)
        assertEquals(listOf("No.Operación" to "kf1jldsUqZ1o", "Descripción" to "Ahorro mensual"), detail.rows)
        assertEquals(BankBrand.BANMET, detail.sourceBank)
        assertEquals("T", detail.sourceHolder)
        assertEquals("(6856) CUP", detail.sourceSuffix)
        assertEquals("SIG-1", detail.qrContent)
    }

    @Test
    fun theDateFallsBackToTheReceivedTextAndIsAbsentWithoutIt() {
        val raw = TransferReceipt("EZ-1", TransferStatus.ACCEPTED, null, null, "ayer", null)
        assertEquals("ayer", finished(TransferOutcome.Accepted(raw)).toDetail().dateText)
        val none = TransferReceipt("EZ-1", TransferStatus.ACCEPTED, null, null, null, null)
        val detail = finished(TransferOutcome.Accepted(none)).toDetail()
        assertNull(detail.dateText)
        assertNull(detail.qrContent, "sin firma no hay QR")
    }

    @Test
    fun aFailedOrUnknownReceiptHasNoOperationNumberDateOrQr() {
        for (outcome in listOf(
            TransferOutcome.Rejected(RejectionCause.REFUSED, 400, "Saldo insuficiente"),
            TransferOutcome.Unknown(UnknownCause.NO_RESPONSE),
        )) {
            val detail = finished(outcome).toDetail()
            assertEquals(listOf("Descripción" to "Ahorro mensual"), detail.rows)
            assertNull(detail.dateText)
            assertNull(detail.qrContent)
            assertNotNull(detail.note)
        }
        assertEquals("Transferencia fallida", finished(TransferOutcome.Rejected(RejectionCause.REFUSED, 400, null)).toDetail().statusTitle)
    }

    @Test
    fun aBlankDescriptionAddsNoRow() {
        val blank = OwnCardTransferUiState.Finished(
            TransferOutcome.Unknown(UnknownCause.NO_RESPONSE), request.copy(description = "  "), banmet, bandec, form,
        )
        assertEquals(emptyList(), blank.toDetail().rows)
    }

    @Test
    fun everyNotSentReasonHasAMessage() {
        for (reason in NotSentReason.entries) assertNotNull(reason.message().takeIf { it.isNotBlank() })
        assertEquals("Escribe tu contraseña de pago.", NotSentReason.PASSWORD_EMPTY.message())
    }
}
