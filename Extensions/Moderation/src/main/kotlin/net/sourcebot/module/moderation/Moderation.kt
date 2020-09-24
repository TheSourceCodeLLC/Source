package net.sourcebot.module.moderation

import net.sourcebot.api.module.SourceModule
import net.sourcebot.module.moderation.command.*

class Moderation : SourceModule() {
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
            UnbanCommand()
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