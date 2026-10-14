package org.andrew.enzona.feature.auth.data

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import okio.ByteString.Companion.decodeBase64

/**
 * Lee claims de un JWT SIN verificar la firma: sirve solo para datos de presentación
 * (p. ej. `sub` = username). Nunca para decidir autorización: eso lo hace el servidor.
 */
object JwtClaims {
    fun payload(token: String): JsonObject? {
        val part = token.split('.').getOrNull(1) ?: return null
        val bytes = part.decodeBase64() ?: return null // okio acepta base64url sin padding
        return try {
            Json.parseToJsonElement(bytes.utf8()).jsonObject
        } catch (e: Exception) {
            null
        }
    }

    fun string(token: String, claim: String): String? =
        (payload(token)?.get(claim) as? JsonPrimitive)?.takeIf { it.isString }?.content
            ?.takeIf { it.isNotBlank() }
}
