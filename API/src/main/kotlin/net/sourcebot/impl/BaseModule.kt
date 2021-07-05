package net.sourcebot.impl

import com.fasterxml.jackson.databind.node.ObjectNode
import me.hwiggy.extensible.binding.jvm.classloader.JarClassLoader
import me.hwiggy.extensible.exception.InvalidExtensionException
import net.sourcebot.Source
import net.sourcebot.api.configuration.ConfigurationInfo
import net.sourcebot.api.configuration.JsonSerial
import net.sourcebot.api.module.ModuleDescriptor
import net.sourcebot.api.module.SourceModule
import net.sourcebot.impl.command.*
import net.sourcebot.impl.listener.ChannelDeleteListener
import net.sourcebot.impl.listener.ConnectionListener
import net.sourcebot.impl.listener.MentionListener
import java.io.File

class BaseModule : SourceModule() {
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
        val path = BaseModule::class.java.protectionDomain.codeSource.location.toURI()
        classLoader = JarClassLoader(Source.MODULE_HANDLER, File(path))
        descriptor = this.javaClass.getResourceAsStream("/module.json").use {
            if (it == null) throw InvalidExtensionException("Could not find module.json!")
            else JsonSerial.mapper.readTree(it) as ObjectNode
        }.let(::ModuleDescriptor)
    }

    override fun enable() {
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
            ShutdownCommand(),
            ReactCommand()
        )
        subscribeEvents(
            ConnectionListener(),
            ChannelDeleteListener(),
            MentionListener()
        )
    }
}