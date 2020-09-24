package net.sourcebot.module.moderation

import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.TextChannel
import net.sourcebot.api.configuration.GuildConfigurationManager
import net.sourcebot.api.database.MongoDB
import net.sourcebot.api.formatted
import net.sourcebot.api.response.ErrorResponse
import net.sourcebot.api.response.Response
import net.sourcebot.api.response.SuccessResponse
import net.sourcebot.module.moderation.data.*
import java.time.Duration

class PunishmentHandler(
    private val configurationManager: GuildConfigurationManager,
    private val mongo: MongoDB
) {

    fun clearIncident(
        guild: Guild, sender: Member, channel: TextChannel, amount: Int, reason: String
    ) = submitIncident(guild, {
        ClearIncident(guild, getNextId(guild), sender, channel, amount, reason)
    }, {
        SuccessResponse(
            "Clear Success (#${it.id})",
            "You have cleared $amount messages in channel `${channel.name}`!"
        )
    }, "Clear Failure!", "Could not execute clear incident!")

    fun warnIncident(
        guild: Guild, sender: Member, warned: Member, reason: String
    ): Response {
        if (sender == warned) return ErrorResponse(
            "Warn Failure!", "You may not warn yourself!"
        )
        if (!sender.canInteract(warned)) return ErrorResponse(
            "Warn Failure!", "You do not have permission to warn that member!"
        )
        if (warned.user.isBot) return ErrorResponse(
            "Warn Failure!", "You may not warn bots!"
        )
        return submitIncident(guild, {
            WarnIncident(getNextId(guild), sender, warned, reason)
        }, {
            SuccessResponse(
                "Warn Success (#${it.id})",
                "You have successfully warned ${it.warned.formatted()}!"
            )
        }, "Warn Failure", "Could not execute warn incident!")
    }

    fun kickIncident(
        guild: Guild, sender: Member, kicked: Member, reason: String
    ): Response {
        if (sender == kicked) return ErrorResponse(
            "Kick Failure!", "You may not kick yourself!"
        )
        if (!sender.canInteract(kicked)) return ErrorResponse(
            "Kick Failure!", "You do not have permission to kick that member!"
        )
        if (kicked.user.isBot) return ErrorResponse(
            "Kick Failure!", "You may not kick bots!"
        )
        return submitIncident(guild, {
            KickIncident(getNextId(guild), sender, kicked, reason)
        }, {
            SuccessResponse(
                "Kick Success (#${it.id})",
                "You have successfully kicked ${it.kicked.formatted()}!"
            )
        }, "Kick Failure", "Could not execute kick incident!")
    }

    fun muteIncident(
        guild: Guild, sender: Member, muted: Member, duration: Duration, reason: String
    ): Response {
        if (sender == muted) return ErrorResponse(
            "Mute Failure!", "You may not mute yourself!"
        )
        if (!sender.canInteract(muted)) return ErrorResponse(
            "Mute Failure!", "You do not have permission to mute that member!"
        )
        if (muted.user.isBot) return ErrorResponse(
            "Mute Failure!", "You may not mute bots!"
        )
        val muteRole = getMuteRole(guild) ?: return ErrorResponse(
            "No Mute Role!", "The mute role has not been configured!"
        )
        return submitIncident(guild, {
            MuteIncident(getNextId(guild), muteRole, sender, muted, duration, reason)
        }, {
            SuccessResponse(
                "Mute Success (#${it.id})",
                "You have successfully muted ${it.muted.formatted()}!"
            )
        }, "Mute Failure", "Could not execute mute incident!")
    }

    fun tempbanIncident(
        guild: Guild, sender: Member, tempbanned: Member, delDays: Int, duration: Duration, reason: String
    ): Response {
        if (sender == tempbanned) return ErrorResponse(
            "Tempban Failure!", "You may not tempban yourself!"
        )
        if (!sender.canInteract(tempbanned)) return ErrorResponse(
            "Tempban Failure!", "You do not have permission to tempban that member!"
        )
        if (tempbanned.user.isBot) return ErrorResponse(
            "Tempban Failure!", "You may not tempban bots!"
        )
        return submitIncident(guild, {
            TempbanIncident(getNextId(guild), sender, tempbanned, delDays, duration, reason)
        }, {
            SuccessResponse(
                "Tempban Success (#${it.id})",
                "You have successfully tempbanned ${it.tempbanned.formatted()}!"
            )
        }, "Tempban Failure", "Could not execute tempban incident!")
    }

    fun banIncident(
        guild: Guild, sender: Member, banned: Member, delDays: Int, reason: String
    ): Response {
        if (sender == banned) return ErrorResponse(
            "Ban Failure!", "You may not ban yourself!"
        )
        if (!sender.canInteract(banned)) return ErrorResponse(
            "Ban Failure!", "You do not have permission to ban that member!"
        )
        if (banned.user.isBot) return ErrorResponse(
            "Ban Failure!", "You may not ban bots!"
        )
        return submitIncident(guild, {
            BanIncident(getNextId(guild), sender, banned, delDays, reason)
        }, {
            SuccessResponse(
                "Ban Success (#${it.id})",
                "You have successfully banned ${it.banned.formatted()}!"
            )
        }, "Ban Failure", "Could not execute ban incident!")
    }

    fun unmuteIncident(
        guild: Guild, sender: Member, unmuted: Member, reason: String
    ): Response {
        if (sender == unmuted) return ErrorResponse(
            "Unmute Failure!", "You may not unmute yourself!"
        )
        if (!sender.canInteract(unmuted)) return ErrorResponse(
            "Unmute Failure!", "You do not have permission to unban that member!"
        )
        if (unmuted.user.isBot) return ErrorResponse(
            "Unmute Failure!", "You may not unmute bots!"
        )
        val muteRole = getMuteRole(guild) ?: return ErrorResponse(
            "No Mute Role!", "The mute role has not been configured!"
        )
        return submitIncident(guild, {
            UnmuteIncident(getNextId(guild), muteRole, sender, unmuted, reason)
        }, {
            SuccessResponse(
                "Unmute Success (#${it.id})",
                "You have successfully unmuted ${it.unmuted.formatted()}!"
            )
        }, "Unmute Failure", "Could not execute unmute incident!")
    }

    fun unbanIncident(
        guild: Guild, sender: Member, unbanned: String, reason: String
    ): Response {
        val ban = try {
            guild.retrieveBanById(unbanned).complete()
        } catch (err: Throwable) {
            return ErrorResponse("Unknown Ban!", "The specified user is not banned!")
        }
        return submitIncident(guild, {
            UnbanIncident(getNextId(guild), sender, ban.user, reason)
        }, {
            SuccessResponse(
                "Unban Success (#${it.id})",
                "You have successfully unbanned ${it.unbanned.formatted()}!"
            )
        }, "Unban Failure", "Could not execute unban incident!")
    }

    private fun <T : Incident> submitIncident(
        guild: Guild,
        supplier: () -> T,
        onSuccess: (T) -> Response,
        errTitle: String,
        errDescription: String
    ): Response {
        val logChannel = getIncidentChannel(guild) ?: return ErrorResponse(
            "No Log Channel!", "The incident log has not been configured!"
        )
        return try {
            val incident = supplier()
            incident.execute()
            incident.sendLog(logChannel)
            getCollection(guild).insertOne(incident.asDocument())
            onSuccess(incident)
        } catch (err: Throwable) {
            ErrorResponse(
                errTitle,
                errDescription
            ).addField("Exception:", err.toString(), false) as Response
        }
    }

    private fun getCollection(guild: Guild) = mongo.getCollection(guild.id, "punishments")
    private fun getNextId(guild: Guild) = getCollection(guild).countDocuments() + 1

    private fun getIncidentChannel(
        guild: Guild
    ) = configurationManager[guild].optional<String>(
        "moderation.incident-log"
    )?.let(guild::getTextChannelById)

    private fun getMuteRole(
        guild: Guild
    ) = configurationManager[guild].optional<String>(
        "moderation.mute-role"
    )?.let(guild::getRoleById)
}