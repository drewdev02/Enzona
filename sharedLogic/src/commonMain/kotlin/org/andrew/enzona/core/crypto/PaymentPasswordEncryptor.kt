package org.andrew.enzona.core.crypto

import kotlin.io.encoding.Base64
import org.andrew.enzona.core.result.EzResult
import org.andrew.enzona.core.result.Failure

/**
 * Protege la contraseña de pago antes de enviarla: RSA con la clave pública de EnZona y relleno PKCS#1 v1.5, en
 * base64 (lo que hace la app original; ver `docs/enzona-kmp/08`). El relleno es aleatorio: dos cifrados de la
 * misma contraseña son distintos. Nunca se registra la contraseña ni el cifrado.
 */
interface PaymentPasswordEncryptor {
    fun encrypt(password: String): EzResult<String>
}

class RsaPaymentPasswordEncryptor(pem: String = ENZONA_PUBLIC_KEY_PEM) : PaymentPasswordEncryptor {
    // La clave se interpreta una sola vez, al construir; un fallo se devuelve en cada `encrypt`, no se lanza.
    private val key: EzResult<RsaPublicKey> = parseRsaPublicKeyPem(pem)

    override fun encrypt(password: String): EzResult<String> {
        val publicKey = when (val k = key) {
            is EzResult.Success -> k.value
            is EzResult.Error -> return k
        }
        if (password.isEmpty()) return EzResult.Error(Failure.Crypto("La contraseña de pago está vacía"))
        val plaintext = password.encodeToByteArray()
        if (plaintext.size > publicKey.maxPlaintextBytes) {
            return EzResult.Error(Failure.Crypto("La contraseña de pago es demasiado larga"))
        }
        return when (val encrypted = rsaEncryptPkcs1v15(publicKey, plaintext)) {
            is EzResult.Success -> {
                // Un cifrado RSA mide siempre lo que el módulo; otra cosa es una plataforma que no hizo lo pedido.
                if (encrypted.value.size != publicKey.modulusBytes) {
                    EzResult.Error(Failure.Crypto("El cifrado devolvió un tamaño inesperado"))
                } else {
                    EzResult.Success(Base64.encode(encrypted.value))
                }
            }
            is EzResult.Error -> encrypted
        }
    }
}

/** RSA PKCS#1 v1.5 con la API de cada plataforma. Recibe la clave ya interpretada; no valida el tamaño del texto. */
internal expect fun rsaEncryptPkcs1v15(key: RsaPublicKey, plaintext: ByteArray): EzResult<ByteArray>
