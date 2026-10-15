package org.andrew.enzona.feature.cards.domain

/** Color de tinta de un texto sobre un skin. Cada plataforma lo traduce a su color. */
enum class CardInk { BLACK, SOFT_BLACK, WHITE }

/**
 * Aspecto de una tarjeta: una imagen completa por banco (logo, fondo y franja del número).
 * El nombre en minúsculas es la clave del recurso (`BANDEC_TROPICAL` -> `card_skin_bandec_tropical`) en
 * Android e iOS; `tools/extract_assets.py` las importa y `tools/check_assets.py` lo comprueba.
 *
 * Las tintas NO son iguales en todos los skins (hay uno dorado, uno verde oscuro, una playa...) y
 * se eligieron MIDIENDO los píxeles bajo cada texto: `tools/check_card_skins.py` las verifica
 * (texto >= 4.5:1, número >= 3:1 por ser texto grande, menú >= 3:1). Se conserva la tinta del
 * original cuando cumple; si no cumple se usa la mejor medida (Bandec: el número negro sobre la
 * franja roja daba ~2.3:1). Si cambias una tinta, ejecuta ese script.
 *
 * - [number]: "**** **** **** 1234".  - [text]: nombre, moneda y vencimiento.
 * - [menu]: icono de tres puntos y check.  - [menuScrim]: velo oscuro tras el icono, para fondos
 *   fotográficos donde ninguna tinta llega a 3:1 (la playa de la Bandec Tropical).
 * - [numberInset]: dónde empieza el número, en fracción del ancho. BPA trae un triángulo dorado a
 *   la izquierda de su caja del número y el texto lo taparía.
 */
enum class CardSkin(
    val number: CardInk,
    val text: CardInk,
    val menu: CardInk,
    val menuScrim: Boolean = false,
    val numberInset: Float = DEFAULT_NUMBER_INSET,
) {
    AIS(number = CardInk.WHITE, text = CardInk.BLACK, menu = CardInk.BLACK),
    BANDEC(number = CardInk.WHITE, text = CardInk.WHITE, menu = CardInk.WHITE),
    /** Bandec Tropical / prepago (la de la playa). La elige el BIN, no la moneda: ver [skinFor]. */
    BANDEC_TROPICAL(number = CardInk.BLACK, text = CardInk.BLACK, menu = CardInk.WHITE, menuScrim = true),
    BANMET(number = CardInk.BLACK, text = CardInk.SOFT_BLACK, menu = CardInk.BLACK),
    BICSA(number = CardInk.BLACK, text = CardInk.SOFT_BLACK, menu = CardInk.BLACK),
    BPA(number = CardInk.BLACK, text = CardInk.SOFT_BLACK, menu = CardInk.BLACK, numberInset = 0.115f),
    CLASICA(number = CardInk.BLACK, text = CardInk.WHITE, menu = CardInk.BLACK),

    /** Banco desconocido: sin imagen; la UI dibuja un fondo propio con texto blanco. */
    GENERIC(number = CardInk.WHITE, text = CardInk.WHITE, menu = CardInk.WHITE);

    private companion object {
        const val DEFAULT_NUMBER_INSET = 0.044f
    }

    /** Nombre del recurso de imagen, o null si el skin se dibuja sin imagen. */
    val imageKey: String? get() = if (this == GENERIC) null else "card_skin_${name.lowercase()}"
}

/**
 * BIN (los 9 primeros dígitos del número) de la tarjeta Bandec **Tropical / prepago**. Es el único dato que el
 * original usa para distinguirla de una Bandec normal: en el despachador de tarjetas, con código de banco `06`,
 * compara el BIN con el entero 924006999 (el mismo prefijo `9240 0699 9` que reconoce al añadir una tarjeta) y
 * dibuja `BandecUsdBankCard`; si no coincide, `BandecBankCard`. La moneda NO interviene. Decodificado del APK v20003
 * (sin validar en vivo con una tarjeta tropical real).
 */
const val BANDEC_TROPICAL_BIN: String = "924006999"

/**
 * Skin según el código de banco y el BIN. Los códigos salen de la tabla única de [bankBrandOfCode]; aquí solo se
 * elige la imagen de cada banco.
 *
 * Bandec tiene dos skins: la normal y la Tropical / prepago ([BANDEC_TROPICAL_BIN]). Un BIN ausente o distinto es
 * una Bandec normal, como en el original; una Bandec en USD con otro BIN NO es tropical.
 */
fun skinFor(bankCode: String?, bin: String?): CardSkin = when (bankBrandOfCode(bankCode)) {
    BankBrand.AIS -> CardSkin.AIS
    BankBrand.CLASICA -> CardSkin.CLASICA
    BankBrand.BICSA -> CardSkin.BICSA
    BankBrand.BANMET -> CardSkin.BANMET
    BankBrand.BANDEC -> if (bin?.trim() == BANDEC_TROPICAL_BIN) CardSkin.BANDEC_TROPICAL else CardSkin.BANDEC
    BankBrand.BPA -> CardSkin.BPA
    BankBrand.UNKNOWN -> CardSkin.GENERIC
}
