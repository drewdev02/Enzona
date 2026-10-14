package org.andrew.enzona.core.storage

import android.content.Context
import android.content.SharedPreferences

/**
 * SecureStorage para Android.
 * M0: SharedPreferences privadas. TODO M1: cifrar con EncryptedSharedPreferences/Keystore.
 */
class AndroidSecureStorage(context: Context) : SecureStorage {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("enzona_secure", Context.MODE_PRIVATE)

    override fun put(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }

    override fun get(key: String): String? = prefs.getString(key, null)

    override fun remove(key: String) {
        prefs.edit().remove(key).apply()
    }

    override fun clear() {
        prefs.edit().clear().apply()
    }
}
