package org.andrew.enzona.core.crypto

import okio.ByteString.Companion.encodeUtf8
import okio.ByteString.Companion.toByteString
import kotlin.random.Random

/** Par PKCE (RFC 7636): code_verifier + code_challenge S256 (como usa el APK oficial). */
data class Pkce(val verifier: String, val challenge: String) {
    companion object {
        fun generate(): Pkce {
            val bytes = Random.nextBytes(32)
            val verifier = bytes.toByteString().base64Url().trimPadding()
            val challenge = verifier.encodeUtf8().sha256().base64Url().trimPadding()
            return Pkce(verifier, challenge)
        }

        private fun String.trimPadding(): String = trimEnd('=')
    }
}
