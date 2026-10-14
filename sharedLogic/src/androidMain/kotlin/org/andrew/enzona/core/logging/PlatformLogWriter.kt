package org.andrew.enzona.core.logging

import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.LogcatWriter

internal actual fun defaultLogWriter(debug: Boolean): LogWriter = LogcatWriter()
