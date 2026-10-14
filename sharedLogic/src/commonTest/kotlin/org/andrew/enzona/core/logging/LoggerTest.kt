package org.andrew.enzona.core.logging

import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Severity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

private class SessionLike

class LoggerTest {
    @Test
    fun usingALoggerBeforeInstallingAConfigurationIsSilentAndHarmless() {
        Logging.reset()
        logger("X").critical("nadie escucha {A}", 1)
    }

    @Test
    fun theCategoryIsTheSimpleClassName() = withLogging { sink ->
        logger<SessionLike>().info("hola")
        assertEquals("SessionLike", sink.events.single().category)
    }

    @Test
    fun eventsBelowTheMinimumLevelAreDropped() = withLogging(minimumLevel = LogLevel.INFO) { sink ->
        val log = logger("C")
        log.debug("no")
        log.info("sí")
        log.error("sí también")
        assertEquals(listOf(LogLevel.INFO, LogLevel.ERROR), sink.events.map { it.level })
    }

    @Test
    fun theTemplateIsNotRenderedWhenTheLevelIsDisabled() = withLogging(minimumLevel = LogLevel.INFO) { sink ->
        var renders = 0
        val costly = object {
            override fun toString(): String { renders++; return "caro" }
        }
        logger("C").debug("valor {V}", costly)
        assertEquals(0, renders)
        assertTrue(sink.events.isEmpty())
    }

    @Test
    fun aCategoryOverrideBeatsTheGlobalMinimum() = withLogging(
        minimumLevel = LogLevel.WARN,
        categoryLevels = mapOf("Network" to LogLevel.DEBUG),
    ) { sink ->
        logger("Network").debug("detalle de red")
        logger("Session").debug("detalle de sesión")
        assertEquals(listOf("Network"), sink.events.map { it.category })
    }

    @Test
    fun childLoggersCarryTheirContextAndLeaveTheParentUntouched() = withLogging { sink ->
        val parent = logger("C")
        val child = parent.with("requestId" to "r1", "secret" to Sensitive("s3cr3t"))
        child.info("con contexto")
        parent.info("sin contexto")
        assertEquals(mapOf<String, Any?>("requestId" to "r1", "secret" to "***"), sink.events[0].context)
        assertTrue(sink.events[1].context.isEmpty())
    }

    @Test
    fun theExceptionTravelsWithTheEvent() = withLogging { sink ->
        val boom = IllegalStateException("boom")
        logger("C").error(boom, "falló {Op}", "carga")
        assertSame(boom, sink.events.single().throwable)
        assertEquals("falló carga", sink.events.single().message)
    }

    @Test
    fun aFailingSinkNeitherThrowsNorStarvesTheOthers() = withLogging(
        extraSinks = arrayOf(LogSink { error("sink roto") }),
    ) { sink ->
        logger("C").info("sigue")
        assertEquals(1, sink.events.size)
    }

    @Test
    fun noSinkMeansNothingIsEnabled() {
        Logging.install(LogConfig(LogLevel.TRACE, emptyList()))
        try {
            assertFalse(logger("C").isEnabled(LogLevel.CRITICAL))
        } finally {
            Logging.reset()
        }
    }

    // --- KermitSink ---

    private class Captured(val severity: Severity, val message: String, val tag: String, val throwable: Throwable?)

    private fun kermitCapture(): Pair<MutableList<Captured>, KermitSink> {
        val out = mutableListOf<Captured>()
        val writer = object : LogWriter() {
            override fun log(severity: Severity, message: String, tag: String, throwable: Throwable?) {
                out += Captured(severity, message, tag, throwable)
            }
        }
        return out to KermitSink(writer)
    }

    @Test
    fun kermitSinkMapsLevelsTagAndContext() {
        val (out, sink) = kermitCapture()
        Logging.install(LogConfig(LogLevel.TRACE, listOf(sink)))
        try {
            logger("Session").with("requestId" to "r1").warn("caducada {Hace}", "5m")
        } finally {
            Logging.reset()
        }
        val c = out.single()
        assertEquals(Severity.Warn, c.severity)
        assertEquals("Session", c.tag)
        assertEquals("caducada 5m [requestId=r1]", c.message)
    }

    @Test
    fun criticalIsSentAsErrorNotAssertBecauseAssertCanKillTheProcessOnAndroid() {
        val (out, sink) = kermitCapture()
        Logging.install(LogConfig(LogLevel.TRACE, listOf(sink)))
        try {
            logger("C").critical("grave")
        } finally {
            Logging.reset()
        }
        assertEquals(Severity.Error, out.single().severity)
        assertEquals("[CRITICAL] grave", out.single().message)
    }
}
