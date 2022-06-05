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
        node("mute-role", "Role ID to mute players from the counting channel.")
    }

    private val countingListener = CountingListener()
    private val checkpointTask = countingListener.handleCheckpoints()
    override fun enable() {
        Source.SHARD_MANAGER.guilds.forEach(::clearCountingOverride)
        registerCommands(CountingCommand())
        subscribeEvents(countingListener)
    }

    override fun disable() {
        Source.SHARD_MANAGER.guilds.forEach(::denyCountingOverride)
        checkpointTask.cancel(true)
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

    private fun clearCountingOverride(guild: Guild) = setCountingOverride(guild) { a, b -> a.clear(b) }
    private fun denyCountingOverride(guild: Guild) = setCountingOverride(guild) { a, b -> a.deny(b) }

    companion object {
        @JvmStatic fun getCountingChannel(guild: Guild) = Source.CONFIG_MANAGER[guild].optional<String>(
            "counting.channel"
        )?.let(guild::getTextChannelById)
    }
}