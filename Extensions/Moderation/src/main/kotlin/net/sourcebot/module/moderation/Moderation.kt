package net.sourcebot.module.moderation

import net.sourcebot.api.configuration.ConfigurationInfo
import net.sourcebot.api.module.SourceModule
import net.sourcebot.module.moderation.command.*
import net.sourcebot.module.moderation.data.PunishCommand

class Moderation : SourceModule() {
    override val configurationInfo = ConfigurationInfo("moderation") {
        node("incident-log", "Channel ID for the incident log channel.")
        node("report-log", "Channel ID for the reports channel.")
        node("mute-role", "Role ID for the mute role.")
    }

    override fun onEnable() {
        punishmentHandler = PunishmentHandler(
            source.configurationManager,
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
            CaseCommand(),
            HistoryCommand(),
            ReportCommand(),
            PunishCommand()
        )
        punishmentHandler.performTasks {
            source.shardManager.guilds
        }
    }

    companion object {
        @JvmStatic
        lateinit var punishmentHandler: PunishmentHandler
            private set
    }
}