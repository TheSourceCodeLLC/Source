package net.sourcebot.api.logger

import ch.qos.logback.classic.*
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.ConsoleAppender
import ch.qos.logback.core.encoder.LayoutWrappingEncoder

class LoggerConfiguration : BasicConfigurator() {
    private val loggerLevels = mapOf<String, Level>(
        Pair("net.dv8tion.jda.internal", Level.WARN),
        Pair("uk.org.lidalia.sysoutslf4j", Level.WARN),
        Pair("org.mongodb.driver.cluster", Level.WARN)
    )

    override fun configure(context: LoggerContext) {
        val layout = PatternLayout().apply {
            this.pattern = "[%highlight(%d{hh:mm:ss a z} %-5level)] [%highlight(%logger)] %msg%n"
            this.context = context
            start()
        }
        val encoder = LayoutWrappingEncoder<ILoggingEvent>().apply {
            this.context = context
            this.layout = layout
        }
        val appender = ConsoleAppender<ILoggingEvent>().apply {
            this.context = context
            this.name = "STDOUT"
            this.isWithJansi = System.getProperty("useJansi")?.toBoolean() ?: false
            setEncoder(encoder)
            start()
        }
        context.getLogger(Logger.ROOT_LOGGER_NAME).apply {
            this.level = Level.INFO
            addAppender(appender)
        }

        loggerLevels.forEach { (name, level) ->
            context.getLogger(name).level = level
        }
    }
}