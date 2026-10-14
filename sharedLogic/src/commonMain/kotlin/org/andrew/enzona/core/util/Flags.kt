package org.andrew.enzona.core.util

/**
 * El backend expresa los booleanos de varias formas ("1", "true", `true`, 1) y usa `false` o
 * ausencia para "no". Se leen como String y se normalizan AQUÍ, una sola vez.
 */
fun String?.asFlag(): Boolean = this == "1" || this.equals("true", ignoreCase = true)
