package org.andrew.enzona.core.money

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MoneyTest {

    @Test
    fun parsesStandardDecimalAndIntegerValues() {
        assertEquals(95000L, parseMinorUnits("950.00"))
        assertEquals(95000L, parseMinorUnits("950"))
        assertEquals(95050L, parseMinorUnits("950.5"))
        assertEquals(339515L, parseMinorUnits("3395.15"))
        assertEquals(1708800L, parseMinorUnits("17088.00"))
    }

    @Test
    fun acceptsOptionalSignsAndSurroundingWhitespace() {
        assertEquals(95000L, parseMinorUnits("  +950.00  "))
        assertEquals(-95000L, parseMinorUnits("  -950.00  "))
        assertEquals(95000L, parseMinorUnits("+950"))
        assertEquals(-95000L, parseMinorUnits("-950"))
        assertEquals(95050L, parseMinorUnits(" +950.5 "))
        assertEquals(-95050L, parseMinorUnits(" -950.5 "))
    }

    @Test
    fun acceptsTrailingZerosBeyondTwoDecimals() {
        assertEquals(150L, parseMinorUnits("1.500"))
        assertEquals(150L, parseMinorUnits("1.5000"))
        assertEquals(150L, parseMinorUnits("1.500000000"))
        assertEquals(100L, parseMinorUnits("1.0000"))
    }

    @Test
    fun rejectsMoreThanTwoSignificantDecimals() {
        assertNull(parseMinorUnits("1.501"))
        assertNull(parseMinorUnits("1.0001"))
        assertNull(parseMinorUnits("0.001"))
        assertNull(parseMinorUnits("950.555"))
    }

    @Test
    fun handlesZeroAndBoundaryDotCases() {
        assertEquals(0L, parseMinorUnits("0"))
        assertEquals(0L, parseMinorUnits("+0"))
        assertEquals(0L, parseMinorUnits("-0"))
        assertEquals(0L, parseMinorUnits("0.00"))
        assertEquals(0L, parseMinorUnits("-0.00"))

        assertEquals(50L, parseMinorUnits(".5"))
        assertEquals(50L, parseMinorUnits("+.5"))
        assertEquals(-50L, parseMinorUnits("-.5"))

        assertEquals(500L, parseMinorUnits("5."))
        assertEquals(500L, parseMinorUnits("+5."))
        assertEquals(-500L, parseMinorUnits("-5."))
    }

    @Test
    fun rejectsCommasNonNumericEmptyAndScientificNotation() {
        assertNull(parseMinorUnits("1,50"))
        assertNull(parseMinorUnits("abc"))
        assertNull(parseMinorUnits("12a"))
        assertNull(parseMinorUnits(""))
        assertNull(parseMinorUnits("   "))
        assertNull(parseMinorUnits(null))
        assertNull(parseMinorUnits("."))
        assertNull(parseMinorUnits("+."))
        assertNull(parseMinorUnits("-."))
        assertNull(parseMinorUnits("+"))
        assertNull(parseMinorUnits("-"))
        assertNull(parseMinorUnits("1.2.3"))
        assertNull(parseMinorUnits("1e5"))
        assertNull(parseMinorUnits("1E5"))
        assertNull(parseMinorUnits("1.5e2"))
    }

    @Test
    fun handlesOverflowGracefully() {
        // Long.MAX_VALUE = 9223372036854775807L -> 92233720368547758.07
        assertEquals(Long.MAX_VALUE, parseMinorUnits("92233720368547758.07"))
        assertNull(parseMinorUnits("92233720368547758.08"))

        // Long.MIN_VALUE = -9223372036854775808L -> -92233720368547758.08
        assertEquals(Long.MIN_VALUE, parseMinorUnits("-92233720368547758.08"))
        assertNull(parseMinorUnits("-92233720368547758.09"))

        // Valores numéricos astronómicos
        assertNull(parseMinorUnits("99999999999999999999999999"))
        assertNull(parseMinorUnits("-99999999999999999999999999"))
    }

    @Test
    fun moneyModelPreservesCurrencyAsProvided() {
        val minorUnits = parseMinorUnits("950.00")!!
        val cup = Money(minorUnits, "CUP")
        assertEquals(95000L, cup.minorUnits)
        assertEquals("CUP", cup.currency)

        val lowercase = Money(parseMinorUnits("3395.15")!!, "cup")
        assertEquals(339515L, lowercase.minorUnits)
        assertEquals("cup", lowercase.currency)
    }
}
