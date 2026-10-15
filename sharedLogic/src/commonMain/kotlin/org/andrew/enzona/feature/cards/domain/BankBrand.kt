package org.andrew.enzona.feature.cards.domain

/** Un banco que reconocemos. Cada plataforma decide qué logo le corresponde; el dominio solo dice cuál es. */
enum class BankBrand {
    BANMET,
    BANDEC,
    BPA,
    BICSA,
    CLASICA,
    AIS,
    UNKNOWN,
}

/**
 * LA tabla de códigos de banco (leída del despachador del original): 02 AIS · 03 Clásica · 04 BICSA · 05 y 95
 * Metropolitano · 06 Bandec · 12 BPA. Es la única: los skins de tarjeta se derivan de aquí ([skinFor]).
 */
private val CODES = mapOf(
    "02" to BankBrand.AIS,
    "03" to BankBrand.CLASICA,
    "04" to BankBrand.BICSA,
    "05" to BankBrand.BANMET,
    "95" to BankBrand.BANMET,
    "06" to BankBrand.BANDEC,
    "12" to BankBrand.BPA,
)

/** Los códigos que la tabla reconoce, para comprobar (en tests) que todo lo que se dibuja sale de ella. */
internal val KNOWN_BANK_CODES: Set<String> get() = CODES.keys

/** El banco a partir de SOLO su código (`95`), sin intentar leer etiquetas ni nombres. [BankBrand.UNKNOWN] si no está. */
fun bankBrandOfCode(code: String?): BankBrand = CODES[code?.trim()] ?: BankBrand.UNKNOWN

/**
 * El banco a partir del texto que da el backend: el código (`95`), la etiqueta (`BANMET`) o el nombre
 * (`Banco Metropolitano S.A`); no se ha visto cuál de los tres llega en las transferencias. Los códigos salen de la
 * misma tabla que usan los skins de tarjeta. Si no se reconoce es [BankBrand.UNKNOWN]: no se adivina.
 */
fun bankBrandOf(bank: String?): BankBrand {
    val text = bank?.trim().orEmpty()
    if (text.isEmpty()) return BankBrand.UNKNOWN
    bankBrandOfCode(text).takeIf { it != BankBrand.UNKNOWN }?.let { return it }
    val upper = text.uppercase().map { ACCENTS[it] ?: it }.joinToString("")
    // Siglas cortas como palabra entera: "AIS" o "BPA" no deben coincidir dentro de otra palabra.
    val words = upper.split(Regex("[^A-Z0-9]+")).filter { it.isNotEmpty() }.toSet()
    return when {
        "BANMET" in words || "METROPOLITANO" in upper -> BankBrand.BANMET
        "BANDEC" in words || "CREDITO Y COMERCIO" in upper -> BankBrand.BANDEC
        "BPA" in words || "POPULAR DE AHORRO" in upper -> BankBrand.BPA
        "BICSA" in words -> BankBrand.BICSA
        "CLASICA" in words || "FINCIMEX" in words -> BankBrand.CLASICA
        "AIS" in words || "AMERICAN INTERNATIONAL" in upper -> BankBrand.AIS
        else -> BankBrand.UNKNOWN
    }
}

private val ACCENTS = mapOf('Á' to 'A', 'É' to 'E', 'Í' to 'I', 'Ó' to 'O', 'Ú' to 'U', 'Ü' to 'U')
