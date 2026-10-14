package org.andrew.enzona.feature.auth.data

import org.andrew.enzona.core.storage.SecureKeys
import org.andrew.enzona.core.storage.SecureStorage
import org.andrew.enzona.feature.auth.data.dto.TokenDto

/** Persiste el token en SecureStorage (espejo del flutter_secure_storage del APK). */
class TokenStore(private val storage: SecureStorage) {

    fun save(token: TokenDto) {
        storage.put(SecureKeys.ACCESS_TOKEN, token.accessToken)
        token.refreshToken?.let { storage.put(SecureKeys.REFRESH_TOKEN, it) }
        token.idToken?.let { storage.put(SecureKeys.ID_TOKEN, it) }
    }

    fun accessToken(): String? = storage.get(SecureKeys.ACCESS_TOKEN)
    fun refreshToken(): String? = storage.get(SecureKeys.REFRESH_TOKEN)
    fun idToken(): String? = storage.get(SecureKeys.ID_TOKEN)

    fun hasSession(): Boolean = accessToken() != null

    fun clear() {
        storage.remove(SecureKeys.ACCESS_TOKEN)
        storage.remove(SecureKeys.REFRESH_TOKEN)
        storage.remove(SecureKeys.ID_TOKEN)
    }
}
