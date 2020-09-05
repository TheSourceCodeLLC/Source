package net.sourcebot

import ch.qos.logback.classic.Level
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.entities.Activity.ActivityType
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.EventListener
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.requests.GatewayIntent.DIRECT_MESSAGE_TYPING
import net.dv8tion.jda.api.requests.GatewayIntent.GUILD_MESSAGE_TYPING
import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder
import net.sourcebot.api.command.CommandHandler
import net.sourcebot.api.configuration.GuildConfigurationManager
import net.sourcebot.api.configuration.JsonSerial
import net.sourcebot.api.configuration.Properties
import net.sourcebot.api.database.MongoDB
import net.sourcebot.api.database.MongoSerial
import net.sourcebot.api.event.EventSystem
import net.sourcebot.api.event.SourceEvent
import net.sourcebot.api.logger.LoggerConfiguration
import net.sourcebot.api.module.ModuleHandler
import net.sourcebot.api.permission.PermissionHandler
import net.sourcebot.api.permission.SourcePermission
import net.sourcebot.api.permission.SourceRole
import net.sourcebot.api.permission.SourceUser
import net.sourcebot.api.response.EmbedResponse
import net.sourcebot.impl.BaseModule
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import uk.org.lidalia.sysoutslf4j.context.SysOutOverSLF4J
import java.io.File
import java.nio.file.Files
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.concurrent.Executors

class Source(val properties: Properties) {
    private val logger: Logger = LoggerFactory.getLogger(Source::class.java)

    private val ignoredIntents = EnumSet.of(
        GUILD_MESSAGE_TYPING, DIRECT_MESSAGE_TYPING
    )

    val sourceEventSystem = EventSystem<SourceEvent>()
    val jdaEventSystem = EventSystem<GenericEvent>()

    val guildConfigurationManager = GuildConfigurationManager(File("storage"))
    val mongodb = MongoDB(properties.required("mongodb"))
    val permissionHandler = PermissionHandler(mongodb, properties.required("global-admins"))
    val moduleHandler = ModuleHandler(this)

    val commandHandler = CommandHandler(
        properties.required("commands.prefix"),
        properties.required("commands.delete-seconds"),
        permissionHandler
    )

    private val activityProvider = properties.required<ActivityProvider>("activity")
    val shardManager = DefaultShardManagerBuilder.create(
        properties.required("token"),
        EnumSet.complementOf(ignoredIntents)
    ).addEventListeners(
        EventListener(jdaEventSystem::fireEvent),
        object : ListenerAdapter() {
            override fun onMessageReceived(
                event: MessageReceivedEvent
            ) = commandHandler.onMessageReceived(event)
        }
    ).setActivityProvider(activityProvider::asActivity).build().also {
        it.shards.forEach(JDA::awaitReady)
    }

    init {
        EmbedResponse.footer = properties.required("embed-response.footer")
        registerSerial()
        loadModules()
        logger.info("Source is now online!")
        Runtime.getRuntime().addShutdownHook(object : Thread() {
            override fun run() {
                moduleHandler.getModules().forEach(moduleHandler::disableModule)
                shardManager.shards.forEach(JDA::shutdown)
                guildConfigurationManager.saveAll()
            }
        })
    }

    private fun registerSerial() {
        MongoSerial.register(SourcePermission.Serial())
        MongoSerial.register(SourceUser.Serial(permissionHandler))
        MongoSerial.register(SourceRole.Serial(permissionHandler))
    }

    private fun loadModules() {
        val modulesFolder = File("modules")
        if (!modulesFolder.exists()) modulesFolder.mkdir()
        logger.info("Loading modules...")
        val baseModule = BaseModule(this)
        moduleHandler.loadModule(baseModule)
        val modules = moduleHandler.loadModules(modulesFolder)
        logger.info("Enabling modules...")
        moduleHandler.enableModule(baseModule)
        modules.forEach(moduleHandler::enableModule)
        logger.info("All modules have been enabled!")
    }

    companion object {
        @JvmField val TIME_ZONE: ZoneId = ZoneId.of("America/New_York")
        @JvmField
        val DATE_TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern(
            "MM/dd/yyyy hh:mm:ss a z"
        ).withZone(TIME_ZONE)
        @JvmField
        val TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern(
            "hh:mm:ss a z"
        ).withZone(TIME_ZONE)
        @JvmField
        val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern(
            "MM/dd/yyyy"
        ).withZone(TIME_ZONE)

        private val numCores = Runtime.getRuntime().availableProcessors()
        @JvmField
        val SCHEDULED_EXECUTOR_SERVICE = Executors.newScheduledThreadPool(2 * numCores)
        @JvmField
        val EXECUTOR_SERVICE = Executors.newFixedThreadPool(2 * numCores)

        var enabled = false
            internal set

        @JvmStatic
        fun main(args: Array<String>) {
            SysOutOverSLF4J.sendSystemOutAndErrToSLF4J()
            start()
        }

        @JvmStatic
        fun start() {
            if (enabled) throw IllegalStateException("Source is already enabled!")
            enabled = true

            JsonSerial.registerSerial(Properties.Serial())
            val properties = File("config.json").apply {
                if (!exists()) {
                    Source::class.java.getResourceAsStream("/config.example.json").use {
                        Files.copy(it, this.toPath())
                    }
                }
            }.let {
                JsonSerial.mapper.readValue(it, Properties::class.java)
            }
            val logLevelName = properties.required<String>("log-level")
            val logLevel = Level.toLevel(logLevelName, Level.INFO)
            LoggerConfiguration.LOG_LEVEL = logLevel
            Source(properties)
        }
    }

    class ActivityProvider @JsonCreator constructor(
        @JsonProperty("type") private val type: ActivityType,
        @JsonProperty("value") private val value: String
    ) {
        fun asActivity(shards: Int) = Activity.of(type, value.format(shards))
    }
}