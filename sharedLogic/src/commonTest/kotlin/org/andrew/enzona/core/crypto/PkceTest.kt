package org.andrew.enzona.core.crypto

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PkceTest {
    @Test
    fun challengeMatchesRfc7636Vector() {
        // Apéndice B de la RFC 7636.
        assertEquals(
            "E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM",
            Pkce.challengeFor("dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk"),
        )
    }

    @Test
    fun generatedPairIsUrlSafeAndConsistent() {
        val pkce = Pkce.generate()
        assertEquals(43, pkce.verifier.length) // 32 bytes → 43 chars base64url sin padding
        assertTrue(pkce.verifier.all { it.isLetterOrDigit() || it == '-' || it == '_' })
        assertEquals(Pkce.challengeFor(pkce.verifier), pkce.challenge)
    }
}
