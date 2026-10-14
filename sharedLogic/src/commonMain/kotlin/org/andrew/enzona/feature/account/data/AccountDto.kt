package org.andrew.enzona.feature.account.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** La respuesta real viene envuelta: {"account": {...}} (verificado contra el backend). */
@Serializable
data class AccountEnvelopeDto(val account: AccountDto? = null)

/**
 * Solo los campos que usa la app. El backend mezcla tipos (`"verified":"1"`,
 * `"fingerprint":"true"`, `"profile_level":"8"`, y `false` en campos no aplicables), así que
 * los valores de tipo inestable se leen como String y se normalizan en [toDomain].
 */
@Serializable
data class AccountDto(
    val uuid: String? = null,
    val username: String? = null,
    val name: String? = null,
    val lastname: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val avatar: String? = null,
    @SerialName("primary_currency") val primaryCurrency: String? = null,
    @SerialName("profile_level") val profileLevel: String? = null,
    val verified: String? = null,
    @SerialName("verified_phone") val verifiedPhone: String? = null,
    @SerialName("payment_password") val paymentPassword: String? = null,
    val fingerprint: String? = null,
    @SerialName("merchant_account") val merchantAccount: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
)
