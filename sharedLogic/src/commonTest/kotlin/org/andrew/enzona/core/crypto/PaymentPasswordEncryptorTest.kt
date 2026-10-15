package org.andrew.enzona.core.crypto

import kotlin.io.encoding.Base64
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import org.andrew.enzona.core.result.EzResult
import org.andrew.enzona.core.result.Failure

class PaymentPasswordEncryptorTest {
    private val encryptor = RsaPaymentPasswordEncryptor()

    private fun encrypted(password: String): ByteArray {
        val result = encryptor.encrypt(password)
        assertIs<EzResult.Success<String>>(result)
        return Base64.decode(result.value)
    }

    private fun assertCrypto(result: EzResult<*>) {
        assertIs<EzResult.Error>(result)
        assertIs<Failure.Crypto>(result.failure)
    }

    // ---- La clave del original ----

    @Test
    fun theBundledKeyIsAnRsa2048KeyWithRoomForPkcs1v15() {
        val key = (parseRsaPublicKeyPem(ENZONA_PUBLIC_KEY_PEM) as EzResult.Success).value
        assertEquals(256, key.modulusBytes)
        assertEquals(245, key.maxPlaintextBytes)
        // RSAPublicKey (PKCS#1) empieza por SEQUENCE y es lo que hay dentro del BIT STRING del SPKI.
        assertEquals(0x30, key.pkcs1[0].toInt())
        assertEquals(270, key.pkcs1.size)
        assertContentEquals(key.pkcs1, key.spki.copyOfRange(key.spki.size - key.pkcs1.size, key.spki.size))
    }

    @Test
    fun theKeyParsesWithWindowsLineEndingsAndSurroundingSpace() {
        val crlf = "\n  " + ENZONA_PUBLIC_KEY_PEM.replace("\n", "\r\n") + "\n\n"
        assertIs<EzResult.Success<RsaPublicKey>>(parseRsaPublicKeyPem(crlf))
    }

    // ---- Cifrado ----

    @Test
    fun theResultIsBase64OfOneModulusWorthOfBytes() {
        val bytes = encrypted("1234")
        assertEquals(256, bytes.size)
    }

    @Test
    fun paddingIsRandomSoTheSamePasswordEncryptsDifferently() {
        assertNotEquals(encryptor.encrypt("1234"), encryptor.encrypt("1234"))
    }

    @Test
    fun theLongestPasswordThatFitsIsAcceptedAndOneMoreByteIsNot() {
        assertEquals(256, encrypted("a".repeat(245)).size)
        assertCrypto(encryptor.encrypt("a".repeat(246)))
    }

    @Test
    fun lengthIsMeasuredInBytesNotCharacters() {
        // "ñ" ocupa 2 bytes en UTF-8: 122 caracteres = 244 bytes caben; 123 = 246 no.
        assertEquals(256, encrypted("ñ".repeat(122)).size)
        assertCrypto(encryptor.encrypt("ñ".repeat(123)))
    }

    @Test
    fun anEmptyPasswordIsRejected() {
        assertCrypto(encryptor.encrypt(""))
    }

    @Test
    fun failuresNeverCarryThePassword() {
        val secret = "contraseña-secreta-" + "x".repeat(300)
        val result = encryptor.encrypt(secret) as EzResult.Error
        assertTrue(secret !in result.failure.toString())
        assertTrue("secreta" !in result.failure.toString())
    }

    @Test
    fun aBrokenKeyFailsEveryEncryptionInsteadOfThrowing() {
        val broken = RsaPaymentPasswordEncryptor("no es una clave")
        assertCrypto(broken.encrypt("1234"))
        assertCrypto(broken.encrypt("1234"))
    }

    // ---- Interpretación de la clave: se rechaza lo que no es lo esperado ----

    private fun pem(base64: String) = "-----BEGIN PUBLIC KEY-----\n$base64\n-----END PUBLIC KEY-----"

    private val rsa1024 = pem(
        "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQC8hc/VNLgnHZ31w6sUTKnEOSh0At00eJLYhQaAPiN5NfM5NcXBkz5qw7Pf" +
            "tkgrXJIek56k5pCMBH6WmM1kmIa7iTaO/9+yQtyYg8gyz3+h0fMVygIzqcIFjMnz3ei+K6PIgMRi3SS8Obbu8el53k+/9Fl3" +
            "D/hJaepIzd+Ic4GhXwIDAQAB",
    )
    private val ecKey = pem(
        "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEBaV4+khwxfsehxU0FPoQ/8LN613N" +
            "pmlhBb3IadRxdaeG8eap7pNtm41uAXFDr3UbA5GrFvxR7RnrczhR6MVvzw==",
    )

    @Test
    fun aWeakRsaKeyIsRejected() = assertCrypto(parseRsaPublicKeyPem(rsa1024))

    @Test
    fun aKeyOfAnotherAlgorithmIsRejected() = assertCrypto(parseRsaPublicKeyPem(ecKey))

    @Test
    fun otherPemShapesAreRejected() {
        val body = ENZONA_PUBLIC_KEY_PEM.lines().drop(1).dropLast(1).joinToString("\n")
        assertCrypto(parseRsaPublicKeyPem(""))
        assertCrypto(parseRsaPublicKeyPem(body)) // sin cabecera
        assertCrypto(parseRsaPublicKeyPem("-----BEGIN RSA PUBLIC KEY-----\n$body\n-----END RSA PUBLIC KEY-----"))
        assertCrypto(parseRsaPublicKeyPem(ENZONA_PUBLIC_KEY_PEM + "\n" + ENZONA_PUBLIC_KEY_PEM)) // dos bloques
        assertCrypto(parseRsaPublicKeyPem(pem("esto no es base64!!")))
    }

    private fun der(): ByteArray = (parseRsaPublicKeyPem(ENZONA_PUBLIC_KEY_PEM) as EzResult.Success).value.spki

    @Test
    fun truncatedOrExtendedDerIsRejected() {
        val good = der()
        assertIs<EzResult.Success<RsaPublicKey>>(parseRsaPublicKeyDer(good))
        assertCrypto(parseRsaPublicKeyDer(good.copyOf(good.size - 1)))
        assertCrypto(parseRsaPublicKeyDer(good + byteArrayOf(0)))
        assertCrypto(parseRsaPublicKeyDer(ByteArray(0)))
        assertCrypto(parseRsaPublicKeyDer(good.copyOf(10)))
    }

    @Test
    fun aCorruptedStructureIsRejectedNotReinterpreted() {
        fun mutate(index: Int, value: Int) = der().also { it[index] = value.toByte() }
        assertCrypto(parseRsaPublicKeyDer(mutate(0, 0x31)))       // ya no es SEQUENCE
        assertCrypto(parseRsaPublicKeyDer(mutate(6, 0x07)))       // el OID deja de ser OID
        assertCrypto(parseRsaPublicKeyDer(mutate(17, 0x02)))      // un OID distinto de rsaEncryption
        assertCrypto(parseRsaPublicKeyDer(mutate(24, 0x01)))      // BIT STRING con bits sobrantes
        assertCrypto(parseRsaPublicKeyDer(mutate(2, 0x80)))       // longitud indefinida
    }

    @Test
    fun anEvenExponentIsRejected() {
        val bad = der()
        bad[bad.size - 1] = 0x00 // 65537 -> 65536
        assertCrypto(parseRsaPublicKeyDer(bad))
    }
}
