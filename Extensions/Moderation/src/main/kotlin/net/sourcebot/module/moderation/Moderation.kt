package net.sourcebot.module.moderation

import net.sourcebot.api.configuration.ConfigurationInfo
import net.sourcebot.api.module.SourceModule
import net.sourcebot.module.moderation.command.*

class Moderation : SourceModule() {
    override val configurationInfo = ConfigurationInfo("moderation") {
        node("incident-log", "Channel ID for the incident log channel.")
        node("mute-role", "Role ID for the mute role.")
    }

    override fun onEnable() {
        punishmentHandler = PunishmentHandler(
            source.guildConfigurationManager,
            source.mongodb
        )
        registerCommands(
            ClearCommand(),
            WarnCommand(),
            KickCommand(),
            MuteCommand(),
            TempbanCommand(),
            BanCommand(),
            UnmuteCommand(),
            UnbanCommand(),
            CaseCommand()
        )
        punishmentHandler.expireOldIncidents {
            source.shardManager.guilds
        }
    }

    companion object {
        @JvmStatic
        lateinit var punishmentHandler: PunishmentHandler
            private set
    }
}