package net.sourcebot.impl

import com.fasterxml.jackson.databind.node.ObjectNode
import net.sourcebot.Source
import net.sourcebot.api.configuration.ConfigurationInfo
import net.sourcebot.api.configuration.JsonSerial
import net.sourcebot.api.module.ModuleClassLoader
import net.sourcebot.api.module.ModuleDescriptor
import net.sourcebot.api.module.SourceModule
import net.sourcebot.api.module.exception.InvalidModuleException
import net.sourcebot.impl.command.*
import net.sourcebot.impl.listener.ConnectionListener

class BaseModule(
    source: Source
) : SourceModule() {
    override val configurationInfo = ConfigurationInfo("source") {
        section("connections") {
            node("channel", "The channel ID join / leave messages will be sent to.")
            node("joinMessages", "Messages to be sent when members join.")
            node("leaveMessages", "Messages to be sent when members leave.")
        }
    }

    init {
        this.source = source
        classLoader = object : ModuleClassLoader(source.moduleHandler) {
            override fun findClass(name: String, searchParent: Boolean): Class<*> {
                return try {
                    if (searchParent) source.moduleHandler.findClass(name)
                    else source.javaClass.classLoader.loadClass(name)
                } catch (ex: Exception) {
                    null
                } ?: throw ClassNotFoundException(name)
            }
        }
        descriptor = this.javaClass.getResourceAsStream("/module.json").use {
            if (it == null) throw InvalidModuleException("Could not find module.json!")
            else JsonSerial.mapper.readTree(it) as ObjectNode
        }.let(::ModuleDescriptor)
    }

    override fun onEnable() {
        registerCommands(
            HelpCommand(source.permissionHandler, source.commandHandler),
            GuildInfoCommand(),
            TimingsCommand(),
            PermissionsCommand(source.permissionHandler),
            ConfigurationCommand(source.configurationManager),
            *lifecycleCommands(source.properties.required("lifecycle"))
        )
        subscribeEvents(
            ConnectionListener(source.configurationManager)
        )
    }
}