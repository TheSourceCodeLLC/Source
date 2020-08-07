package net.sourcebot

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.EventListener
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.requests.GatewayIntent.DIRECT_MESSAGE_TYPING
import net.dv8tion.jda.api.requests.GatewayIntent.GUILD_MESSAGE_TYPING
import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder
import net.sourcebot.api.alert.EmbedAlert
import net.sourcebot.api.command.CommandHandler
import net.sourcebot.api.database.MongoDB
import net.sourcebot.api.database.MongoSerial
import net.sourcebot.api.event.EventSystem
import net.sourcebot.api.event.SourceEvent
import net.sourcebot.api.module.*
import net.sourcebot.api.permission.*
import net.sourcebot.api.properties.JsonSerial
import net.sourcebot.api.properties.Properties
import net.sourcebot.impl.command.GuildInfoCommand
import net.sourcebot.impl.command.HelpCommand
import net.sourcebot.impl.command.PermissionsCommand
import net.sourcebot.impl.command.TimingsCommand
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import uk.org.lidalia.sysoutslf4j.context.SysOutOverSLF4J
import java.io.File
import java.io.FileReader
import java.io.InputStreamReader
import java.nio.file.Files
import java.nio.file.Path
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatter.ofPattern
import java.util.*

class Source internal constructor(val properties: Properties) : SourceModule() {
    private val ignoredIntents = EnumSet.of(
        GUILD_MESSAGE_TYPING, DIRECT_MESSAGE_TYPING
    )
    val globalAdmins: Set<String> = properties.required("global-admins")

    val sourceEventSystem = EventSystem<SourceEvent>()
    val jdaEventSystem = EventSystem<GenericEvent>()

    val mongodb = MongoDB(properties.required("mongodb"))
    val permissionHandler = PermissionHandler(mongodb, globalAdmins)
    val moduleHandler = ModuleHandler()

    val commandHandler = CommandHandler(
        properties.required("commands.prefix"),
        properties.required("commands.delete-seconds"),
        globalAdmins,
        permissionHandler
    )

    val shardManager = DefaultShardManagerBuilder.create(
        properties.required("token"),
        EnumSet.complementOf(ignoredIntents)
    ).addEventListeners(
        EventListener(jdaEventSystem::fireEvent),
        object : ListenerAdapter() {
            override fun onMessageReceived(event: MessageReceivedEvent) {
                commandHandler.onMessageReceived(event)
            }
        }
    ).setActivityProvider {
        Activity.watching("TSC. Shard $it")
    }.build()

    init {
        shardManager.shards.forEach { it.awaitReady() }
        instance = this
        EmbedAlert.footer = properties.required("alert.footer")
        classLoader = object : ModuleClassLoader() {
            override fun findClass(name: String, searchParent: Boolean): Class<*> {
                return try {
                    if (searchParent) moduleHandler.findClass(name)
                    else Source::class.java.classLoader.loadClass(name)
                } catch (ex: Exception) {
                    null
                } ?: throw ClassNotFoundException(name)
            }
        }
        descriptor = this.javaClass.getResourceAsStream("/module.json").use {
            if (it == null) throw InvalidModuleException("Could not find module.json!")
            else JsonParser.parseReader(InputStreamReader(it)) as JsonObject
        }.let(::ModuleDescriptor)

        registerSerial()
        loadModules()

        logger.info("Source is now online!")
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
        moduleHandler.loadModule(this)
        val modules = moduleHandler.loadModules(modulesFolder)
        logger.info("Enabling modules...")
        moduleHandler.enableModule(this)
        modules.forEach(moduleHandler::enableModule)
        logger.info("All modules have been enabled!")
    }

    override fun onEnable() {
        registerCommands(
            HelpCommand(moduleHandler, commandHandler),
            GuildInfoCommand(),
            TimingsCommand(),
            PermissionsCommand(permissionHandler)
        )
    }

    companion object {
        @JvmField val DATE_TIME_FORMAT: DateTimeFormatter = ofPattern("MM/dd/yyyy hh:mm:ss a z")
        @JvmField val TIME_FORMAT: DateTimeFormatter = ofPattern("hh:mm:ss a z")
        @JvmField val DATE_FORMAT: DateTimeFormatter = ofPattern("MM/dd/yyyy")

        @JvmField val TIME_ZONE: ZoneId = ZoneId.of("America/New_York")
        @JvmField val logger: Logger = LoggerFactory.getLogger(Source::class.java)

        @JvmStatic lateinit var instance: Source
        var enabled = false
            internal set

        @JvmStatic fun main(args: Array<String>) {
            SysOutOverSLF4J.sendSystemOutAndErrToSLF4J()
            start()
        }

        @JvmStatic fun start(): Source {
            if (enabled) throw IllegalStateException("Source is already enabled!")

            JsonSerial.register(Properties.Serial())
            val configFile = File("config.json")
            if (!configFile.exists()) {
                Source::class.java.getResourceAsStream("/config.example.json").use {
                    Files.copy(it, Path.of("config.json"))
                }
            }
            return FileReader(configFile).use {
                JsonParser.parseReader(it) as JsonObject
            }.let(::Properties).let(::Source)
        }
    }
}