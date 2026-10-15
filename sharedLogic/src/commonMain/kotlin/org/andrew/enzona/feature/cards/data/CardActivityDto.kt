package org.andrew.enzona.feature.cards.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * DTO laxo para la respuesta de `GET /coremz/1.0.2/api/balance/lastOperations/{funding_source_uuid}`.
 * Los campos numéricos o de texto pueden llegar como strings, números, booleano false o null.
 * Usamos [JsonElement] para tolerar cualquier tipo sin perder información ni lanzar excepciones.
 */
@Serializable
data class CardActivityDto(
    val operation: JsonElement? = null,
    val balance: JsonElement? = null,
    @SerialName("balance_available") val balanceAvailable: JsonElement? = null,
    val currency: JsonElement? = null,
    @SerialName("bank_account") val bankAccount: JsonElement? = null,
    @SerialName("status_code") val statusCode: JsonElement? = null,
    @SerialName("status_denom") val statusDenom: JsonElement? = null,
    @SerialName("transaction_signature") val transactionSignature: JsonElement? = null,
    val fault: JsonElement? = null,
)

@Serializable
data class OperationDto(
    val operationDate: JsonElement? = null, // ⚠ clave camelCase del backend original
    val action: JsonElement? = null,
    val channel: JsonElement? = null,
    val description: JsonElement? = null,
    val amount: JsonElement? = null, // puede ser número o string numérico
    val codigo: JsonElement? = null,
    val avatar: JsonElement? = null,
    val alias: JsonElement? = null,
)
