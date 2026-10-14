package org.andrew.enzona.core.storage

import platform.Foundation.NSUserDefaults

/**
 * SecureStorage para iOS.
 * M0: NSUserDefaults (simple, para arrancar). TODO M1: migrar a Keychain (Security framework)
 * antes de guardar tokens reales.
 */
class IosSecureStorage(private val suiteName: String = "cu.enzona.kmp") : SecureStorage {
    private val defaults = NSUserDefaults(suiteName = suiteName)

    override fun put(key: String, value: String) {
        defaults.setObject(value, forKey = key)
    }

    override fun get(key: String): String? = defaults.stringForKey(key)

    override fun remove(key: String) {
        defaults.removeObjectForKey(key)
    }

    override fun clear() {
        defaults.removePersistentDomainForName(suiteName)
    }
}
