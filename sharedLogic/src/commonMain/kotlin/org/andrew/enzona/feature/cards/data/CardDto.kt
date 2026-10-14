package org.andrew.enzona.feature.cards.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** La respuesta real viene envuelta: {"card": [ ... ]} (verificado contra el backend). */
@Serializable
data class CardsEnvelopeDto(val card: List<CardDto> = emptyList())

/**
 * Solo los campos que usa la app. Los de tipo inestable (`primary_source` llega como booleano,
 * `status`/`verified` como número) se leen como String y se normalizan en el mapper.
 * Las tarjetas usan `create_at` (SIN la "d"), al revés que la cuenta.
 */
@Serializable
data class CardDto(
    @SerialName("funding_source_uuid") val fundingSourceUuid: String? = null,
    val last4: String? = null,
    val cardholder: String? = null,
    val expdate: String? = null,
    val currency: String? = null,
    @SerialName("bank_code") val bankCode: String? = null,
    @SerialName("bank_name") val bankName: String? = null,
    @SerialName("primary_source") val primarySource: String? = null,
    @SerialName("create_at") val createAt: String? = null,
)
