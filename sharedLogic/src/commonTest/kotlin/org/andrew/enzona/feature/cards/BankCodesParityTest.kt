package org.andrew.enzona.feature.cards

import org.andrew.enzona.core.money.sameCurrency
import org.andrew.enzona.feature.cards.domain.BankBrand
import org.andrew.enzona.feature.cards.domain.CardSkin
import org.andrew.enzona.feature.cards.domain.KNOWN_BANK_CODES
import org.andrew.enzona.feature.cards.domain.bankBrandOf
import org.andrew.enzona.feature.cards.domain.bankBrandOfCode
import org.andrew.enzona.feature.cards.domain.skinFor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Una sola tabla de códigos de banco: el logo de una transferencia y el skin de una tarjeta nunca se contradicen. */
class BankCodesParityTest {
    private fun brandOfSkin(skin: CardSkin): BankBrand = when (skin) {
        CardSkin.AIS -> BankBrand.AIS
        CardSkin.CLASICA -> BankBrand.CLASICA
        CardSkin.BICSA -> BankBrand.BICSA
        CardSkin.BANMET -> BankBrand.BANMET
        CardSkin.BANDEC, CardSkin.BANDEC_USD -> BankBrand.BANDEC
        CardSkin.BPA -> BankBrand.BPA
        CardSkin.GENERIC -> BankBrand.UNKNOWN
    }

    @Test
    fun everyKnownCodeGivesTheSameBankForSkinAndBrand() {
        assertEquals(setOf("02", "03", "04", "05", "06", "12", "95"), KNOWN_BANK_CODES)
        for (code in KNOWN_BANK_CODES) {
            for (currency in listOf("CUP", "USD", "", null)) {
                val skin = skinFor(code, currency)
                assertEquals(bankBrandOfCode(code), brandOfSkin(skin), "código $code, moneda $currency")
                assertEquals(bankBrandOf(code), brandOfSkin(skin))
            }
        }
    }

    @Test
    fun anUnknownCodeIsGenericAndUnknownEverywhere() {
        for (code in listOf("99", "", " ", null, "BANMET", "banco")) {
            assertEquals(CardSkin.GENERIC, skinFor(code, "CUP"), "código $code")
            assertEquals(BankBrand.UNKNOWN, bankBrandOfCode(code), "código $code")
        }
    }

    @Test
    fun skinForReadsOnlyCodesNotNamesEvenThoughTheBrandLookupReadsBoth() {
        // Comportamiento que se conserva: un NOMBRE en el campo de código no elige skin; `bankBrandOf` sí lo reconoce.
        assertEquals(CardSkin.GENERIC, skinFor("BANMET", "CUP"))
        assertEquals(BankBrand.BANMET, bankBrandOf("BANMET"))
    }

    @Test
    fun bandecHasTwoSkinsChosenByCurrencyWithoutCaseOrSpaceSensitivity() {
        assertEquals(CardSkin.BANDEC_USD, skinFor("06", "USD"))
        assertEquals(CardSkin.BANDEC_USD, skinFor(" 06 ", " usd "))
        assertEquals(CardSkin.BANDEC, skinFor("06", "CUP"))
        assertEquals(CardSkin.BANDEC, skinFor("06", null))
    }

    @Test
    fun currenciesAreComparedWithOneRule() {
        assertTrue(sameCurrency("cup", "CUP"))
        assertTrue(sameCurrency(" CUP ", "cup"))
        assertFalse(sameCurrency("CUP", "USD"))
        assertFalse(sameCurrency(null, "USD"))
        assertTrue(sameCurrency(null, ""), "dos monedas ausentes no se contradicen")
    }
}
