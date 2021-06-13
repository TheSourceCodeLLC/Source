package net.sourcebot

import ch.qos.logback.classic.Level
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.entities.Activity.ActivityType
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.hooks.EventListener
import net.dv8tion.jda.api.requests.GatewayIntent.DIRECT_MESSAGE_TYPING
import net.dv8tion.jda.api.requests.GatewayIntent.GUILD_MESSAGE_TYPING
import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder
import net.dv8tion.jda.api.sharding.ShardManager
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
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService

@Suppress("Unused")
object Source {
    private val logger: Logger = LoggerFactory.getLogger(Source::class.java)
    @JvmStatic internal val properties: JsonConfiguration

    init {
        if (this::SHARD_MANAGER.isInitialized) throw IllegalStateException("Source is already enabled!")
        logger.info("Starting Source...")
        JsonSerial.register(JsonConfiguration.JsonSerialization())
        MongoSerial.register(JsonConfiguration.MongoSerialization())
        properties = File("config.json").apply {
            if (!exists()) {
                Source::class.java.getResourceAsStream("/config.example.json").use {
                    Files.copy(it, this.toPath())
                }
            }
        }.let { JsonSerial.fromFile(it) }
    }

    private val ignoredIntents = EnumSet.of(
        GUILD_MESSAGE_TYPING, DIRECT_MESSAGE_TYPING
    )

    @JvmStatic val SOURCE_EVENTS = EventSystem<SourceEvent>()
    @JvmStatic val JDA_EVENTS = EventSystem<GenericEvent>()

    private fun registerSerial() {
        MongoSerial.register(SourcePermission.Serial())
        MongoSerial.register(SourceUser.Serial(PERMISSION_HANDLER))
        MongoSerial.register(SourceRole.Serial(PERMISSION_HANDLER))
        JsonSerial.register(object : JsonSerial<Instant> {
            override val serializer = JsonSerial.createSerializer<Instant> { obj, gen, _ ->
                gen.writeNumber(obj.toEpochMilli())
            }

            override val deserializer = JsonSerial.createDeserializer { parser, _ ->
                Instant.ofEpochMilli(parser.longValue)
            }
        })
    }

    private fun loadModules() {
        val modulesFolder = File("modules")
        if (!modulesFolder.exists()) modulesFolder.mkdir()
        MODULE_HANDLER.loadAndEnable(BaseModule(this::class.java.classLoader))
        MODULE_HANDLER.loadAndEnable(modulesFolder)
        logger.info("All modules have been enabled!")
    }

    @JvmStatic val MODULE_HANDLER = ModuleHandler()
    @JvmStatic val MENU_HANDLER = MenuHandler()
    @JvmStatic val MONGODB = MongoDB(properties.required("mongodb"))
    @JvmStatic val PERMISSION_HANDLER = PermissionHandler(properties.required("global-admins"))
    @JvmStatic val CONFIG_MANAGER = ConfigurationManager(File("storage"))
    @JvmStatic val COMMAND_HANDLER = CommandHandler(
        properties.required("commands.prefix"),
        properties.required("commands.delete-seconds")
    )

    @JvmStatic val TIME_ZONE: ZoneId = ZoneId.of("America/New_York")
    @JvmStatic val DATE_TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern(
        "MM/dd/yyyy hh:mm:ss a z"
    ).withZone(TIME_ZONE)

    @JvmStatic val TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern(
        "hh:mm:ss a z"
    ).withZone(TIME_ZONE)

    @JvmStatic val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern(
        "MM/dd/yyyy"
    ).withZone(TIME_ZONE)

    private val numCores = Runtime.getRuntime().availableProcessors()
    @JvmField val EXECUTOR_SERVICE: ExecutorService = Executors.newFixedThreadPool(2 * numCores)
    @JvmField val SCHEDULED_EXECUTOR_SERVICE: ScheduledExecutorService = Executors.newScheduledThreadPool(
        2 * numCores
    )

    @JvmStatic lateinit var SHARD_MANAGER: ShardManager

    @JvmStatic fun main(args: Array<String>) {
        SysOutOverSLF4J.sendSystemOutAndErrToSLF4J()
        start()
    }

    @JvmStatic fun start() {
        StandardEmbedResponse.footer = properties.required("embed-response.footer")
        LoggerConfiguration.LOG_LEVEL = properties.required<String>("log-level").let {
            Level.toLevel(it, Level.INFO)
        }
        val activityProvider: ActivityProvider = properties.required("activity")
        SHARD_MANAGER = DefaultShardManagerBuilder.create(
            properties.required("token"),
            EnumSet.complementOf(ignoredIntents)
        ).setEnableShutdownHook(false).addEventListeners(
            EventListener(JDA_EVENTS::fireEvent),
            COMMAND_HANDLER, MENU_HANDLER
        ).setActivityProvider(activityProvider::asActivity).build().also {
            it.shards.forEach(JDA::awaitReady)
        }
        registerSerial()
        loadModules()
        logger.info("Source is now online!")
        Runtime.getRuntime().addShutdownHook(object : Thread() {
            override fun run() {
                MODULE_HANDLER.loader.getExtensions().forEach(MODULE_HANDLER::disableModule)
                SHARD_MANAGER.shards.forEach(JDA::shutdown)
                CONFIG_MANAGER.saveAll()
            }
        })
    }

    private class ActivityProvider @JsonCreator constructor(
        @JsonProperty("type") private val type: ActivityType,
        @JsonProperty("value") private val value: String
    ) {
        fun asActivity(shards: Int) = Activity.of(type, value.format(shards))
    }
}