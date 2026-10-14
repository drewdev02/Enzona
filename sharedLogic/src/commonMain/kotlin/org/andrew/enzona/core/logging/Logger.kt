package org.andrew.enzona.core.logging

/**
 * Logger de una categoría (normalmente una clase). Las plantillas se renderizan solo si el nivel
 * está activo, así que dejar `debug(...)` en el código es barato. No hay sobrecarga con lambda:
 * invita a interpolar strings, que se saltaría [Sensitive].
 *
 * En lugar del `BeginScope` de .NET (ligado a un hilo, que no sirve con corrutinas) hay loggers
 * hijos inmutables: `log.with("requestId" to id)`.
 */
class Logger internal constructor(
    val category: String,
    private val context: Map<String, Any?>,
) {
    fun isEnabled(level: LogLevel): Boolean = Logging.isEnabled(level, category)

    fun with(vararg properties: Pair<String, Any?>): Logger =
        Logger(category, context + properties.associate { (k, v) -> k to redact(v) })

    fun log(level: LogLevel, throwable: Throwable?, template: String, vararg args: Any?) {
        if (!isEnabled(level)) return
        val rendered = renderTemplate(template, args)
        Logging.write(
            LogEvent(level, category, rendered.text, template, rendered.arguments, context, throwable),
        )
    }

    fun trace(template: String, vararg args: Any?) = log(LogLevel.TRACE, null, template, *args)
    fun debug(template: String, vararg args: Any?) = log(LogLevel.DEBUG, null, template, *args)
    fun info(template: String, vararg args: Any?) = log(LogLevel.INFO, null, template, *args)
    fun warn(template: String, vararg args: Any?) = log(LogLevel.WARN, null, template, *args)
    fun error(template: String, vararg args: Any?) = log(LogLevel.ERROR, null, template, *args)
    fun critical(template: String, vararg args: Any?) = log(LogLevel.CRITICAL, null, template, *args)

    fun warn(throwable: Throwable, template: String, vararg args: Any?) =
        log(LogLevel.WARN, throwable, template, *args)

    fun error(throwable: Throwable, template: String, vararg args: Any?) =
        log(LogLevel.ERROR, throwable, template, *args)

    fun critical(throwable: Throwable, template: String, vararg args: Any?) =
        log(LogLevel.CRITICAL, throwable, template, *args)
}

fun logger(category: String): Logger = Logger(category, emptyMap())

/**
 * Categoría = nombre simple de la clase. Si algún día se activa R8/minify, estos nombres se
 * ofuscan: habría que conservarlos con una regla keep o usar [logger] con un nombre explícito.
 */
inline fun <reified T : Any> logger(): Logger = logger(T::class.simpleName ?: "Unknown")
