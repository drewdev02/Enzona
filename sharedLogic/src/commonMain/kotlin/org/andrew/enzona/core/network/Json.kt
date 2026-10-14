package org.andrew.enzona.core.network

import kotlinx.serialization.json.Json

/**
 * Config JSON TOLERANTE: obligatoria porque el backend mezcla tipos
 * (devuelve `false` en campos no aplicables, y flags como número/string/bool).
 * Ver docs 06 §Tipos laxos.
 */
val enzonaJson: Json = Json {
    isLenient = true
    ignoreUnknownKeys = true
    coerceInputValues = true
    explicitNulls = false
}
