package net.sourcebot.api.logger

import ch.qos.logback.classic.*
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.Appender
import ch.qos.logback.core.ConsoleAppender
import ch.qos.logback.core.encoder.Encoder
import ch.qos.logback.core.encoder.LayoutWrappingEncoder
import ch.qos.logback.core.rolling.RollingFileAppender
import ch.qos.logback.core.rolling.TimeBasedRollingPolicy
import org.slf4j.LoggerFactory
import java.nio.charset.StandardCharsets
import kotlin.properties.Delegates.observable

class LoggerConfiguration : BasicConfigurator() {
    private val loggerLevels = mapOf<String, Level>(
        Pair("net.dv8tion.jda.internal", Level.WARN),
        Pair("uk.org.lidalia.sysoutslf4j", Level.WARN),
        Pair("org.mongodb.driver.cluster", Level.WARN),
        Pair("org.mongodb.driver.connection", Level.WARN)
    )

    override fun configure(context: LoggerContext) {
        context.getLogger(Logger.ROOT_LOGGER_NAME).apply {
            this.level = LOG_LEVEL
            addAppender(consoleAppender(context))
            addAppender(fileAppender(context))
        }

        loggerLevels.forEach { (name, level) ->
            context.getLogger(name).level = level
        }
    }

    private fun consoleAppender(context: LoggerContext): Appender<ILoggingEvent> {
        val encoder = encoder(context, "[%highlight(%d{hh:mm:ss a z} %-5level)] [%highlight(%logger)] %msg%n")
        return ConsoleAppender<ILoggingEvent>().apply {
            this.isWithJansi = System.getProperty("useJansi")?.toBoolean() ?: false
            this.context = context
            this.name = "STDOUT"
            setEncoder(encoder)
            start()
        }
    }

    private fun fileAppender(context: LoggerContext): Appender<ILoggingEvent> {
        val encoder = encoder(context, "[%d{hh:mm:ss a z} %-5level] [%logger] %msg%n")
        val rollingPolicy = TimeBasedRollingPolicy<ILoggingEvent>().apply {
            this.fileNamePattern = "logs/%d{yyyy-MM-dd}.log.gz"
            this.isCleanHistoryOnStart = true
            this.context = context
            this.maxHistory = 7
        }
        return RollingFileAppender<ILoggingEvent>().apply {
            this.rollingPolicy = rollingPolicy.also {
                it.setParent(this)
                it.start()
            }
            this.context = context
            this.encoder = encoder
            this.name = "FILE"
            start()
        }
    }

    private fun encoder(context: LoggerContext, pattern: String): Encoder<ILoggingEvent> {
        val layout = PatternLayout().apply {
            this.pattern = pattern
            this.context = context
            start()
        }
        return LayoutWrappingEncoder<ILoggingEvent>().apply {
            this.charset = StandardCharsets.UTF_8
            this.context = context
            this.layout = layout
            start()
        }
    }

    companion object {
        @JvmStatic
        var LOG_LEVEL: Level by observable(Level.INFO) { _, old, new ->
            if (old == new) return@observable
            val context = LoggerFactory.getILoggerFactory() as LoggerContext
            context.getLogger(Logger.ROOT_LOGGER_NAME).level = new
        }
    }
}