package net.sourcebot.impl

import com.fasterxml.jackson.databind.node.ObjectNode
import net.sourcebot.Source
import net.sourcebot.api.configuration.JsonSerial
import net.sourcebot.api.module.InvalidModuleException
import net.sourcebot.api.module.ModuleClassLoader
import net.sourcebot.api.module.ModuleDescriptor
import net.sourcebot.api.module.SourceModule
import net.sourcebot.impl.command.*
import net.sourcebot.impl.command.lifecycle.RestartCommand

class BaseModule(
    source: Source
) : SourceModule() {
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
            HelpCommand(source.moduleHandler, source.permissionHandler, source.commandHandler),
            GuildInfoCommand(),
            TimingsCommand(),
            PermissionsCommand(source.permissionHandler),
            ConfigurationCommand(source.guildConfigurationManager),

            RestartCommand(source.properties.required("restart-script")),
        )
    }
}