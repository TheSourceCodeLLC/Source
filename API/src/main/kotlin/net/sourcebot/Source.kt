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
import net.sourcebot.api.configuration.ConfigurationManager
import net.sourcebot.api.configuration.JsonConfiguration
import net.sourcebot.api.configuration.JsonSerial
import net.sourcebot.api.database.MongoDB
import net.sourcebot.api.database.MongoSerial
import net.sourcebot.api.event.EventSystem
import net.sourcebot.api.event.SourceEvent
import net.sourcebot.api.logger.LoggerConfiguration
import net.sourcebot.api.menus.MenuHandler
import net.sourcebot.api.module.ModuleHandler
import net.sourcebot.api.module.SourceModule
import net.sourcebot.api.permission.PermissionHandler
import net.sourcebot.api.permission.SourcePermission
import net.sourcebot.api.permission.SourceRole
import net.sourcebot.api.permission.SourceUser
import net.sourcebot.api.response.StandardEmbedResponse
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

class Source(val properties: JsonConfiguration) {
    private val ignoredIntents = EnumSet.of(
        GUILD_MESSAGE_TYPING, DIRECT_MESSAGE_TYPING
    )

    val sourceEventSystem = EventSystem<SourceEvent>()
    val jdaEventSystem = EventSystem<GenericEvent>()
    val configurationManager = ConfigurationManager(File("storage"))
    val mongodb = MongoDB(properties.required("mongodb"))
    val permissionHandler = PermissionHandler(mongodb, properties.required("global-admins"))
    val moduleHandler = ModuleHandler(this)

    val commandHandler = CommandHandler(
        properties.required("commands.prefix"),
        properties.required("commands.delete-seconds"),
        configurationManager,
        permissionHandler
    )

    val menuHandler = MenuHandler()
    private val activityProvider = properties.required<ActivityProvider>("activity")
    val shardManager = DefaultShardManagerBuilder.create(
        properties.required("token"),
        EnumSet.complementOf(ignoredIntents)
    ).setEnableShutdownHook(false).addEventListeners(
        EventListener(jdaEventSystem::fireEvent),
        object : ListenerAdapter() {
            override fun onMessageReceived(
                event: MessageReceivedEvent
            ) = commandHandler.onMessageReceived(event)
        },
        menuHandler
    ).setActivityProvider(activityProvider::asActivity).build().also {
        it.shards.forEach(JDA::awaitReady)
    }

    init {
        StandardEmbedResponse.footer = properties.required("embed-response.footer")
        registerSerial()
        loadModules()
        logger.info("Source is now online!")
        Runtime.getRuntime().addShutdownHook(object : Thread() {
            override fun run() {
                ModuleHandler.getModules().forEach(moduleHandler::disableModule)
                shardManager.shards.forEach(JDA::shutdown)
                configurationManager.saveAll()
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
        moduleHandler.loadAndEnable(BaseModule(this))
        moduleHandler.loadAndEnable(modulesFolder)
        logger.info("All modules have been enabled!")
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(Source::class.java)

        @JvmField
        val TIME_ZONE: ZoneId = ZoneId.of("America/New_York")
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
        val SCHEDULED_EXECUTOR_SERVICE = Executors.newScheduledThreadPool(
            2 * numCores
        )

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
            logger.info("Starting Source...")
            JsonSerial.register(JsonConfiguration.JsonSerialization())
            MongoSerial.register(JsonConfiguration.MongoSerialization())
            val properties: JsonConfiguration = File("config.json").apply {
                if (!exists()) {
                    Source::class.java.getResourceAsStream("/config.example.json").use {
                        Files.copy(it, this.toPath())
                    }
                }
            }.let { JsonSerial.fromFile(it) }
            val logLevelName = properties.required<String>("log-level")
            val logLevel = Level.toLevel(logLevelName, Level.INFO)
            LoggerConfiguration.LOG_LEVEL = logLevel
            Source(properties)
        }

        @JvmStatic
        fun <T : SourceModule> findModule(
            name: String
        ) = ModuleHandler.findModule<T>(name)

        @JvmStatic
        fun <T : SourceModule> getModule(
            type: Class<T>
        ) = ModuleHandler.getModule(type)

        @JvmStatic
        inline fun <reified T : SourceModule> getModule() = ModuleHandler.getModule(T::class.java)
    }

    class ActivityProvider @JsonCreator constructor(
        @JsonProperty("type") private val type: ActivityType,
        @JsonProperty("value") private val value: String
    ) {
        fun asActivity(shards: Int) = Activity.of(type, value.format(shards))
    }
}