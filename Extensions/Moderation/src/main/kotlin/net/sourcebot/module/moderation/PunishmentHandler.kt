package net.sourcebot.module.moderation

import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.TextChannel
import net.sourcebot.Source
import net.sourcebot.api.configuration.ConfigurationManager
import net.sourcebot.api.database.MongoDB
import net.sourcebot.api.formatted
import net.sourcebot.api.response.ErrorResponse
import net.sourcebot.api.response.Response
import net.sourcebot.api.response.SuccessResponse
import net.sourcebot.module.moderation.data.*
import org.bson.Document
import java.time.Duration
import java.time.Instant
import java.util.concurrent.TimeUnit

class PunishmentHandler(
    private val configurationManager: ConfigurationManager,
    private val mongo: MongoDB
) {
    //TODO: Punishment points
    private val ONE_DAY: Duration = Duration.ofSeconds(86400L)
    private val ONE_WEEK = ONE_DAY.multipliedBy(7)
    private val HALF_MONTH = ONE_WEEK.multipliedBy(2)
    private val ONE_MONTH = HALF_MONTH.multipliedBy(2)

    fun clearIncident(
        guild: Guild, sender: Member, channel: TextChannel, amount: Int, reason: String
    ) = submitIncident(guild, {
        ClearIncident(guild, getNextId(guild), sender, channel, amount, reason)
    }, {
        SuccessResponse(
            "Clear Success (#${it.id})",
            "You have cleared $amount messages in channel `${channel.name}`!"
        )
    }, { ClearFailureResponse("Could not execute clear incident!") })

    private class ClearFailureResponse(
        description: String
    ) : ErrorResponse("Clear Failure!", description)

    fun warnIncident(
        guild: Guild, sender: Member, warned: Member, reason: String
    ): Response {
        if (sender == warned) return WarnFailureResponse("You may not warn yourself!")
        if (warned.user.isBot) return WarnFailureResponse("You may not warn bots!")
        if (!guild.selfMember.canInteract(warned)) return WarnFailureResponse(
            "I do not have permission to warn that member!"
        )
        if (!sender.canInteract(warned)) return WarnFailureResponse(
            "You do not have permission to warn that member!"
        )
        return submitIncident(guild, {
            WarnIncident(getNextId(guild), sender, warned, reason)
        }, {
            SuccessResponse(
                "Warn Success (#${it.id})",
                "You have successfully warned ${it.warned.formatted()}!"
            )
        }, { WarnFailureResponse("Could not execute warn incident!") })
    }

    private class WarnFailureResponse(
        description: String
    ) : ErrorResponse("Warn Failure!", description)

    fun kickIncident(
        guild: Guild, sender: Member, kicked: Member, reason: String
    ): Response {
        if (sender == kicked) return KickFailureResponse("You may not kick yourself!")
        if (kicked.user.isBot) return KickFailureResponse("You may not kick bots!")
        if (!guild.selfMember.canInteract(kicked)) return KickFailureResponse(
            "I do not have permission to kick that member!"
        )
        if (!sender.canInteract(kicked)) return KickFailureResponse(
            "You do not have permission to kick that member!"
        )
        return submitIncident(guild, {
            KickIncident(getNextId(guild), sender, kicked, reason)
        }, {
            SuccessResponse(
                "Kick Success (#${it.id})",
                "You have successfully kicked ${it.kicked.formatted()}!"
            )
        }, { KickFailureResponse("Could not execute kick incident!") })
    }

    private class KickFailureResponse(
        description: String
    ) : ErrorResponse("Kick Failure!", description)

    fun muteIncident(
        guild: Guild, sender: Member, muted: Member, duration: Duration, reason: String
    ): Response {
        if (sender == muted) return MuteFailureResponse("You may not mute yourself!")
        if (muted.user.isBot) return MuteFailureResponse("You may not mute bots!")
        if (!guild.selfMember.canInteract(muted)) return MuteFailureResponse(
            "I do not have permission to mute that member!"
        )
        if (!sender.canInteract(muted)) return MuteFailureResponse(
            "You do not have permission to mute that member!"
        )
        val muteRole = getMuteRole(guild) ?: return MuteFailureResponse(
            "The mute role has not been configured!"
        )
        return submitIncident(guild, {
            MuteIncident(getNextId(guild), muteRole, sender, muted, duration, reason)
        }, {
            SuccessResponse(
                "Mute Success (#${it.id})",
                "You have successfully muted ${it.muted.formatted()}!"
            )
        }, { MuteFailureResponse("Could not execute mute incident!") })
    }

    private class MuteFailureResponse(
        description: String
    ) : ErrorResponse("Mute Failure!", description)

    fun tempbanIncident(
        guild: Guild, sender: Member, tempbanned: Member, delDays: Int, duration: Duration, reason: String
    ): Response {
        if (sender == tempbanned) return TempbanFailureResponse("You may not tempban yourself!")
        if (tempbanned.user.isBot) return TempbanFailureResponse("You may not tempban bots!")
        if (!guild.selfMember.canInteract(tempbanned)) return TempbanFailureResponse(
            "I do not have permission to tempban that member!"
        )
        if (!sender.canInteract(tempbanned)) return TempbanFailureResponse(
            "You do not have permission to tempban that member!"
        )
        return submitIncident(guild, {
            TempbanIncident(getNextId(guild), sender, tempbanned, delDays, duration, reason)
        }, {
            SuccessResponse(
                "Tempban Success (#${it.id})",
                "You have successfully tempbanned ${it.tempbanned.formatted()}!"
            )
        }, { TempbanFailureResponse("Could not execute tempban incident!") })
    }

    private class TempbanFailureResponse(
        description: String
    ) : ErrorResponse("Tempban Failure!", description)

    fun banIncident(
        guild: Guild, sender: Member, banned: Member, delDays: Int, reason: String
    ): Response {
        if (sender == banned) return BanFailureResponse("You may not ban yourself!")
        if (banned.user.isBot) return BanFailureResponse("You may not ban bots!")
        if (!guild.selfMember.canInteract(banned)) return BanFailureResponse(
            "I do not have permission to ban that member!"
        )
        if (!sender.canInteract(banned)) return BanFailureResponse(
            "You do not have permission to ban that member!"
        )
        return submitIncident(guild, {
            BanIncident(getNextId(guild), sender, banned, delDays, reason)
        }, {
            SuccessResponse(
                "Ban Success (#${it.id})",
                "You have successfully banned ${it.banned.formatted()}!"
            )
        }, { BanFailureResponse("Could not execute ban incident!") })
    }

    private class BanFailureResponse(
        description: String
    ) : ErrorResponse("Ban Failure!", description)

    fun unmuteIncident(
        guild: Guild, sender: Member, unmuted: Member, reason: String
    ): Response {
        if (sender == unmuted) return UnmuteFailureResponse("You may not unmute yourself!")
        if (unmuted.user.isBot) return UnmuteFailureResponse("You may not unmute bots!")
        if (!guild.selfMember.canInteract(unmuted)) return UnmuteFailureResponse(
            "I do not have permission to unmute that member!"
        )
        if (!sender.canInteract(unmuted)) return UnmuteFailureResponse(
            "You do not have permission to unban that member!"
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
        }, { UnmuteFailureResponse("Could not execute unmute incident!") })
    }

    private class UnmuteFailureResponse(
        description: String
    ) : ErrorResponse("Unmute Failure!", description)

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
        }, { UnbanFailureResponse("Could not execute unban incident!") })
    }

    private class UnbanFailureResponse(
        description: String
    ) : ErrorResponse("Unban Failure!", description)

    private fun <T : ExecutableIncident> submitIncident(
        guild: Guild,
        supplier: () -> T,
        onSuccess: (T) -> SuccessResponse,
        onFailure: () -> ErrorResponse
    ): Response {
        val logChannel = getIncidentChannel(guild) ?: return ErrorResponse(
            "No Log Channel!", "The incident log has not been configured!"
        )
        return try {
            val incident = supplier()
            incident.execute()
            incident.sendLog(logChannel)
            incidentCollection(guild).insertOne(incident.asDocument())
            onSuccess(incident)
        } catch (err: Throwable) {
            onFailure().apply { addField("Exception:", err.toString(), false) }
        }
    }

    private fun incidentCollection(guild: Guild) = mongo.getCollection(guild.id, "incidents")
    private fun getNextId(guild: Guild) = incidentCollection(guild).countDocuments() + 1

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

    fun getCase(
        guild: Guild, id: Long
    ): Case? = incidentCollection(guild).find(Document("_id", id)).first()?.let(::Case)

    fun getHistory(
        guild: Guild, id: String
    ): List<Case> = incidentCollection(guild).find(Document("target", id)).map(::Case).toList()

    fun getHistory(
        member: Member
    ): List<Case> = getHistory(member.guild, member.id)

    fun performTasks(
        guilds: () -> Collection<Guild>
    ) {
        Source.SCHEDULED_EXECUTOR_SERVICE.scheduleAtFixedRate({
            guilds().forEach { guild ->
                expireOldIncidents(guild)
                doPointDecay(guild)
            }
        }, 0, 1, TimeUnit.SECONDS)
    }

    private fun expireOldIncidents(guild: Guild) {
        val reason = "The punishment has expired."
        val collection = incidentCollection(guild)
        val query = Document().apply {
            this["expiry"] = Document().apply {
                this["\$exists"] = true
                this["\$lte"] = Instant.now().toEpochMilli()
            }
            this["resolved"] = Document("\$ne", true)
        }
        collection.find(query).forEach {
            when (it["type"] as String) {
                "MUTE" -> {
                    val muted = (it["target"] as String).let(guild::getMemberById)!!
                    unmuteIncident(guild, guild.selfMember, muted, reason)
                }
                "TEMPBAN" -> {
                    unbanIncident(guild, guild.selfMember, it["target"] as String, reason)
                }
            }
            collection.updateOne(it, Document("\$set", Document("resolved", true)))
        }
    }

    private fun doPointDecay(guild: Guild) {
        val collection = incidentCollection(guild)
        val query = Document().apply {
            this["points"] = Document().apply {
                this["\$exists"] = true
            }
        }
        collection.find(query).forEach {
            val points = it["points"] as Document
            var decay = points["decay"] as Long
            if (--decay <= 0) collection.updateOne(it, Document("\$unset", Document("points", "")))
            else collection.updateOne(it, Document("\$set", Document("points.decay", decay)))
        }
    }
}