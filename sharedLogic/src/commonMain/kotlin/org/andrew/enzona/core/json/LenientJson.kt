package org.andrew.enzona.core.json

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull

// El backend es laxo con los tipos: el mismo campo llega como texto, número o `false` según el
// contexto. Aquí vive la ÚNICA interpretación de esa laxitud; los mapeadores de cada feature la
// reutilizan en vez de reinterpretar el JSON.

/**
 * Texto limpio de un valor JSON. Solo son "ausentes" (`null`): `null`/`JsonNull`, los booleanos JSON sin
 * comillas (`false`/`true`), los objetos y los arrays. Un string se conserva tal cual (tras `trim`; si
 * queda vacío es `null`), incluidos `"True"` o `"false"` como valores textuales. Un número conserva su texto
 * exacto: el dinero nunca pasa por `Double`.
 */
internal fun JsonElement?.asCleanText(): String? {
    if (this == null || this is JsonNull) return null
    if (this !is JsonPrimitive) return null
    if (!isString && booleanOrNull != null) return null
    return content.trim().ifEmpty { null }
}

/** `fault` ausente, `null` o `false` significa "sin error"; cualquier otra cosa es un error del servicio. */
internal fun JsonElement?.hasFault(): Boolean {
    if (this == null || this is JsonNull) return false
    if (this is JsonPrimitive) {
        if (booleanOrNull == false) return false
        val s = content.trim()
        if (s.isEmpty() || s.equals("false", ignoreCase = true)) return false
    }
    return true
}

/**
 * Un booleano que el backend expresa de varias formas: `true`/`false` sin comillas, `"true"`, `"1"`, `1`.
 * Ausente, `null`, `false`, `"0"`, `"false"` y cualquier otra cosa es `false`. OJO: [asCleanText] descarta los
 * booleanos sin comillas, así que un flag NO se puede leer con él (un `true` desaparecería).
 */
internal fun JsonElement?.asFlag(): Boolean {
    if (this !is JsonPrimitive) return false
    if (!isString) booleanOrNull?.let { return it }
    val s = content.trim()
    return s == "1" || s.equals("true", ignoreCase = true)
}

internal fun JsonObject?.flag(key: String): Boolean = this?.get(key).asFlag()

internal fun JsonElement?.asObjectOrNull(): JsonObject? = this as? JsonObject

/** Los elementos de un array JSON; cualquier otra cosa (`false`, `null`, un objeto...) es "sin elementos". */
internal fun JsonElement?.asArrayOrEmpty(): List<JsonElement> = (this as? JsonArray) ?: emptyList()

/** Texto limpio de la clave [key] de un objeto, o `null` si falta o no es utilizable. */
internal fun JsonObject?.text(key: String): String? = this?.get(key).asCleanText()
