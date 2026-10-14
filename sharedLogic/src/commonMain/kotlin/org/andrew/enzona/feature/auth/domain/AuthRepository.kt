package org.andrew.enzona.feature.auth.domain

import org.andrew.enzona.core.crypto.Pkce
import org.andrew.enzona.core.result.EzResult
import org.andrew.enzona.core.result.map
import org.andrew.enzona.feature.auth.data.OAuthService
import org.andrew.enzona.feature.auth.data.TokenStore

/** Orquesta el login OAuth y la sesión persistente. */
class AuthRepository(
    private val oauth: OAuthService,
    private val tokenStore: TokenStore,
) {
    /** Genera el PKCE y la URL de authorize para que la UI nativa la abra. */
    fun startLogin(): Pair<Pkce, String> {
        val pkce = Pkce.generate()
        return pkce to oauth.authorizeUrl(pkce)
    }

    /** Canjea el code capturado del redirect y guarda la sesión. */
    suspend fun completeLogin(code: String, pkce: Pkce): EzResult<Unit> =
        oauth.exchangeCode(code, pkce).also { result ->
            if (result is EzResult.Success) tokenStore.save(result.value)
        }.map { }

    /** Renueva el access_token con el refresh_token guardado. */
    suspend fun refresh(): EzResult<Unit> {
        val rt = tokenStore.refreshToken() ?: return EzResult.Error(
            org.andrew.enzona.core.result.Failure.Auth("Sin refresh_token")
        )
        return oauth.refresh(rt).also { result ->
            if (result is EzResult.Success) tokenStore.save(result.value)
        }.map { }
    }

    fun isLoggedIn(): Boolean = tokenStore.hasSession()

    fun idToken(): String? = tokenStore.idToken()

    fun logout() = tokenStore.clear()
}
