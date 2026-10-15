package org.andrew.enzona.core.crypto

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.get
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import org.andrew.enzona.core.result.EzResult
import org.andrew.enzona.core.result.Failure
import platform.CoreFoundation.CFDataCreate
import platform.CoreFoundation.CFDataGetBytePtr
import platform.CoreFoundation.CFDataGetLength
import platform.CoreFoundation.CFDataRef
import platform.CoreFoundation.CFDictionaryAddValue
import platform.CoreFoundation.CFDictionaryCreateMutable
import platform.CoreFoundation.CFErrorRefVar
import platform.CoreFoundation.CFRelease
import platform.CoreFoundation.kCFTypeDictionaryKeyCallBacks
import platform.CoreFoundation.kCFTypeDictionaryValueCallBacks
import platform.Security.SecKeyCreateEncryptedData
import platform.Security.SecKeyCreateWithData
import platform.Security.kSecAttrKeyClass
import platform.Security.kSecAttrKeyClassPublic
import platform.Security.kSecAttrKeyType
import platform.Security.kSecAttrKeyTypeRSA
import platform.Security.kSecKeyAlgorithmRSAEncryptionPKCS1

@OptIn(ExperimentalForeignApi::class)
internal actual fun rsaEncryptPkcs1v15(key: RsaPublicKey, plaintext: ByteArray): EzResult<ByteArray> = memScoped {
    val failure = EzResult.Error(Failure.Crypto("RSA falló en iOS"))
    val keyData = key.pkcs1.toCFData() ?: return@memScoped failure
    val attributes = CFDictionaryCreateMutable(null, 2, kCFTypeDictionaryKeyCallBacks.ptr, kCFTypeDictionaryValueCallBacks.ptr)
    CFDictionaryAddValue(attributes, kSecAttrKeyType, kSecAttrKeyTypeRSA)
    CFDictionaryAddValue(attributes, kSecAttrKeyClass, kSecAttrKeyClassPublic)
    val error = alloc<CFErrorRefVar>()
    // iOS espera la clave como PKCS#1 (`RSAPublicKey`), no como SPKI: por eso `RsaPublicKey` trae las dos formas.
    val secKey = SecKeyCreateWithData(keyData, attributes, error.ptr)
    CFRelease(attributes)
    CFRelease(keyData)
    if (secKey == null) return@memScoped failure
    val input = plaintext.toCFData()
    if (input == null) {
        CFRelease(secKey)
        return@memScoped failure
    }
    val encrypted = SecKeyCreateEncryptedData(secKey, kSecKeyAlgorithmRSAEncryptionPKCS1, input, error.ptr)
    CFRelease(input)
    CFRelease(secKey)
    if (encrypted == null) return@memScoped failure
    val bytes = encrypted.toByteArray()
    CFRelease(encrypted)
    EzResult.Success(bytes)
}

@OptIn(ExperimentalForeignApi::class)
private fun ByteArray.toCFData(): CFDataRef? =
    if (isEmpty()) CFDataCreate(null, null, 0) else usePinned { CFDataCreate(null, it.addressOf(0).reinterpret(), size.toLong()) }

@OptIn(ExperimentalForeignApi::class)
private fun CFDataRef.toByteArray(): ByteArray {
    val length = CFDataGetLength(this).toInt()
    val pointer = CFDataGetBytePtr(this) ?: return ByteArray(0)
    return ByteArray(length) { pointer[it].toByte() }
}
