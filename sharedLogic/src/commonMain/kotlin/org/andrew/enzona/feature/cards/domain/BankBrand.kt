package org.andrew.enzona.feature.cards.domain

/** Un banco que reconocemos. Cada plataforma decide qué logo le corresponde; el dominio solo dice cuál es. */
enum class BankBrand { BANMET, BANDEC, BPA, BICSA, CLASICA, AIS, UNKNOWN }

private val CODES = mapOf(
    "02" to BankBrand.AIS,
    "03" to BankBrand.CLASICA,
    "04" to BankBrand.BICSA,
    "05" to BankBrand.BANMET,
    "95" to BankBrand.BANMET,
    "06" to BankBrand.BANDEC,
    "12" to BankBrand.BPA,
)

/**
 * El banco a partir del texto que da el backend: el código (`95`), la etiqueta (`BANMET`) o el nombre
 * (`Banco Metropolitano S.A`); no se ha visto cuál de los tres llega en las transferencias. Mismos códigos que
 * la tabla de skins de tarjeta. Si no se reconoce es [BankBrand.UNKNOWN]: no se adivina.
 */
fun bankBrandOf(bank: String?): BankBrand {
    val text = bank?.trim().orEmpty()
    if (text.isEmpty()) return BankBrand.UNKNOWN
    CODES[text]?.let { return it }
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
