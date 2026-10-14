package org.andrew.enzona.feature.auth

import okio.ByteString.Companion.encodeUtf8
import org.andrew.enzona.feature.auth.data.JwtClaims
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class JwtClaimsTest {
    private fun jwt(payloadJson: String): String =
        "header.${payloadJson.encodeUtf8().base64Url().trimEnd('=')}.signature"

    @Test
    fun readsStringClaim() {
        assertEquals("user1", JwtClaims.string(jwt("""{"sub":"user1","given_name":"Test"}"""), "sub"))
    }

    @Test
    fun handlesNonAsciiAndMissingPadding() {
        assertEquals("Ñandú", JwtClaims.string(jwt("""{"given_name":"Ñandú"}"""), "given_name"))
    }

    @Test
    fun missingOrNonStringClaimIsNull() {
        val t = jwt("""{"exp":123,"flag":true}""")
        assertNull(JwtClaims.string(t, "sub"))
        assertNull(JwtClaims.string(t, "exp"))
    }

    @Test
    fun malformedTokensAreNullNotCrashes() {
        assertNull(JwtClaims.string("", "sub"))
        assertNull(JwtClaims.string("solo-una-parte", "sub"))
        assertNull(JwtClaims.string("a.@@@no-es-base64@@@.c", "sub"))
        assertNull(JwtClaims.string(jwt("esto no es json"), "sub"))
    }
}
