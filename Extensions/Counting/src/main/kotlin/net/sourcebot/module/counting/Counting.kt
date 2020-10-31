package net.sourcebot.module.counting

import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import net.sourcebot.Source
import net.sourcebot.api.configuration.ConfigurationInfo
import net.sourcebot.api.module.SourceModule
import net.sourcebot.module.counting.command.CountingCommand
import net.sourcebot.module.counting.data.CountingListener

class Counting : SourceModule() {
    override val configurationInfo = ConfigurationInfo("counting") {
        node("channel", "Channel ID for the counting channel.")
    }

    override fun onEnable() {
        Source.SHARD_MANAGER.guilds.forEach { guild ->
            val countingChannel = getCountingChannel(guild) ?: return@forEach
            countingChannel.upsertPermissionOverride(
                guild.publicRole
            ).clear(Permission.MESSAGE_WRITE).complete()
        }
        registerCommands(CountingCommand())
        subscribeEvents(CountingListener())
    }

    override fun onDisable() {
        Source.SHARD_MANAGER.guilds.forEach { guild ->
            val countingChannel = getCountingChannel(guild) ?: return@forEach
            countingChannel.upsertPermissionOverride(
                guild.publicRole
            ).deny(Permission.MESSAGE_WRITE).complete()
        }
    }

    private fun getCountingChannel(guild: Guild) = Source.CONFIG_MANAGER[guild].optional<String>(
        "counting.channel"
    )?.let(guild::getTextChannelById)
}