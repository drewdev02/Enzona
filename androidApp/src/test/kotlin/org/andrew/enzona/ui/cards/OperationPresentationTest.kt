package org.andrew.enzona.ui.cards

import org.andrew.enzona.feature.cards.domain.OperationDirection
import org.andrew.enzona.feature.cards.domain.OperationKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class OperationPresentationTest {
    @Test
    fun formatsMinorUnitsWithTwoDecimalsAndNoThousandsSeparator() {
        assertEquals("3395.15", formatMinorUnits(339_515))
        assertEquals("950.00", formatMinorUnits(95_000))
        assertEquals("17088.00", formatMinorUnits(1_708_800))
        assertEquals("0.00", formatMinorUnits(0))
        assertEquals("0.05", formatMinorUnits(5))
        assertEquals("0.50", formatMinorUnits(50))
    }

    @Test
    fun formatsNegativeAmountsKeepingTheSign() {
        assertEquals("-9.50", formatMinorUnits(-950))
        assertEquals("-0.01", formatMinorUnits(-1))
    }

    @Test
    fun theExtremesOfLongDoNotOverflow() {
        // `abs(Long.MIN_VALUE)` desbordaría; se formatea con el texto de los dígitos.
        assertEquals("-92233720368547758.08", formatMinorUnits(Long.MIN_VALUE))
        assertEquals("92233720368547758.07", formatMinorUnits(Long.MAX_VALUE))
    }

    @Test
    fun currencyIsShownInUppercase() {
        assertEquals("CUP", displayCurrency("cup"))
        assertEquals("USD", displayCurrency(" usd "))
        assertEquals("", displayCurrency(""))
    }

    @Test
    fun everyKindHasATitleAndTheMerchantAliasOnlyAppliesToMerchantPayments() {
        OperationKind.entries.forEach { assertTrue(it.title(null).isNotBlank(), "$it sin título") }
        assertEquals("Tienda La Esquina", OperationKind.MERCHANT_PAYMENT.title("Tienda La Esquina"))
        assertEquals("Pago a comercio", OperationKind.MERCHANT_PAYMENT.title(null))
        assertEquals("Transferencia", OperationKind.TRANSFER.title("Un alias que no debe usarse"))
    }

    @Test
    fun titlesMatchTheOriginalApp() {
        assertEquals("Recarga Móvil", OperationKind.MOBILE_TOPUP.title(null))
        assertEquals("Nómina", OperationKind.PAYROLL.title(null))
        assertEquals("Pago de servicio a ETECSA", OperationKind.ETECSA_PAYMENT.title(null))
    }

    @Test
    fun directionGivesTheSignAndTheLabel() {
        assertEquals("-", OperationDirection.DEBIT.sign())
        assertEquals("+", OperationDirection.CREDIT.sign())
        assertEquals("Débito", OperationDirection.DEBIT.label())
        assertEquals("Crédito", OperationDirection.CREDIT.label())
    }

    @Test
    fun everyKindMapsToADrawable() {
        OperationKind.entries.forEach { assertNotEquals(0, it.iconRes, "$it sin icono") }
    }

    @Test
    fun theDetailLabelShowsTheBankAndTheLastFourDigits() {
        assertEquals("BANMET 6856", CardPreviewData.banmet.detailLabel())
        assertEquals("BANDEC 5800", CardPreviewData.bandec.detailLabel())
        assertEquals("BANDEC 4912", CardPreviewData.bandecUsd.detailLabel())
        assertEquals("Clásica 3130", CardPreviewData.clasica.detailLabel())
        assertEquals("BICSA 1204", CardPreviewData.bicsa.detailLabel())
        assertEquals("BPA 7731", CardPreviewData.bpa.detailLabel())
        assertEquals("AIS 9087", CardPreviewData.ais.detailLabel())
    }

    @Test
    fun anUnknownBankShowsItsRealNameInsteadOfAGuess() {
        assertEquals("Banco Desconocido 0042", CardPreviewData.generic.detailLabel())
        assertEquals("Tarjeta 0042", CardPreviewData.generic.copy(bankName = " ").detailLabel())
    }
}
