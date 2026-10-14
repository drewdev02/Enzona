package org.andrew.enzona.core.storage

/**
 * Almacenamiento seguro por plataforma (Keystore en Android, Keychain en iOS).
 * Para tokens/datos sensibles. Nunca en disco plano ni en logs.
 */
interface SecureStorage {
    fun put(key: String, value: String)
    fun get(key: String): String?
    fun remove(key: String)
    fun clear()
}

object SecureKeys {
    const val ACCESS_TOKEN = "access_token"
    const val REFRESH_TOKEN = "refresh_token"
    const val ID_TOKEN = "id_token"
}
