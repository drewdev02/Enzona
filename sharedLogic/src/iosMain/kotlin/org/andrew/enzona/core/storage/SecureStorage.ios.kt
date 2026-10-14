package org.andrew.enzona.core.storage

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.allocArrayOf
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import platform.CoreFoundation.CFDictionaryAddValue
import platform.CoreFoundation.CFDictionaryCreateMutable
import platform.CoreFoundation.CFMutableDictionaryRef
import platform.CoreFoundation.CFRelease
import platform.CoreFoundation.CFStringRef
import platform.CoreFoundation.CFTypeRef
import platform.CoreFoundation.CFTypeRefVar
import platform.CoreFoundation.kCFBooleanTrue
import platform.CoreFoundation.kCFTypeDictionaryKeyCallBacks
import platform.CoreFoundation.kCFTypeDictionaryValueCallBacks
import platform.Foundation.CFBridgingRelease
import platform.Foundation.CFBridgingRetain
import platform.Foundation.NSData
import platform.Foundation.create
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemDelete
import platform.Security.errSecSuccess
import platform.Security.kSecAttrAccessible
import platform.Security.kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrService
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecMatchLimit
import platform.Security.kSecMatchLimitOne
import platform.Security.kSecReturnData
import platform.Security.kSecValueData
import platform.posix.memcpy

/**
 * SecureStorage para iOS respaldado por el Keychain (clase generic password).
 * Accesible tras el primer desbloqueo y solo en este dispositivo: el token no se
 * sincroniza por iCloud ni viaja en backups, y el refresh sigue funcionando en segundo plano.
 */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
class IosSecureStorage(private val service: String = "cu.enzona.kmp") : SecureStorage {

    override fun put(key: String, value: String) {
        remove(key)
        val query = newQuery(key)
        query.setObject(kSecValueData, value.encodeToByteArray().toNSData())
        query.setRef(kSecAttrAccessible, kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly)
        SecItemAdd(query, null)
        CFRelease(query)
    }

    override fun get(key: String): String? = memScoped {
        val query = newQuery(key)
        query.setRef(kSecReturnData, kCFBooleanTrue)
        query.setRef(kSecMatchLimit, kSecMatchLimitOne)
        val result = alloc<CFTypeRefVar>()
        val status = SecItemCopyMatching(query, result.ptr)
        CFRelease(query)
        if (status != errSecSuccess) return@memScoped null
        val data = CFBridgingRelease(result.value) as? NSData ?: return@memScoped null
        data.toByteArray().decodeToString()
    }

    override fun remove(key: String) {
        val query = newQuery(key)
        SecItemDelete(query)
        CFRelease(query)
    }

    override fun clear() {
        val query = newQuery(null)
        SecItemDelete(query)
        CFRelease(query)
    }

    private fun newQuery(account: String?): CFMutableDictionaryRef {
        val query = CFDictionaryCreateMutable(
            null, 0, kCFTypeDictionaryKeyCallBacks.ptr, kCFTypeDictionaryValueCallBacks.ptr,
        )!!
        query.setRef(kSecClass, kSecClassGenericPassword)
        query.setObject(kSecAttrService, service)
        account?.let { query.setObject(kSecAttrAccount, it) }
        return query
    }

    /** Añade una constante de CoreFoundation (ya es un CFTypeRef, no se retiene aparte). */
    private fun CFMutableDictionaryRef.setRef(key: CFStringRef?, value: CFTypeRef?) {
        CFDictionaryAddValue(this, key, value)
    }

    /** Añade un objeto Kotlin/Foundation (String, NSData); el diccionario lo retiene. */
    private fun CFMutableDictionaryRef.setObject(key: CFStringRef?, value: Any?) {
        val ref = CFBridgingRetain(value)
        CFDictionaryAddValue(this, key, ref)
        CFRelease(ref) // el diccionario ya tiene su propia referencia
    }

    private fun ByteArray.toNSData(): NSData = memScoped {
        NSData.create(bytes = allocArrayOf(this@toNSData), length = size.toULong())
    }

    private fun NSData.toByteArray(): ByteArray {
        val size = length.toInt()
        if (size == 0) return ByteArray(0)
        return ByteArray(size).also { out ->
            out.usePinned { memcpy(it.addressOf(0), bytes, length) }
        }
    }
}
