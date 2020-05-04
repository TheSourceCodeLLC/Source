package net.sourcebot

import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.requests.GatewayIntent
import net.sourcebot.api.command.CommandHandler
import net.sourcebot.api.command.CommandMap
import net.sourcebot.api.misc.EventSubsystem
import net.sourcebot.api.misc.Properties.Companion.fromPath
import net.sourcebot.api.module.SourceModule
import net.sourcebot.module.info.InformationModule
import net.sourcebot.module.misc.MiscellaneousModule
import java.nio.file.Path
import java.util.*

fun main() {
    Source()
}

class Source internal constructor() {
    private val ignoredIntents = EnumSet.of(
        GatewayIntent.GUILD_MESSAGE_TYPING, GatewayIntent.DIRECT_MESSAGE_TYPING
    )
    private val eventSubsystem = EventSubsystem()
    private val commandMap = CommandMap()
    private val modules: MutableSet<SourceModule> = HashSet()
    private val commandHandler: CommandHandler
    private val jda: JDA

    init {
        val properties = fromPath(Path.of("config.json"))
        commandHandler = CommandHandler(
            commandMap,
            properties.getJsonObjectRequired("commands")
        )
        eventSubsystem.listen(commandHandler::onMessageReceived)
        jda = JDABuilder.create(
            properties.getStringRequired("token"),
            EnumSet.complementOf(ignoredIntents)
        ).addEventListeners(eventSubsystem).build().awaitReady()
        loadModule(InformationModule(modules, commandMap))
        loadModule(MiscellaneousModule())
    }

    fun getEventSubsystem() = eventSubsystem

    fun loadModule(module: SourceModule) {
        modules.add(module)
        module.commands.forEach(commandMap::register)
    }

    fun unloadModule(module: SourceModule) {
        modules.remove(module)
        module.commands.forEach(commandMap::unregister)
    }
}