package org.andrew.enzona.core.logging

/** Sink en memoria para que los tests comprueben qué se registró. */
class RecordingSink : LogSink {
    val events = mutableListOf<LogEvent>()
    override fun write(event: LogEvent) {
        events += event
    }

    /** Todo lo que un destino real podría imprimir de un evento, en un solo texto. */
    fun everythingWritten(): String = events.joinToString("\n") { e ->
        listOf(e.message, e.template, e.arguments.toString(), e.context.toString(), e.throwable?.stackTraceToString().orEmpty())
            .joinToString(" | ")
    }
}

/** Instala una configuración durante [block] y vuelve al estado silencioso (el logging es global). */
inline fun <T> withLogging(
    minimumLevel: LogLevel = LogLevel.TRACE,
    categoryLevels: Map<String, LogLevel> = emptyMap(),
    vararg extraSinks: LogSink,
    block: (RecordingSink) -> T,
): T {
    val sink = RecordingSink()
    Logging.install(LogConfig(minimumLevel, listOf(sink) + extraSinks, categoryLevels))
    try {
        return block(sink)
    } finally {
        Logging.reset()
    }
}
