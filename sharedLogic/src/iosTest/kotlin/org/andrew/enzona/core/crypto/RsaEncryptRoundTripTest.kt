package org.andrew.enzona.core.crypto

import kotlin.io.encoding.Base64
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.alloc
import kotlinx.cinterop.get
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import org.andrew.enzona.core.result.EzResult
import platform.CoreFoundation.CFDataCreate
import platform.CoreFoundation.CFDataGetBytePtr
import platform.CoreFoundation.CFDataGetLength
import platform.CoreFoundation.CFDataRef
import platform.CoreFoundation.CFDictionaryAddValue
import platform.CoreFoundation.CFDictionaryCreateMutable
import platform.CoreFoundation.CFErrorRefVar
import platform.CoreFoundation.CFNumberCreate
import platform.CoreFoundation.kCFNumberIntType
import platform.CoreFoundation.kCFTypeDictionaryKeyCallBacks
import platform.CoreFoundation.kCFTypeDictionaryValueCallBacks
import platform.Security.SecKeyCopyExternalRepresentation
import platform.Security.SecKeyCopyPublicKey
import platform.Security.SecKeyCreateDecryptedData
import platform.Security.SecKeyCreateRandomKey
import platform.Security.kSecAttrKeySizeInBits
import platform.Security.kSecAttrKeyType
import platform.Security.kSecAttrKeyTypeRSA
import platform.Security.kSecKeyAlgorithmRSAEncryptionOAEPSHA1
import platform.Security.kSecKeyAlgorithmRSAEncryptionPKCS1

/** Prueba real del cifrado en iOS (Security.framework): lo que sale se descifra con la clave privada y PKCS#1 v1.5. */
@OptIn(ExperimentalForeignApi::class)
class RsaEncryptRoundTripTest {
    /** Prefijo fijo de un SPKI RSA-2048 (`MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8A`): el resto es el PKCS#1 de la clave. */
    private val spkiPrefix = Base64.decode("MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8A")

    private fun CFDataRef.bytes(): ByteArray {
        val pointer = CFDataGetBytePtr(this)!!
        return ByteArray(CFDataGetLength(this).toInt()) { pointer[it].toByte() }
    }

    private fun ByteArray.cfData(): CFDataRef = usePinned { CFDataCreate(null, it.addressOf(0).reinterpret(), size.toLong()) }!!

    private val privateKey = memScoped {
        val attributes = CFDictionaryCreateMutable(null, 2, kCFTypeDictionaryKeyCallBacks.ptr, kCFTypeDictionaryValueCallBacks.ptr)
        CFDictionaryAddValue(attributes, kSecAttrKeyType, kSecAttrKeyTypeRSA)
        val bits = alloc<kotlinx.cinterop.IntVar>().also { it.value = 2048 }
        CFDictionaryAddValue(attributes, kSecAttrKeySizeInBits, CFNumberCreate(null, kCFNumberIntType, bits.ptr))
        SecKeyCreateRandomKey(attributes, null)
    }

    private val pem: String by lazy {
        val publicKey = SecKeyCopyPublicKey(privateKey)
        val pkcs1 = SecKeyCopyExternalRepresentation(publicKey, null)!!.bytes()
        "-----BEGIN PUBLIC KEY-----\n" + Base64.encode(spkiPrefix + pkcs1) + "\n-----END PUBLIC KEY-----"
    }

    private fun decrypt(cipher: ByteArray, algorithm: platform.Security.SecKeyAlgorithm?): ByteArray? = memScoped {
        val error = alloc<CFErrorRefVar>()
        SecKeyCreateDecryptedData(privateKey, algorithm, cipher.cfData(), error.ptr)?.bytes()
    }

    private fun encrypt(password: String): ByteArray {
        val result = RsaPaymentPasswordEncryptor(pem).encrypt(password)
        assertIs<EzResult.Success<String>>(result)
        return Base64.decode(result.value)
    }

    @Test
    fun decryptsToTheOriginalPassword() {
        val plain = decrypt(encrypt("1234"), kSecKeyAlgorithmRSAEncryptionPKCS1)
        assertNotNull(plain)
        assertEquals("1234", plain.decodeToString())
    }

    @Test
    fun unicodeAndTheMaximumLengthSurviveTheRoundTrip() {
        for (password in listOf("contraseña ñandú 🔒", "a".repeat(245), "p")) {
            assertEquals(password, decrypt(encrypt(password), kSecKeyAlgorithmRSAEncryptionPKCS1)?.decodeToString())
        }
    }

    @Test
    fun theOutputIsNotOaep() {
        assertNull(decrypt(encrypt("1234"), kSecKeyAlgorithmRSAEncryptionOAEPSHA1))
    }
}
