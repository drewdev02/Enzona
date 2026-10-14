package org.andrew.enzona.feature.cards

import org.andrew.enzona.feature.cards.domain.CardSkin
import org.andrew.enzona.feature.cards.domain.skinFor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CardSkinTest {
    @Test
    fun bankCodesMapToTheOriginalSkins() {
        assertEquals(CardSkin.AIS, skinFor("02", "cup"))
        assertEquals(CardSkin.CLASICA, skinFor("03", "usd"))
        assertEquals(CardSkin.BICSA, skinFor("04", "cup"))
        assertEquals(CardSkin.BANMET, skinFor("05", "cup"))
        assertEquals(CardSkin.BANMET, skinFor("95", "cup")) // los dos códigos del Metropolitano
        assertEquals(CardSkin.BPA, skinFor("12", "cup"))
    }

    @Test
    fun bandecDependsOnCurrencyIgnoringCase() {
        assertEquals(CardSkin.BANDEC, skinFor("06", "cup"))
        assertEquals(CardSkin.BANDEC, skinFor("06", "CUP"))
        assertEquals(CardSkin.BANDEC_USD, skinFor("06", "usd"))
        assertEquals(CardSkin.BANDEC_USD, skinFor("06", "USD"))
        assertEquals(CardSkin.BANDEC, skinFor("06", null))
    }

    @Test
    fun unknownOrMissingCodeFallsBackToGenericNotACrash() {
        assertEquals(CardSkin.GENERIC, skinFor("99", "cup"))
        assertEquals(CardSkin.GENERIC, skinFor("", "cup"))
        assertEquals(CardSkin.GENERIC, skinFor(null, null))
        assertEquals(CardSkin.BANMET, skinFor(" 95 ", "cup")) // tolera espacios
    }

    @Test
    fun imageKeysAreResourceNamesAndGenericHasNone() {
        assertEquals("card_skin_bandec_usd", CardSkin.BANDEC_USD.imageKey)
        assertEquals("card_skin_clasica", CardSkin.CLASICA.imageKey)
        assertNull(CardSkin.GENERIC.imageKey)
        val keys = CardSkin.entries.mapNotNull { it.imageKey }
        assertEquals(keys.size, keys.toSet().size)
    }
}
