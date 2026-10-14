package org.andrew.enzona.core.logging

import co.touchlab.kermit.LogWriter

/** Escritor de la plataforma: Logcat en Android, os_log en iOS. */
internal expect fun defaultLogWriter(debug: Boolean): LogWriter

/**
 * Configuración por defecto de las apps, llamada una vez al arrancar.
 * Debug: desde Debug. Release: desde Info (y en iOS os_log oculta los mensajes fuera del depurador).
 */
fun installLogging(debug: Boolean) {
    Logging.install(
        LogConfig(
            minimumLevel = if (debug) LogLevel.DEBUG else LogLevel.INFO,
            sinks = listOf(KermitSink(defaultLogWriter(debug))),
        ),
    )
}
