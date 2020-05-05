package net.sourcebot

import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.requests.GatewayIntent
import net.sourcebot.api.command.CommandHandler
import net.sourcebot.api.command.CommandMap
import net.sourcebot.api.event.EventSubsystem
import net.sourcebot.api.misc.Properties
import net.sourcebot.api.module.SourceModule
import net.sourcebot.base.EightBallCommand
import net.sourcebot.base.HelpCommand
import net.sourcebot.base.OngCommand
import net.sourcebot.base.TimingsCommand
import java.nio.file.Path
import java.util.*
import kotlin.collections.HashSet

fun main() {
    val source = Source()
    //Register base module
    source.loadModule(source)
}

class Source internal constructor() : SourceModule {
    private val ignoredIntents = EnumSet.of(
        GatewayIntent.GUILD_MESSAGE_TYPING, GatewayIntent.DIRECT_MESSAGE_TYPING
    )
    private val eventSubsystem = EventSubsystem()
    private val commandMap = CommandMap()
    private val modules: MutableSet<SourceModule> = HashSet()
    private val commandHandler: CommandHandler
    private val jda: JDA

    init {
        val properties = Properties.fromPath(Path.of("config.json"))
        commandHandler = CommandHandler(
            commandMap,
            properties.getJsonObjectRequired("commands")
        )
        jda = JDABuilder.create(
            properties.getStringRequired("token"),
            EnumSet.complementOf(ignoredIntents)
        ).addEventListeners(eventSubsystem).build().awaitReady()
    }

    fun loadModule(module: SourceModule) {
        modules.add(module)
        module.commands.forEach(commandMap::register)
        module.registerEvents(eventSubsystem)
    }

    fun unloadModule(module: SourceModule) {
        modules.remove(module)
        module.commands.forEach(commandMap::unregister)
        eventSubsystem.unregister(module)
    }

    override val name = "Source"
    override val description = "Source base module"
    override val commands = setOf(
        HelpCommand(modules, commandMap),
        TimingsCommand(),
        EightBallCommand(),
        OngCommand()
    )

    override fun registerEvents(eventSubsystem: EventSubsystem) {
        eventSubsystem.listen(this, commandHandler::onMessageReceived)
    }
}