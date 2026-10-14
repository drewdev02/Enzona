package org.andrew.enzona.feature.auth.data

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.submitForm
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
import io.ktor.http.URLBuilder
import org.andrew.enzona.core.EnzonaConfig
import org.andrew.enzona.core.crypto.Pkce
import org.andrew.enzona.core.result.EzResult
import org.andrew.enzona.core.result.Failure
import org.andrew.enzona.feature.auth.data.dto.TokenDto

/**
 * OAuth2 Authorization Code + PKCE contra identity.enzona.net (verificado en vivo).
 * El login interactivo (multi-paso con SMS OTP) lo abre la UI nativa en el navegador;
 * aquí se construye la URL de authorize y se canjea/renueva el token (form-urlencoded).
 */
class OAuthService(private val client: HttpClient) {

    /** URL que la UI nativa abre en Custom Tabs / ASWebAuthenticationSession. */
    fun authorizeUrl(pkce: Pkce): String {
        val b = URLBuilder(EnzonaConfig.IDENTITY_BASE + EnzonaConfig.AUTHORIZE_PATH)
        b.parameters.apply {
            append("response_type", "code")
            append("client_id", EnzonaConfig.CLIENT_ID)
            append("scope", EnzonaConfig.SCOPE)
            append("redirect_uri", EnzonaConfig.REDIRECT_URI)
            append("code_challenge", pkce.challenge)
            append("code_challenge_method", "S256")
        }
        return b.buildString()
    }

    suspend fun exchangeCode(code: String, pkce: Pkce): EzResult<TokenDto> = tokenRequest(
        Parameters.build {
            append("grant_type", "authorization_code")
            append("client_id", EnzonaConfig.CLIENT_ID)
            append("code", code)
            append("code_verifier", pkce.verifier)
            append("redirect_uri", EnzonaConfig.REDIRECT_URI)
        }
    )

    suspend fun refresh(refreshToken: String): EzResult<TokenDto> = tokenRequest(
        Parameters.build {
            append("grant_type", "refresh_token")
            append("client_id", EnzonaConfig.CLIENT_ID)
            append("refresh_token", refreshToken)
        }
    )

    private suspend fun tokenRequest(form: Parameters): EzResult<TokenDto> = try {
        val resp = client.submitForm(
            url = EnzonaConfig.IDENTITY_BASE + EnzonaConfig.TOKEN_PATH,
            formParameters = form,
        )
        if (resp.status == HttpStatusCode.OK) {
            EzResult.Success(resp.body<TokenDto>())
        } else {
            EzResult.Error(Failure.Auth("Token HTTP ${resp.status.value}: ${resp.bodyAsText()}"))
        }
    } catch (e: Exception) {
        EzResult.Error(Failure.Network(e.message))
    }
}
