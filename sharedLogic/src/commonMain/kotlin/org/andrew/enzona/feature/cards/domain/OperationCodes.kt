package org.andrew.enzona.feature.cards.domain

/**
 * La acción de un movimiento tal como la da el backend (`DB` débito, `CR` crédito). Cualquier otro valor se conserva
 * en [Other] en vez de adivinarse: quien clasifica decide qué hacer con lo que no reconoce.
 */
sealed interface OperationAction {
    data object Debit : OperationAction
    data object Credit : OperationAction
    data class Other(val raw: String) : OperationAction
}

/** El canal de un movimiento (`REV` reverso, `EV` evento/nómina, `POS`, `ENZ`). Lo que no se reconoce conserva su texto. */
sealed interface OperationChannel {
    data object Reversal : OperationChannel
    data object Event : OperationChannel
    data object Pos : OperationChannel
    data object Enzona : OperationChannel
    data class Other(val raw: String) : OperationChannel
}

/**
 * Sentido de un movimiento. Como la app original, SOLO un débito explícito es débito: una acción ausente o no
 * reconocida se muestra como crédito (el importe llega como magnitud y el sentido sale únicamente de aquí).
 */
val OperationAction.direction: OperationDirection
    get() = if (this == OperationAction.Debit) OperationDirection.DEBIT else OperationDirection.CREDIT
