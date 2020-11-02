package net.sourcebot.module.counting

import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.requests.restaction.PermissionOverrideAction
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
        Source.SHARD_MANAGER.guilds.forEach(::clearCountingOverride)
        registerCommands(CountingCommand())
        subscribeEvents(CountingListener())
    }

    override fun onDisable() {
        Source.SHARD_MANAGER.guilds.forEach(::denyCountingOverride)
    }

    private fun setCountingOverride(
        guild: Guild,
        action: (PermissionOverrideAction, Permission) -> PermissionOverrideAction
    ) {
        val countingChannel = getCountingChannel(guild) ?: return
        action(
            countingChannel.upsertPermissionOverride(guild.publicRole),
            Permission.MESSAGE_WRITE
        ).complete()
    }

    private fun clearCountingOverride(guild: Guild) = setCountingOverride(guild, PermissionOverrideAction::clear)
    private fun denyCountingOverride(guild: Guild) = setCountingOverride(guild, PermissionOverrideAction::deny)

    companion object {
        @JvmStatic fun getCountingChannel(guild: Guild) = Source.CONFIG_MANAGER[guild].optional<String>(
            "counting.channel"
        )?.let(guild::getTextChannelById)
    }
}