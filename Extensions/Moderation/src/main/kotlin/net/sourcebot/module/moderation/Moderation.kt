package net.sourcebot.module.moderation

import net.sourcebot.api.configuration.ConfigurationInfo
import net.sourcebot.api.module.SourceModule
import net.sourcebot.module.moderation.command.*
import net.sourcebot.module.moderation.listener.MessageListener

class Moderation : SourceModule() {
    override val configurationInfo = ConfigurationInfo("moderation") {
        node("mention-threshold", "Maximum number of member mentions permitted in a message.")
        node("incident-log", "Channel ID for the incident log channel.")
        node("message-log", "Channel ID for the message log channel.")
        node("report-log", "Channel ID for the reports channel.")
        node("blacklist-role", "Role ID for the blacklist role.")
        node("mute-role", "Role ID for the mute role.")
    }

    override fun onEnable() {
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
            PunishCommand(),
            OffensesCommand(),
            UnblacklistCommand()
        )
        subscribeEvents(MessageListener())
        PUNISHMENT_HANDLER.performTasks()
    }

    companion object {
        @JvmStatic
        val PUNISHMENT_HANDLER = PunishmentHandler()
    }
}