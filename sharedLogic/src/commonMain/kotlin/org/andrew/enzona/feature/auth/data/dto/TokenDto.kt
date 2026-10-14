package org.andrew.enzona.feature.auth.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Respuesta de /oauth2/token (verificada contra el backend real: cobertura 100%). */
@Serializable
data class TokenDto(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String? = null,
    @SerialName("token_type") val tokenType: String? = null,
    @SerialName("expires_in") val expiresIn: Long? = null,
    @SerialName("id_token") val idToken: String? = null,
    @SerialName("scope") val scope: String? = null,
)
