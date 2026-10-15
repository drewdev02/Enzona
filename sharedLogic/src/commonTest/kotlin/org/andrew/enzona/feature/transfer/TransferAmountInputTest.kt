package org.andrew.enzona.feature.transfer

import org.andrew.enzona.feature.transfer.domain.AmountInput
import org.andrew.enzona.feature.transfer.domain.AmountProblem
import org.andrew.enzona.feature.transfer.domain.parseAmountInput
import kotlin.test.Test
import kotlin.test.assertEquals

class TransferAmountInputTest {
    private fun valid(minor: Long) = AmountInput.Valid(minor)
    private fun problem(p: AmountProblem) = AmountInput.Problem(p)

    @Test
    fun readsPlainDecimals() {
        assertEquals(valid(1_050), parseAmountInput("10.50"))
        assertEquals(valid(1_000), parseAmountInput("10"))
        assertEquals(valid(1_050), parseAmountInput("10.5"))
        assertEquals(valid(5), parseAmountInput("0.05"))
        assertEquals(valid(1_050), parseAmountInput("10.500"), "ceros de más no cambian el valor")
        assertEquals(valid(50), parseAmountInput(".5"))
    }

    @Test
    fun anEmptyAmountIsEmpty() = assertEquals(problem(AmountProblem.EMPTY), parseAmountInput(""))

    @Test
    fun anyWhitespaceIsRejectedWithItsOwnProblem() {
        for (text in listOf(" 10", "10 ", "1 0", "10\t", "10\n")) {
            assertEquals(problem(AmountProblem.HAS_WHITESPACE), parseAmountInput(text), "«$text»")
        }
    }

    @Test
    fun zeroIsRejectedInEveryForm() {
        for (text in listOf("0", "0.0", "0.00", "000", ".0")) {
            assertEquals(problem(AmountProblem.ZERO), parseAmountInput(text), text)
        }
    }

    @Test
    fun whatIsNotAPlainPositiveDecimalIsInvalid() {
        for (text in listOf("-5", "+5", "1,50", "1.234", "1e3", "abc", "$10", ".", "1.2.3", "٣", "１０")) {
            assertEquals(problem(AmountProblem.INVALID), parseAmountInput(text), text)
        }
    }

    @Test
    fun anAmountThatOverflowsIsInvalidNotWrapped() {
        assertEquals(problem(AmountProblem.INVALID), parseAmountInput("99999999999999999999"))
        assertEquals(problem(AmountProblem.INVALID), parseAmountInput("92233720368547758.08"))
        assertEquals(valid(Long.MAX_VALUE), parseAmountInput("92233720368547758.07"))
    }
}
