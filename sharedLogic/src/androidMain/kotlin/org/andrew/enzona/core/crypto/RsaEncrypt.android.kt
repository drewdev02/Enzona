package org.andrew.enzona.core.crypto

import java.security.GeneralSecurityException
import java.security.KeyFactory
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import org.andrew.enzona.core.result.EzResult
import org.andrew.enzona.core.result.Failure

internal actual fun rsaEncryptPkcs1v15(key: RsaPublicKey, plaintext: ByteArray): EzResult<ByteArray> = try {
    val publicKey = KeyFactory.getInstance("RSA").generatePublic(X509EncodedKeySpec(key.spki))
    // El relleno se pide por su nombre: el valor por defecto de "RSA" depende del proveedor.
    val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
    cipher.init(Cipher.ENCRYPT_MODE, publicKey)
    EzResult.Success(cipher.doFinal(plaintext))
} catch (_: GeneralSecurityException) {
    // Sin el mensaje de la excepción: no se arrastra nada del texto a proteger.
    EzResult.Error(Failure.Crypto("RSA falló en Android"))
}
