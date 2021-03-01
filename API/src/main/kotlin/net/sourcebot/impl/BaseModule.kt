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
import net.sourcebot.impl.listener.ChannelDeleteListener
import net.sourcebot.impl.listener.ConnectionListener
import net.sourcebot.impl.listener.MentionListener

class BaseModule(
    private val extClassLoader: ClassLoader
) : SourceModule() {
    override val configurationInfo = ConfigurationInfo("source") {
        section("connections") {
            node("channel", "The channel ID join / leave messages will be sent to.")
            node("joinMessages", "Messages to be sent when members join.")
            node("leaveMessages", "Messages to be sent when members leave.")
        }
        section("command") {
            node("prefix", "The prefix to use for commands.")
            section("cleanup") {
                node("enabled", "Whether or not to cleanup executed commands.")
                node("seconds", "The amount of seconds to clean up a command after it has been run.")
            }
        }
    }

    init {
        classLoader = object : ModuleClassLoader(Source.MODULE_HANDLER) {
            override fun findClass(name: String, searchParent: Boolean): Class<*> {
                return try {
                    if (searchParent) Source.MODULE_HANDLER.findClass(name)
                    else extClassLoader.loadClass(name)
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
            InfoCommand(),
            HelpCommand(),
            GuildInfoCommand(),
            TimingsCommand(),
            PermissionsCommand(),
            ConfigurationCommand(),
            *lifecycleCommands(Source.properties.required("lifecycle")),
            SudoCommand(),
            DiceCommand(),
            SayCommand(),
            ShutdownCommand()
        )
        subscribeEvents(
            ConnectionListener(),
            ChannelDeleteListener(),
            MentionListener()
        )
    }
}