package net.sourcebot.impl

import com.fasterxml.jackson.databind.node.ObjectNode
import net.sourcebot.Source
import net.sourcebot.api.configuration.JsonSerial
import net.sourcebot.api.module.InvalidModuleException
import net.sourcebot.api.module.ModuleClassLoader
import net.sourcebot.api.module.ModuleDescriptor
import net.sourcebot.api.module.SourceModule
import net.sourcebot.impl.command.GuildInfoCommand
import net.sourcebot.impl.command.HelpCommand
import net.sourcebot.impl.command.PermissionsCommand
import net.sourcebot.impl.command.TimingsCommand
import net.sourcebot.impl.command.lifecycle.RestartCommand

class BaseModule(
    private val source: Source
) : SourceModule() {
    init {
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

    override fun onEnable(source: Source) {
        source.commandHandler.registerCommands(
            this,
            HelpCommand(source.moduleHandler, source.commandHandler),
            GuildInfoCommand(),
            TimingsCommand(),
            PermissionsCommand(source.permissionHandler),

            RestartCommand(source.properties.required("restart-script")),
        )
    }
}