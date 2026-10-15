package org.andrew.enzona.feature.cards

import org.andrew.enzona.feature.cards.domain.BankBrand
import org.andrew.enzona.feature.cards.domain.bankBrandOf
import org.andrew.enzona.feature.cards.domain.skinFor
import org.andrew.enzona.feature.cards.domain.CardSkin
import kotlin.test.Test
import kotlin.test.assertEquals

class BankBrandTest {
    @Test
    fun theLabelsOfTheOriginalAreRecognised() {
        assertEquals(BankBrand.BANMET, bankBrandOf("BANMET"))
        assertEquals(BankBrand.BANDEC, bankBrandOf("BANDEC"))
        assertEquals(BankBrand.BPA, bankBrandOf("BPA"))
        assertEquals(BankBrand.BICSA, bankBrandOf("BICSA"))
        assertEquals(BankBrand.CLASICA, bankBrandOf("Clásica"))
        assertEquals(BankBrand.AIS, bankBrandOf("AIS"))
    }

    @Test
    fun theBankCodesMatchTheCardSkinTable() {
        // Mismos códigos que `skinFor` (que viene de la tabla del original).
        for (code in listOf("02", "03", "04", "05", "95", "06", "12", "99")) {
            val brand = bankBrandOf(code)
            val skin = skinFor(code, null)
            val expected = when (skin) {
                CardSkin.AIS -> BankBrand.AIS
                CardSkin.CLASICA -> BankBrand.CLASICA
                CardSkin.BICSA -> BankBrand.BICSA
                CardSkin.BANMET -> BankBrand.BANMET
                CardSkin.BANDEC, CardSkin.BANDEC_TROPICAL -> BankBrand.BANDEC
                CardSkin.BPA -> BankBrand.BPA
                CardSkin.GENERIC -> BankBrand.UNKNOWN
            }
            assertEquals(expected, brand, "código $code")
        }
    }

    @Test
    fun fullNamesAreRecognisedIgnoringCaseAndAccents() {
        assertEquals(BankBrand.BANMET, bankBrandOf("Banco Metropolitano S.A"))
        assertEquals(BankBrand.BANDEC, bankBrandOf("Banco de Crédito y Comercio (BANDEC)"))
        assertEquals(BankBrand.BANDEC, bankBrandOf("Banco de Credito y Comercio"))
        assertEquals(BankBrand.BPA, bankBrandOf("Banco Popular de Ahorro"))
        assertEquals(BankBrand.CLASICA, bankBrandOf("FINCIMEX CLASICA"))
        assertEquals(BankBrand.CLASICA, bankBrandOf("clásica"))
        assertEquals(BankBrand.AIS, bankBrandOf("American International Service"))
        assertEquals(BankBrand.BANMET, bankBrandOf("  banmet "))
    }

    @Test
    fun shortAcronymsMustBeWholeWords() {
        assertEquals(BankBrand.UNKNOWN, bankBrandOf("Banco Aisla"), "AIS dentro de otra palabra no cuenta")
        assertEquals(BankBrand.UNKNOWN, bankBrandOf("Compas"))
        assertEquals(BankBrand.AIS, bankBrandOf("AIS Cuba"))
    }

    @Test
    fun anythingElseIsUnknownAndNeverGuessed() {
        for (text in listOf(null, "", "   ", "99", "Banco Desconocido", "123456")) {
            assertEquals(BankBrand.UNKNOWN, bankBrandOf(text), text.toString())
        }
    }
}
