package org.andrew.enzona.core.logging

import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Severity

/**
 * Entrega los eventos a un [LogWriter] de Kermit (Logcat en Android, os_log en iOS). El filtrado
 * ya lo hizo [Logging]; aquí solo se traduce el formato.
 *
 * CRITICAL se envía como Error con prefijo: el `Assert` de Kermit llama a `Log.wtf` en Android,
 * que según el dispositivo puede terminar el proceso.
 */
internal class KermitSink(private val writer: LogWriter) : LogSink {
    override fun write(event: LogEvent) {
        val severity = when (event.level) {
            LogLevel.TRACE -> Severity.Verbose
            LogLevel.DEBUG -> Severity.Debug
            LogLevel.INFO -> Severity.Info
            LogLevel.WARN -> Severity.Warn
            LogLevel.ERROR, LogLevel.CRITICAL -> Severity.Error
        }
        val prefix = if (event.level == LogLevel.CRITICAL) "[CRITICAL] " else ""
        val context = if (event.context.isEmpty()) "" else
            event.context.entries.joinToString(prefix = " [", postfix = "]", separator = ", ") { "${it.key}=${it.value}" }
        writer.log(severity, prefix + event.message + context, event.category, event.throwable)
    }
}
