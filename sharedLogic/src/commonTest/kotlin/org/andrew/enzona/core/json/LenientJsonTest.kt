package org.andrew.enzona.core.json

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LenientJsonTest {
    private fun json(text: String): JsonElement = Json.parseToJsonElement(text)
    private fun obj(text: String): JsonObject = json(text) as JsonObject

    @Test
    fun flagsAreReadInEveryFormTheBackendUses() {
        for (yes in listOf("true", "\"true\"", "\"TRUE\"", "\"1\"", "1", "\" 1 \"")) {
            assertTrue(json(yes).asFlag(), yes)
        }
        for (no in listOf("false", "\"false\"", "\"0\"", "0", "null", "\"\"", "\"yes\"", "2", "{}", "[]", "\"no\"")) {
            assertFalse(json(no).asFlag(), no)
        }
        assertFalse(null.asFlag())
    }

    @Test
    fun aFlagIsReadFromAnObjectKeyAndAnAbsentKeyIsFalse() {
        val o = obj("""{"deleted":true,"other":"0"}""")
        assertTrue(o.flag("deleted"))
        assertFalse(o.flag("other"))
        assertFalse(o.flag("missing"))
        assertFalse((null as JsonObject?).flag("deleted"))
    }

    @Test
    fun cleanTextDropsUnquotedBooleansButKeepsQuotedOnes() {
        assertNull(json("false").asCleanText())
        assertNull(json("true").asCleanText(), "por eso un flag no se puede leer con asCleanText")
        assertEquals("true", json("\"true\"").asCleanText())
        assertEquals("False", json("\"False\"").asCleanText())
        assertNull(json("null").asCleanText())
        assertNull(json("{}").asCleanText())
        assertNull(json("\"  \"").asCleanText())
        assertEquals("a b", json("\"  a b \"").asCleanText())
    }

    @Test
    fun numbersKeepTheirExactText() {
        assertEquals("3395.15", json("3395.15").asCleanText())
        assertEquals("1400.00", json("1400.00").asCleanText())
        assertEquals("-9.50", json("-9.50").asCleanText())
    }

    @Test
    fun faultsAreOnlyRealErrors() {
        assertFalse(null.hasFault())
        assertFalse(json("null").hasFault())
        assertFalse(json("false").hasFault())
        assertFalse(json("\"false\"").hasFault())
        assertFalse(json("\"\"").hasFault())
        assertTrue(json("{}").hasFault())
        assertTrue(json("{\"code\":1}").hasFault())
        assertTrue(json("true").hasFault())
        assertTrue(json("\"error\"").hasFault())
    }

    @Test
    fun arraysAndObjectsAreReadLeniently() {
        assertEquals(2, json("[1,2]").asArrayOrEmpty().size)
        for (notAnArray in listOf("false", "null", "{}", "\"x\"", "5")) assertTrue(json(notAnArray).asArrayOrEmpty().isEmpty(), notAnArray)
        assertTrue(json("[]").asObjectOrNull() == null)
        assertTrue(json("{}").asObjectOrNull() != null)
        assertEquals("v", obj("""{"k":" v "}""").text("k"))
        assertNull(obj("""{"k":false}""").text("k"))
        assertNull((null as JsonObject?).text("k"))
    }
}
