package org.andrew.enzona.core.logging

import kotlin.test.Test
import kotlin.test.assertEquals

class MessageTemplateTest {
    private fun render(template: String, vararg args: Any?) = renderTemplate(template, args)

    @Test
    fun fillsNamedPlaceholdersByPosition() {
        val r = render("Sesión de {Usuario}: {Tarjetas} tarjetas", "ana", 3)
        assertEquals("Sesión de ana: 3 tarjetas", r.text)
        assertEquals(mapOf<String, Any?>("Usuario" to "ana", "Tarjetas" to 3), r.arguments)
    }

    @Test
    fun doubledBracesAreEscaped() {
        assertEquals("{literal} 5", render("{{literal}} {N}", 5).text)
    }

    @Test
    fun aPlaceholderWithoutArgumentIsKeptAsIs() {
        assertEquals("A=1 B={B}", render("A={A} B={B}", 1).text)
    }

    @Test
    fun extraArgumentsAreIgnored() {
        assertEquals("solo 1", render("solo {A}", 1, 2, 3).text)
    }

    @Test
    fun anUnclosedOrEmptyBraceIsLiteralText() {
        assertEquals("abre { sin cerrar", render("abre { sin cerrar").text)
        assertEquals("vacío {} y 1", render("vacío {} y {A}", 1).text)
    }

    @Test
    fun sensitiveValuesAreMaskedInTheMessageAndInTheProperties() {
        val r = render("token {T}", Sensitive("abc.def.ghi"))
        assertEquals("token ***", r.text)
        assertEquals("***", r.arguments["T"])
    }

    @Test
    fun nullIsShownAsNull() {
        assertEquals("x=null", render("x={X}", null).text)
    }

    @Test
    fun aToStringThatThrowsDoesNotBreakLogging() {
        val hostile = object {
            override fun toString(): String = error("boom")
        }
        assertEquals("v=<toString falló>", render("v={V}", hostile).text)
    }
}
