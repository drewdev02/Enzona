package org.andrew.enzona.feature.cards.domain

/** Color de tinta de un texto sobre un skin. Cada plataforma lo traduce a su color. */
enum class CardInk { BLACK, SOFT_BLACK, WHITE }

/**
 * Aspecto de una tarjeta: una imagen completa por banco (logo, fondo y franja del número).
 * El nombre en minúsculas es la clave del recurso (`BANDEC_USD` -> `card_skin_bandec_usd`) en
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
 *   fotográficos donde ninguna tinta llega a 3:1 (la playa de Bandec USD).
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
    BANDEC_USD(number = CardInk.BLACK, text = CardInk.BLACK, menu = CardInk.WHITE, menuScrim = true),
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
 * Skin según el código de banco (tabla leída del despachador del original):
 * 02 AIS · 03 Clásica · 04 BICSA · 05 y 95 Metropolitano · 06 Bandec · 12 BPA.
 *
 * Bandec tiene dos skins; el original decide con una bandera calculada fuera del despachador
 * (se infiere que es la moneda: en los datos reales las Bandec son CUP y USD).
 */
fun skinFor(bankCode: String?, currency: String?): CardSkin = when (bankCode?.trim()) {
    "02" -> CardSkin.AIS
    "03" -> CardSkin.CLASICA
    "04" -> CardSkin.BICSA
    "05", "95" -> CardSkin.BANMET
    "06" -> if (currency.equals("usd", ignoreCase = true)) CardSkin.BANDEC_USD else CardSkin.BANDEC
    "12" -> CardSkin.BPA
    else -> CardSkin.GENERIC
}
