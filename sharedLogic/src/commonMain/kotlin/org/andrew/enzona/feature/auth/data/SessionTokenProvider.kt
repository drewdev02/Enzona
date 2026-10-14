package org.andrew.enzona.feature.auth.data

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.andrew.enzona.core.network.TokenProvider
import org.andrew.enzona.core.result.EzResult
import org.andrew.enzona.feature.auth.domain.AuthRepository

/**
 * Une el cliente HTTP con la sesión. Serializa los refresh: si varias peticiones reciben
 * 401 a la vez, solo la primera renueva (el refresh_token puede rotar y gastarse una vez);
 * las demás ven un token distinto al que fallaron y simplemente reintentan.
 */
class SessionTokenProvider(
    private val tokenStore: TokenStore,
    private val repo: AuthRepository,
) : TokenProvider {
    private val mutex = Mutex()

    override fun accessToken(): String? = tokenStore.accessToken()

    override suspend fun refreshAfterUnauthorized(failedToken: String?): Boolean =
        mutex.withLock {
            val current = tokenStore.accessToken()
            if (current != null && current != failedToken) true
            else repo.refresh() is EzResult.Success
        }
}
