package org.andrew.enzona.core.logging

/**
 * Marca un valor como secreto: en cualquier plantilla o contexto de log se imprime `***`.
 * Redactar es una decisión de tipos, no de disciplina: envuelve el dato al pasarlo al logger.
 */
class Sensitive<out T>(val value: T) {
    override fun toString(): String = REDACTED

    internal companion object {
        const val REDACTED = "***"
    }
}
