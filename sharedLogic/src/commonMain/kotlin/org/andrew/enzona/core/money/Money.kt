package org.andrew.enzona.core.money

data class Money(
    val minorUnits: Long,
    val currency: String,
)

/**
 * Parsea un texto decimal a unidades menores (centavos, 2 decimales) sin usar Double/Float.
 * Devuelve null si la entrada es nula, vacía, contiene caracteres no numéricos,
 * usa coma en lugar de punto, tiene más de 2 decimales significativos,
 * usa notación científica, o si el valor resultante desborda Long.
 */
fun parseMinorUnits(raw: String?): Long? {
    if (raw == null) return null
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return null

    // Notación científica o caracteres no numéricos previos
    if (trimmed.any { it == 'e' || it == 'E' }) return null

    var idx = 0
    val isNegative: Boolean
    when (trimmed[0]) {
        '-' -> {
            isNegative = true
            idx = 1
        }
        '+' -> {
            isNegative = false
            idx = 1
        }
        else -> {
            isNegative = false
        }
    }

    if (idx >= trimmed.length) return null

    val body = trimmed.substring(idx)
    val dotIndex = body.indexOf('.')

    val intPartStr: String
    val fracPartStr: String

    if (dotIndex < 0) {
        intPartStr = body
        fracPartStr = ""
    } else {
        // No se permite más de un punto decimal
        if (body.indexOf('.', dotIndex + 1) >= 0) return null
        intPartStr = body.substring(0, dotIndex)
        fracPartStr = body.substring(dotIndex + 1)
    }

    // No se permite punto aislado (ej. "." o "+." o "-.")
    if (intPartStr.isEmpty() && fracPartStr.isEmpty()) return null

    // Verificar que ambas partes contengan únicamente dígitos numéricos
    if (intPartStr.any { !it.isDigit() }) return null
    if (fracPartStr.any { !it.isDigit() }) return null

    val centavosStr: String
    when {
        fracPartStr.isEmpty() -> {
            centavosStr = "00"
        }
        fracPartStr.length == 1 -> {
            centavosStr = "${fracPartStr}0"
        }
        fracPartStr.length == 2 -> {
            centavosStr = fracPartStr
        }
        else -> {
            // Más de 2 decimales: los sobrantes deben ser exclusivamente ceros
            for (i in 2 until fracPartStr.length) {
                if (fracPartStr[i] != '0') return null
            }
            centavosStr = fracPartStr.substring(0, 2)
        }
    }

    // Eliminar ceros iniciales de la parte entera
    val trimmedInt = intPartStr.trimStart('0')
    val digitsToProcess = if (trimmedInt.isEmpty()) centavosStr else trimmedInt + centavosStr

    // Acumulación en negativo para soportar Long.MIN_VALUE sin desbordamiento previo
    var result = 0L
    val limit = if (isNegative) Long.MIN_VALUE else -Long.MAX_VALUE
    val multmin = limit / 10L

    for (ch in digitsToProcess) {
        val digit = ch - '0'
        if (result < multmin) return null
        result *= 10L
        if (result < limit + digit) return null
        result -= digit
    }

    return if (isNegative) result else -result
}

/**
 * El importe como texto decimal con punto y dos decimales, sin separador de miles ni símbolo (`1400.00`): la forma en
 * que la API recibe los importes. Trabaja con los dígitos del `Long` (el dinero nunca pasa por `Double`, y `abs` de
 * `Long.MIN_VALUE` desbordaría). Es el inverso de [parseMinorUnits].
 */
fun Money.toDecimalText(): String {
    val negative = minorUnits < 0
    val digits = minorUnits.toString().removePrefix("-").padStart(3, '0')
    val text = digits.dropLast(2) + "." + digits.takeLast(2)
    return if (negative) "-$text" else text
}

/**
 * Las monedas llegan como texto libre (`cup`, `CUP `): se comparan sin distinguir mayúsculas ni espacios en los
 * extremos. Es la ÚNICA comparación de monedas: no repetir `equals(..., ignoreCase = true)` por el código.
 */
fun sameCurrency(a: String?, b: String?): Boolean = a.orEmpty().trim().equals(b.orEmpty().trim(), ignoreCase = true)
