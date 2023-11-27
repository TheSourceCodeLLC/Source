package net.sourcebot.api.logger

import ch.qos.logback.classic.*
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.ConsoleAppender
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

    private fun consoleAppender(
        context: LoggerContext
    ) = ConsoleAppender<ILoggingEvent>().apply {
        this.encoder = encoder(context, "[%highlight(%d{hh:mm:ss a z} %-5level)] [%highlight(%logger)] %msg%n")
        this.isWithJansi = System.getProperty("useJansi")?.toBoolean() ?: false
        this.context = context
        this.name = "STDOUT"
        start()
    }

    private fun fileAppender(
        context: LoggerContext
    ) = RollingFileAppender<ILoggingEvent>().apply {
        this.encoder = encoder(context, "[%d{hh:mm:ss a z} %-5level] [%logger] %msg%n")
        this.rollingPolicy = TimeBasedRollingPolicy<ILoggingEvent>().also {
            it.fileNamePattern = "logs/%d{yyyy-MM-dd}.log.gz"
            it.isCleanHistoryOnStart = true
            it.context = context
            it.maxHistory = 7
            it.setParent(this)
            it.start()
        }
        this.context = context
        this.name = "FILE"
        start()
    }

    private fun encoder(
        context: LoggerContext,
        pattern: String
    ) = LayoutWrappingEncoder<ILoggingEvent>().apply {
        this.charset = StandardCharsets.UTF_8
        this.layout = PatternLayout().apply {
            this.pattern = pattern
            this.context = context
            start()
        }
        this.context = context
        start()
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