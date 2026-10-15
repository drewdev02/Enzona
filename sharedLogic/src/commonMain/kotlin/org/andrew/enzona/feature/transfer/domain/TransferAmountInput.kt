package org.andrew.enzona.feature.transfer.domain

import org.andrew.enzona.core.money.parseMinorUnits

/** Qué está mal en el importe tecleado. Los textos son de la UI. */
enum class AmountProblem {
    EMPTY,

    /** La app original lo rechaza con un aviso propio: "El monto no puede contener espacios en blanco". */
    HAS_WHITESPACE,

    /** No es un decimal válido: letras, coma, más de dos decimales, notación científica, negativo, o desborda. */
    INVALID,

    /** Es cero ("El monto no puede ser $0.00"). */
    ZERO,
}

/** Resultado de leer el importe tecleado: unidades menores (centavos) o el problema. */
sealed interface AmountInput {
    data class Valid(val minorUnits: Long) : AmountInput
    data class Problem(val problem: AmountProblem) : AmountInput
}

/**
 * Lee el importe tal como lo teclea el usuario, con las comprobaciones de la app original: no vacío, sin espacios,
 * un decimal válido y mayor que cero. No decide máximos ni saldo: eso es del servidor. Solo acepta dígitos y un
 * punto decimal (nada de signo, coma ni símbolos): lo que se ve es lo que se envía.
 */
fun parseAmountInput(text: String): AmountInput {
    if (text.isEmpty()) return AmountInput.Problem(AmountProblem.EMPTY)
    if (text.any { it.isWhitespace() }) return AmountInput.Problem(AmountProblem.HAS_WHITESPACE)
    // Solo dígitos ASCII: `isDigit()` aceptaría también dígitos de otros alfabetos.
    if (text.any { it !in '0'..'9' && it != '.' }) return AmountInput.Problem(AmountProblem.INVALID)
    val minorUnits = parseMinorUnits(text) ?: return AmountInput.Problem(AmountProblem.INVALID)
    if (minorUnits == 0L) return AmountInput.Problem(AmountProblem.ZERO)
    return AmountInput.Valid(minorUnits)
}
