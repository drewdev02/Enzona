package org.andrew.enzona.feature.cards

import org.andrew.enzona.feature.cards.domain.BANDEC_TROPICAL_BIN
import org.andrew.enzona.feature.cards.domain.CardSkin
import org.andrew.enzona.feature.cards.domain.skinFor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CardSkinTest {
    @Test
    fun bankCodesMapToTheOriginalSkins() {
        assertEquals(CardSkin.AIS, skinFor("02", "976000000"))
        assertEquals(CardSkin.CLASICA, skinFor("03", "976003122"))
        assertEquals(CardSkin.BICSA, skinFor("04", null))
        assertEquals(CardSkin.BANMET, skinFor("05", null))
        assertEquals(CardSkin.BANMET, skinFor("95", "922795987")) // los dos códigos del Metropolitano
        assertEquals(CardSkin.BPA, skinFor("12", null))
    }

    @Test
    fun theTropicalBandecIsChosenByItsBinNotByCurrency() {
        assertEquals("924006999", BANDEC_TROPICAL_BIN)
        assertEquals(CardSkin.BANDEC_TROPICAL, skinFor("06", "924006999"))
        assertEquals(CardSkin.BANDEC_TROPICAL, skinFor(" 06 ", " 924006999 "), "tolera espacios")
    }

    @Test
    fun aNormalBandecStaysNormalWhateverItsCurrency() {
        // Una Bandec en USD con otro BIN NO es la tropical: antes se confundía por la moneda.
        assertEquals(CardSkin.BANDEC, skinFor("06", "923506999"))
        assertEquals(CardSkin.BANDEC, skinFor("06", "922706999"))
    }

    @Test
    fun aMissingOrUnreadableBinIsANormalBandecLikeTheOriginal() {
        assertEquals(CardSkin.BANDEC, skinFor("06", null))
        assertEquals(CardSkin.BANDEC, skinFor("06", ""))
        assertEquals(CardSkin.BANDEC, skinFor("06", "92400699"), "un dígito menos no es el BIN")
        assertEquals(CardSkin.BANDEC, skinFor("06", "9240069990"), "un dígito más tampoco")
    }

    @Test
    fun theTropicalBinOnlyMattersForBandec() {
        for (code in listOf("02", "03", "04", "05", "95", "12", "99", "")) {
            assertEquals(false, skinFor(code, BANDEC_TROPICAL_BIN) == CardSkin.BANDEC_TROPICAL, "código «$code»")
        }
    }

    @Test
    fun unknownOrMissingCodeFallsBackToGenericNotACrash() {
        assertEquals(CardSkin.GENERIC, skinFor("99", null))
        assertEquals(CardSkin.GENERIC, skinFor("", null))
        assertEquals(CardSkin.GENERIC, skinFor(null, null))
        assertEquals(CardSkin.BANMET, skinFor(" 95 ", null)) // tolera espacios
    }

    @Test
    fun imageKeysAreResourceNamesAndGenericHasNone() {
        assertEquals("card_skin_bandec_tropical", CardSkin.BANDEC_TROPICAL.imageKey)
        assertEquals("card_skin_clasica", CardSkin.CLASICA.imageKey)
        assertNull(CardSkin.GENERIC.imageKey)
        val keys = CardSkin.entries.mapNotNull { it.imageKey }
        assertEquals(keys.size, keys.toSet().size)
    }
}
