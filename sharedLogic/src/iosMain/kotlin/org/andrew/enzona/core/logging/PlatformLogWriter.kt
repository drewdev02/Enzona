package org.andrew.enzona.core.logging

import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.OSLogWriter

/**
 * Kermit trata todo como cadena dinámica: sin `publicLogging`, iOS muestra `<private>` fuera del
 * depurador. En debug se hace público para poder leerlo; en release se deja privado a propósito.
 */
internal actual fun defaultLogWriter(debug: Boolean): LogWriter =
    OSLogWriter(subsystem = "org.andrew.enzona", category = "app", publicLogging = debug)
