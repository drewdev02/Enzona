package org.andrew.enzona.core.crypto

import kotlin.io.encoding.Base64
import org.andrew.enzona.core.result.EzResult
import org.andrew.enzona.core.result.Failure

/**
 * Clave pública RSA ya interpretada: este es el único sitio que lee la PEM y el DER. Cada plataforma recibe la
 * forma que su API acepta (Android: X.509 `SubjectPublicKeyInfo`; iOS: `RSAPublicKey` PKCS#1) sin volver a
 * interpretar los bytes originales.
 */
class RsaPublicKey internal constructor(
    /** `SubjectPublicKeyInfo` (X.509) en DER. */
    val spki: ByteArray,
    /** `RSAPublicKey` (PKCS#1) en DER: el contenido del BIT STRING del SPKI. */
    val pkcs1: ByteArray,
    /** Tamaño del módulo en bytes (256 para RSA-2048). */
    val modulusBytes: Int,
) {
    /** Lo máximo que cabe con relleno PKCS#1 v1.5: el módulo menos 11 bytes de relleno. */
    val maxPlaintextBytes: Int get() = modulusBytes - PKCS1_V15_OVERHEAD

    private companion object {
        const val PKCS1_V15_OVERHEAD = 11
    }
}

private const val MIN_MODULUS_BITS = 2048
private const val PEM_BEGIN = "-----BEGIN PUBLIC KEY-----"
private const val PEM_END = "-----END PUBLIC KEY-----"

private const val TAG_INTEGER = 0x02
private const val TAG_BIT_STRING = 0x03
private const val TAG_NULL = 0x05
private const val TAG_OID = 0x06
private const val TAG_SEQUENCE = 0x30

/** OID 1.2.840.113549.1.1.1 (rsaEncryption), solo el contenido. */
private val RSA_ENCRYPTION_OID = byteArrayOf(0x2A, 0x86.toByte(), 0x48, 0x86.toByte(), 0xF7.toByte(), 0x0D, 0x01, 0x01, 0x01)

private fun bad(what: String): EzResult.Error = EzResult.Error(Failure.Crypto("Clave pública no válida: $what"))

/** Un elemento DER: [tag] y el rango `[start, end)` de su contenido dentro del arreglo. */
private class Der(val tag: Int, val start: Int, val end: Int)

/** Lee el elemento DER que empieza en [at]. Solo longitud definida, como exige DER. */
private fun readDer(bytes: ByteArray, at: Int, limit: Int): Der? {
    if (at + 2 > limit) return null
    val tag = bytes[at].toInt() and 0xFF
    val first = bytes[at + 1].toInt() and 0xFF
    var cursor = at + 2
    val length: Int
    if (first < 0x80) {
        length = first
    } else {
        val count = first and 0x7F
        // 0x80 = longitud indefinida (no es DER); más de 3 bytes de longitud no cabe en una clave real.
        if (count == 0 || count > 3 || cursor + count > limit) return null
        var value = 0
        repeat(count) { value = (value shl 8) or (bytes[cursor++].toInt() and 0xFF) }
        length = value
    }
    if (length < 0 || cursor + length > limit) return null
    return Der(tag, cursor, cursor + length)
}

/** Interpreta una clave pública RSA en PEM (`BEGIN PUBLIC KEY`, formato SPKI). */
fun parseRsaPublicKeyPem(pem: String): EzResult<RsaPublicKey> {
    val text = pem.trim()
    if (!text.startsWith(PEM_BEGIN) || !text.endsWith(PEM_END)) return bad("no es una PEM de clave pública")
    val body = text.removePrefix(PEM_BEGIN).removeSuffix(PEM_END)
    if (PEM_BEGIN in body || PEM_END in body) return bad("la PEM trae más de un bloque")
    val der = try {
        Base64.decode(body.filterNot { it.isWhitespace() })
    } catch (_: IllegalArgumentException) {
        return bad("base64 ilegible")
    }
    return parseRsaPublicKeyDer(der)
}

/** Interpreta un `SubjectPublicKeyInfo` RSA en DER. Rechaza cualquier otra cosa en vez de adivinar. */
fun parseRsaPublicKeyDer(der: ByteArray): EzResult<RsaPublicKey> {
    val spki = readDer(der, 0, der.size)
    if (spki == null || spki.tag != TAG_SEQUENCE || spki.end != der.size) return bad("estructura SPKI")

    val algorithm = readDer(der, spki.start, spki.end)
    if (algorithm == null || algorithm.tag != TAG_SEQUENCE) return bad("algoritmo ausente")
    val oid = readDer(der, algorithm.start, algorithm.end)
    if (oid == null || oid.tag != TAG_OID || !der.copyOfRange(oid.start, oid.end).contentEquals(RSA_ENCRYPTION_OID)) {
        return bad("el algoritmo no es RSA")
    }
    val parameters = readDer(der, oid.end, algorithm.end)
    if (parameters == null || parameters.tag != TAG_NULL || parameters.end != algorithm.end) return bad("parámetros del algoritmo")

    val bitString = readDer(der, algorithm.end, spki.end)
    if (bitString == null || bitString.tag != TAG_BIT_STRING || bitString.end != spki.end) return bad("clave ausente")
    // El primer byte del BIT STRING cuenta los bits sobrantes: una clave RSA no deja ninguno.
    if (bitString.end - bitString.start < 2 || der[bitString.start].toInt() != 0) return bad("BIT STRING de la clave")
    val pkcs1Start = bitString.start + 1

    val rsaKey = readDer(der, pkcs1Start, bitString.end)
    if (rsaKey == null || rsaKey.tag != TAG_SEQUENCE || rsaKey.end != bitString.end) return bad("estructura PKCS#1")
    val modulus = readDer(der, rsaKey.start, rsaKey.end)
    if (modulus == null || modulus.tag != TAG_INTEGER) return bad("módulo ausente")
    val exponent = readDer(der, modulus.end, rsaKey.end)
    if (exponent == null || exponent.tag != TAG_INTEGER || exponent.end != rsaKey.end) return bad("exponente ausente")

    // El módulo es un entero positivo: un 0x00 inicial solo evita que se lea como negativo.
    var first = modulus.start
    if (first < modulus.end && der[first].toInt() == 0) first++
    val modulusBytes = modulus.end - first
    // Vacío, o con ceros de más (DER no los admite): no se adivina qué tamaño quiso decir.
    if (modulusBytes == 0 || der[first].toInt() == 0) return bad("módulo")
    val leadingZeroBits = der[first].toInt().and(0xFF).countLeadingZeroBits() - 24
    if (modulusBytes * 8 - leadingZeroBits < MIN_MODULUS_BITS) return bad("el módulo es menor de $MIN_MODULUS_BITS bits")
    // Exponente impar y no trivial (el habitual es 65537).
    val exponentLast = der[exponent.end - 1].toInt()
    if (exponent.end - exponent.start == 0 || exponentLast and 1 == 0) return bad("exponente")
    if (exponent.end - exponent.start == 1 && exponentLast < 3) return bad("exponente")

    return EzResult.Success(
        RsaPublicKey(
            spki = der.copyOf(),
            pkcs1 = der.copyOfRange(pkcs1Start, bitString.end),
            modulusBytes = modulusBytes,
        ),
    )
}
