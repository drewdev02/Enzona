package org.andrew.enzona.core.crypto

import java.security.GeneralSecurityException
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.util.Base64
import javax.crypto.Cipher
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import org.andrew.enzona.core.result.EzResult

/** Prueba real del cifrado en Android: lo que sale se descifra con la clave privada y el relleno PKCS#1 v1.5. */
class RsaEncryptRoundTripTest {
    private val pair: KeyPair = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()

    private val pem: String = "-----BEGIN PUBLIC KEY-----\n" +
        Base64.getMimeEncoder(64, "\n".toByteArray()).encodeToString(pair.public.encoded) +
        "\n-----END PUBLIC KEY-----"

    private fun cipher(transformation: String): Cipher =
        Cipher.getInstance(transformation).apply { init(Cipher.DECRYPT_MODE, pair.private) }

    private fun encrypt(password: String): ByteArray {
        val result = RsaPaymentPasswordEncryptor(pem).encrypt(password)
        assertIs<EzResult.Success<String>>(result)
        return Base64.getDecoder().decode(result.value)
    }

    @Test
    fun decryptsToTheOriginalPassword() {
        assertEquals("1234", String(cipher("RSA/ECB/PKCS1Padding").doFinal(encrypt("1234"))))
    }

    @Test
    fun unicodeAndTheMaximumLengthSurviveTheRoundTrip() {
        for (password in listOf("contraseña ñandú 🔒", "a".repeat(245), "p".repeat(1))) {
            assertEquals(password, String(cipher("RSA/ECB/PKCS1Padding").doFinal(encrypt(password)), Charsets.UTF_8))
        }
    }

    @Test
    fun theOutputIsNotOaep() {
        // Si el relleno fuera OAEP, un descifrado OAEP lo aceptaría. Con PKCS#1 v1.5 debe fallar.
        assertFailsWith<GeneralSecurityException> {
            cipher("RSA/ECB/OAEPWithSHA-1AndMGF1Padding").doFinal(encrypt("1234"))
        }
    }
}
