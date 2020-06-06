package net.sourcebot

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.EventListener
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.requests.GatewayIntent.DIRECT_MESSAGE_TYPING
import net.dv8tion.jda.api.requests.GatewayIntent.GUILD_MESSAGE_TYPING
import net.sourcebot.api.command.CommandHandler
import net.sourcebot.api.database.MongoDB
import net.sourcebot.api.database.MongoSerial
import net.sourcebot.api.event.EventSystem
import net.sourcebot.api.event.SourceEvent
import net.sourcebot.api.module.ModuleHandler
import net.sourcebot.api.permission.*
import net.sourcebot.api.properties.JsonSerial
import net.sourcebot.api.properties.Properties
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import uk.org.lidalia.sysoutslf4j.context.SysOutOverSLF4J
import java.io.File
import java.io.FileFilter
import java.io.FileReader
import java.util.*
import kotlin.Comparator
import kotlin.collections.ArrayList

class Source internal constructor() {
    private val ignoredIntents = EnumSet.of(
        GUILD_MESSAGE_TYPING, DIRECT_MESSAGE_TYPING
    )

    val sourceEventSystem = EventSystem<SourceEvent>()
    val jdaEventSystem = EventSystem<GenericEvent>()

    val properties = FileReader("config.json").use {
        JsonParser.parseReader(it) as JsonObject
    }.let(::Properties)

    val mongodb = MongoDB(properties.required("mongodb"))
    val permissionHandler = PermissionHandler(mongodb)

    val moduleHandler = ModuleHandler(this)

    val commandHandler = CommandHandler(
        properties.required("commands.prefix"),
        properties.required("commands.delete-seconds"),
        permissionHandler
    )

    @get:JvmName("getJDA")
    val jda = JDABuilder.create(
        properties.required("token"),
        EnumSet.complementOf(ignoredIntents)
    ).addEventListeners(
        EventListener(jdaEventSystem::fireEvent),
        object : ListenerAdapter() {
            override fun onMessageReceived(event: MessageReceivedEvent) {
                commandHandler.onMessageReceived(event)
            }
        }
    ).build().awaitReady()

    private fun registerSerial() {
        MongoSerial.register(SourcePermission.Serial())
        MongoSerial.register(SourceGroup.Serial(permissionHandler))
        MongoSerial.register(SourceUser.Serial(permissionHandler))
        MongoSerial.register(SourceRole.Serial(permissionHandler))
    }

    private fun loadModules() {
        logger.debug("Indexing Modules...")
        val modulesFolder = File("modules")
        if (!modulesFolder.exists()) modulesFolder.mkdir()
        val indexed = modulesFolder.listFiles(FileFilter {
            it.name.endsWith(".jar")
        })!!.sortedWith(Comparator.comparing(File::getName)).mapNotNull(moduleHandler::indexModule)
        logger.debug("Loading Modules...")
        val errored = ArrayList<String>()
        val loaded = indexed.mapNotNull {
            try {
                moduleHandler.loadModule(it)
            } catch (ex: Throwable) {
                when (ex) {
                    is StackOverflowError -> logger.error("Cyclic dependency problem for module '$it' !")
                    else -> logger.error("Error loading module '$it' !", ex)
                }
                errored.add(it)
                null
            }
        }
        errored.forEach(moduleHandler::unloadModule)
        logger.debug("All modules have been loaded, now enabling...")
        loaded.forEach(moduleHandler::enableModule)
    }

    companion object {
        @JvmStatic val logger: Logger = LoggerFactory.getLogger(Source::class.java)
        private var enabled = false

        @JvmStatic fun main(args: Array<String>) {
            start()
        }

        @JvmStatic fun start(): Source {
            if (enabled) throw IllegalStateException("Source is already enabled!")
            enabled = true

            SysOutOverSLF4J.sendSystemOutAndErrToSLF4J()
            JsonSerial.register(Properties.Serial())
            return Source().apply {
                registerSerial()
                loadModules()
                logger.info("Source is now online!")
            }
        }
    }
}