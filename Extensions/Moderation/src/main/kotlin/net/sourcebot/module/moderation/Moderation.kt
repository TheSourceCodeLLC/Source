package net.sourcebot.module.moderation

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import net.dv8tion.jda.api.entities.Guild
import net.sourcebot.Source
import net.sourcebot.api.configuration.ConfigurationInfo
import net.sourcebot.api.module.SourceModule
import net.sourcebot.module.moderation.command.*
import net.sourcebot.module.moderation.listener.MessageListener
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

class Moderation : SourceModule() {
    override val configurationInfo = ConfigurationInfo("moderation") {
        section("advertising") {
            node("whitelist", "Array of Guild IDs that users may send invites to.")
            node("member-limit", "Minimum members in a Guild invite to be ignored.")
        }
        section("message-log") {
            node("channel", "Channel ID for the message log channel.")
            node("blacklist", "Channel IDs blacklisted from message logging.")
        }
        node("mention-threshold", "Maximum number of member mentions permitted in a message.")
        node("incident-log", "Channel ID for the incident log channel.")
        node("report-log", "Channel ID for the reports channel.")
        node("blacklist-role", "Role ID for the blacklist role.")
        node("mute-role", "Role ID for the mute role.")
    }

    private lateinit var task: ScheduledFuture<*>
    override fun enable() {
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
            BlacklistCommand(),
            UnblacklistCommand(),
            RolesCommand()
        )
        subscribeEvents(MessageListener())
        task = Source.SCHEDULED_EXECUTOR_SERVICE.scheduleAtFixedRate({
            Source.SHARD_MANAGER.guilds.forEach {
                Source.EXECUTOR_SERVICE.submit {
                    getPunishmentHandler(it) {
                        expireOldIncidents()
                        doPointDecay()
                    }
                }
            }
        }, 0L, 1L, TimeUnit.SECONDS)
    }

    override fun disable() {
        task.cancel(true)
    }

    companion object {
        private val punishmentHandlers = CacheBuilder.newBuilder()
            .weakKeys().expireAfterWrite(10, TimeUnit.MINUTES)
            .build(object : CacheLoader<Guild, PunishmentHandler>() {
                override fun load(guild: Guild) = PunishmentHandler(guild)
            })

        fun <T> getPunishmentHandler(
            guild: Guild, block: PunishmentHandler.() -> T
        ) = punishmentHandlers[guild].block()

        fun getPunishmentHandler(guild: Guild): PunishmentHandler = punishmentHandlers[guild]
    }
}