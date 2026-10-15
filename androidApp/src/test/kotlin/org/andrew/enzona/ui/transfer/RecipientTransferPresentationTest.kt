package org.andrew.enzona.ui.transfer

import org.andrew.enzona.core.money.Money
import org.andrew.enzona.feature.cards.domain.BankBrand
import org.andrew.enzona.feature.cards.domain.BankCard
import org.andrew.enzona.feature.transfer.domain.TransferOutcome
import org.andrew.enzona.feature.transfer.domain.TransferRecipient
import org.andrew.enzona.feature.transfer.domain.TransferRequest
import org.andrew.enzona.feature.transfer.domain.TransferResultKind
import org.andrew.enzona.feature.transfer.domain.UnknownCause
import org.andrew.enzona.feature.transfer.presentation.RecipientTransferUiState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RecipientTransferPresentationTest {
    private val source = BankCard("c1", "6856", "TITULAR", null, "CUP", "05", "BANMET", true, null)
    private val amount = Money(2_500, "cup")

    @Test
    fun theTitleSaysWhichFormItIs() {
        assertEquals("Transferir a una cuenta", RecipientKind.ACCOUNT.title())
        assertEquals("Transferir a un amigo", RecipientKind.FRIEND.title())
    }

    @Test
    fun confirmationNamesTheSourceCardAndThePerson() {
        assertEquals(
            "¿Está seguro que desea transferir \$25.00 CUP de la tarjeta BANMET 6856 a la cuenta de Ana Pérez?",
            recipientConfirmationText(amount, source, "Ana Pérez", exchangeApplies = false, recipientCurrency = "cup"),
        )
    }

    @Test
    fun confirmationWarnsAboutTheExchangeRateOnlyWhenTheDomainKnowsTheCurrencyDiffers() {
        assertEquals(
            "¿Desea transferir \$25.00 CUP de la tarjeta BANMET 6856 a una tarjeta en USD aplicando la tasa de cambio vigente?",
            recipientConfirmationText(amount, source, "Ana Pérez", exchangeApplies = true, recipientCurrency = "usd"),
        )
        // Sin moneda conocida del destinatario no se afirma ningún cambio, aunque llegara el aviso.
        assertEquals(
            "¿Está seguro que desea transferir \$25.00 CUP de la tarjeta BANMET 6856 a la cuenta de Ana Pérez?",
            recipientConfirmationText(amount, source, "Ana Pérez", exchangeApplies = true, recipientCurrency = null),
        )
    }

    private val ana = TransferRecipient("acc-1", "ana.perez", "Ana Pérez", null, "cup")
    private val form = RecipientTransferUiState.Editing(ana, listOf(source), "c1", "25", "")
    private fun request(description: String) = TransferRequest.ToAccount("c1", ana, amount, description)

    @Test
    fun theReceiptNamesThePersonWithTheNeutralAvatarAndHidesAnEmptyDescription() {
        val outcome = TransferOutcome.Unknown(UnknownCause.NO_RESPONSE)
        val detail = RecipientTransferUiState.Finished(outcome, request(""), source, form).toDetail()
        assertEquals("Ana Pérez", detail.mainText)
        assertEquals(BankBrand.UNKNOWN, detail.destinationBank, "una persona no tiene logo de banco")
        assertEquals("25.00 CUP", detail.amountText)
        assertEquals(TransferResultKind.UNKNOWN, detail.kind)
        assertEquals(emptyList(), detail.rows)
        assertNull(detail.qrContent)
    }

    @Test
    fun aDescriptionBecomesARow() {
        val outcome = TransferOutcome.Unknown(UnknownCause.NO_RESPONSE)
        val detail = RecipientTransferUiState.Finished(outcome, request("Cena"), source, form).toDetail()
        assertEquals(listOf("Descripción" to "Cena"), detail.rows)
        assertEquals("(6856) CUP", detail.sourceSuffix)
    }
}
