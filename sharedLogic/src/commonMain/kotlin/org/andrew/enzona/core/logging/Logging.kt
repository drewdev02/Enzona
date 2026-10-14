package org.andrew.enzona.core.logging

import kotlin.concurrent.Volatile

/** Un evento ya renderizado. [arguments] sale de la plantilla; [context] del logger hijo. */
data class LogEvent(
    val level: LogLevel,
    val category: String,
    val message: String,
    val template: String,
    val arguments: Map<String, Any?>,
    val context: Map<String, Any?>,
    val throwable: Throwable?,
)

/** Destino de los logs (Logcat, os_log, memoria en los tests...). */
fun interface LogSink {
    fun write(event: LogEvent)
}

/**
 * [minimumLevel] aplica a todo salvo a las categorías de [categoryLevels] (nombre exacto).
 * Sin sinks no se escribe nada: es el estado inicial, así que usar un logger antes de
 * instalar la configuración es inocuo.
 */
class LogConfig(
    val minimumLevel: LogLevel,
    val sinks: List<LogSink>,
    val categoryLevels: Map<String, LogLevel> = emptyMap(),
)

/** Configuración global de logging, como el `ILoggerFactory` de .NET. */
object Logging {
    @Volatile
    private var config: LogConfig = LogConfig(LogLevel.INFO, emptyList())

    fun install(config: LogConfig) {
        this.config = config
    }

    /** Vuelve al estado silencioso (para tests). */
    fun reset() {
        config = LogConfig(LogLevel.INFO, emptyList())
    }

    internal fun isEnabled(level: LogLevel, category: String): Boolean {
        val c = config
        return c.sinks.isNotEmpty() && level >= (c.categoryLevels[category] ?: c.minimumLevel)
    }

    /** Un sink que falla no debe tumbar la app ni impedir que los demás reciban el evento. */
    internal fun write(event: LogEvent) {
        for (sink in config.sinks) {
            try {
                sink.write(event)
            } catch (_: Exception) {
            }
        }
    }
}
