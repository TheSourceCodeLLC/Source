package net.sourcebot.module.moderation

import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.TextChannel
import net.sourcebot.Source
import net.sourcebot.api.configuration.ConfigurationManager
import net.sourcebot.api.database.MongoDB
import net.sourcebot.api.durationOf
import net.sourcebot.api.formatted
import net.sourcebot.api.response.Response
import net.sourcebot.api.response.StandardErrorResponse
import net.sourcebot.api.response.StandardSuccessResponse
import net.sourcebot.module.moderation.data.*
import net.sourcebot.module.moderation.data.Incident.Type
import org.bson.Document
import java.time.Duration
import java.time.Instant
import java.util.*
import java.util.concurrent.TimeUnit

class PunishmentHandler(
    private val configurationManager: ConfigurationManager,
    private val mongo: MongoDB
) {
    fun clearIncident(
        guild: Guild, sender: Member, channel: TextChannel, amount: Int, reason: String
    ) = submitIncident(guild, {
        ClearIncident(guild, nextIncidentId(guild), sender, channel, amount, reason)
    }, { ClearSuccessResponse(it.id, amount, channel.name) }, ::ClearFailureResponse)

    private class ClearSuccessResponse(
        id: Long, amount: Int, channel: String
    ) : StandardSuccessResponse(
        "Clear Success (#$id)",
        "You have cleared $amount messages in channel `$channel`!"
    )

    private class ClearFailureResponse : StandardErrorResponse(
        "Clear Failure!", "Could not execute clear incident!"
    )

    fun warnIncident(
        sender: Member, warned: Member, reason: String
    ): Response {
        val guild = sender.guild
        if (sender == warned) return WarnFailureResponse("You may not warn yourself!")
        if (warned.user.isBot) return WarnFailureResponse("You may not warn bots!")
        if (!guild.selfMember.canInteract(warned)) return WarnFailureResponse(
            "I do not have permission to warn that member!"
        )
        if (!sender.canInteract(warned)) return WarnFailureResponse(
            "You do not have permission to warn that member!"
        )
        return submitIncident(guild, {
            WarnIncident(nextIncidentId(guild), sender, warned, reason)
        }, {
            StandardSuccessResponse(
                "Warn Success (#${it.id})",
                "You have successfully warned ${it.warned.formatted()}!"
            )
        }, { WarnFailureResponse("Could not execute warn incident!") })
    }

    private class WarnFailureResponse(
        description: String
    ) : StandardErrorResponse("Warn Failure!", description)

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
            KickIncident(nextIncidentId(guild), sender, kicked, reason)
        }, {
            StandardSuccessResponse(
                "Kick Success (#${it.id})",
                "You have successfully kicked ${it.kicked.formatted()}!"
            )
        }, { KickFailureResponse("Could not execute kick incident!") })
    }

    private class KickFailureResponse(
        description: String
    ) : StandardErrorResponse("Kick Failure!", description)

    fun muteIncident(
        sender: Member, muted: Member, duration: Duration, reason: String
    ): Response {
        val guild = sender.guild
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
            MuteIncident(nextIncidentId(guild), muteRole, sender, muted, duration, reason)
        }, {
            StandardSuccessResponse(
                "Mute Success (#${it.id})",
                "You have successfully muted ${it.muted.formatted()}!"
            )
        }, { MuteFailureResponse("Could not execute mute incident!") })
    }

    private class MuteFailureResponse(
        description: String
    ) : StandardErrorResponse("Mute Failure!", description)

    fun tempbanIncident(
        sender: Member, tempbanned: Member, delDays: Int, duration: Duration, reason: String
    ): Response {
        val guild = sender.guild
        if (sender == tempbanned) return TempbanFailureResponse("You may not tempban yourself!")
        if (tempbanned.user.isBot) return TempbanFailureResponse("You may not tempban bots!")
        if (!guild.selfMember.canInteract(tempbanned)) return TempbanFailureResponse(
            "I do not have permission to tempban that member!"
        )
        if (!sender.canInteract(tempbanned)) return TempbanFailureResponse(
            "You do not have permission to tempban that member!"
        )
        return submitIncident(guild, {
            TempbanIncident(nextIncidentId(guild), sender, tempbanned, delDays, duration, reason)
        }, {
            StandardSuccessResponse(
                "Tempban Success (#${it.id})",
                "You have successfully tempbanned ${it.tempbanned.formatted()}!"
            )
        }, { TempbanFailureResponse("Could not execute tempban incident!") })
    }

    private class TempbanFailureResponse(
        description: String
    ) : StandardErrorResponse("Tempban Failure!", description)

    fun banIncident(
        sender: Member, banned: Member, delDays: Int, reason: String
    ): Response {
        val guild = sender.guild
        if (sender == banned) return BanFailureResponse("You may not ban yourself!")
        if (banned.user.isBot) return BanFailureResponse("You may not ban bots!")
        if (!guild.selfMember.canInteract(banned)) return BanFailureResponse(
            "I do not have permission to ban that member!"
        )
        if (!sender.canInteract(banned)) return BanFailureResponse(
            "You do not have permission to ban that member!"
        )
        return submitIncident(guild, {
            BanIncident(nextIncidentId(guild), sender, banned, delDays, reason)
        }, {
            StandardSuccessResponse(
                "Ban Success (#${it.id})",
                "You have successfully banned ${it.banned.formatted()}!"
            )
        }, { BanFailureResponse("Could not execute ban incident!") })
    }

    private class BanFailureResponse(
        description: String
    ) : StandardErrorResponse("Ban Failure!", description)

    fun unmuteIncident(
        sender: Member, unmuted: Member, reason: String
    ): Response {
        val guild = sender.guild
        if (sender == unmuted) return UnmuteFailureResponse("You may not unmute yourself!")
        if (unmuted.user.isBot) return UnmuteFailureResponse("You may not unmute bots!")
        if (!guild.selfMember.canInteract(unmuted)) return UnmuteFailureResponse(
            "I do not have permission to unmute that member!"
        )
        if (!sender.canInteract(unmuted)) return UnmuteFailureResponse(
            "You do not have permission to unban that member!"
        )
        val muteRole = getMuteRole(guild) ?: return StandardErrorResponse(
            "No Mute Role!", "The mute role has not been configured!"
        )
        return submitIncident(guild, {
            UnmuteIncident(nextIncidentId(guild), muteRole, sender, unmuted, reason)
        }, {
            StandardSuccessResponse(
                "Unmute Success (#${it.id})",
                "You have successfully unmuted ${it.unmuted.formatted()}!"
            )
        }, { UnmuteFailureResponse("Could not execute unmute incident!") })
    }

    private class UnmuteFailureResponse(
        description: String
    ) : StandardErrorResponse("Unmute Failure!", description)

    fun unbanIncident(
        sender: Member, unbanned: String, reason: String
    ): Response {
        val guild = sender.guild
        val ban = try {
            guild.retrieveBanById(unbanned).complete()
        } catch (err: Throwable) {
            return StandardErrorResponse("Unknown Ban!", "The specified user is not banned!")
        }
        return submitIncident(guild, {
            UnbanIncident(nextIncidentId(guild), sender, ban.user, reason)
        }, {
            StandardSuccessResponse(
                "Unban Success (#${it.id})",
                "You have successfully unbanned ${it.unbanned.formatted()}!"
            )
        }, { UnbanFailureResponse("Could not execute unban incident!") })
    }

    private class UnbanFailureResponse(
        description: String
    ) : StandardErrorResponse("Unban Failure!", description)

    private val pointMap = TreeMap<Int, Pair<Type, Duration?>>().apply {
        put(8, Type.WARN to null)
        put(11, Type.MUTE to durationOf("15m"))
        put(16, Type.MUTE to durationOf("30m"))
        put(21, Type.MUTE to durationOf("1h"))
        put(26, Type.MUTE to durationOf("2h"))
        put(31, Type.MUTE to durationOf("3h"))
        put(36, Type.MUTE to durationOf("4h"))
        put(41, Type.MUTE to durationOf("5h"))
        put(46, Type.MUTE to durationOf("6h"))
        put(51, Type.MUTE to durationOf("1d"))
        put(56, Type.MUTE to durationOf("2d"))
        put(61, Type.MUTE to durationOf("3d"))
        put(66, Type.TEMPBAN to durationOf("1d"))
        put(71, Type.TEMPBAN to durationOf("2d"))
        put(76, Type.TEMPBAN to durationOf("3d"))
        put(81, Type.TEMPBAN to durationOf("4d"))
        put(86, Type.TEMPBAN to durationOf("5d"))
        put(91, Type.TEMPBAN to durationOf("1w"))
        put(96, Type.TEMPBAN to durationOf("2w"))
        put(100, Type.BAN to null)
    }

    fun punishMember(sender: Member, target: Member, id: Int): Response {
        val guild = sender.guild
        if (sender == target) return PunishFailureResponse("You may not punish yourself!")
        if (target.user.isBot) return PunishFailureResponse("You may not punish bots!")
        if (!guild.selfMember.canInteract(target)) return PunishFailureResponse(
            "I do not have permission to punish that member!"
        )
        if (!sender.canInteract(target)) return WarnFailureResponse(
            "You do not have permission to punish that member!"
        )
        val punishment = getPunishment(sender.guild, id) ?: return StandardErrorResponse(
            "Invalid Punishment!", "There is no punishment with the ID `$id`!"
        )
        val points = getPoints(target)
        val toAdd = when (punishment["level"] as Int) {
            1 -> 2.5
            2 -> 10.0
            3 -> 65.0
            4 -> 100.0
            else -> 0.0
        }
        val effective = points + toAdd
        val reason = "${punishment["reason"] as String} (Punishments vary based on your history)"
        val (type, duration) = pointMap.ceilingEntry(effective.toInt()).value
        return when (type) {
            Type.WARN -> submitIncident(guild, {
                WarnIncident(nextIncidentId(guild), sender, target, reason, toAdd)
            }, {
                StandardSuccessResponse(
                    "Warn Success (#${it.id})",
                    "You have successfully warned ${it.warned.formatted()}!"
                )
            }, { WarnFailureResponse("Could not execute warn incident!") })
            Type.MUTE -> {
                val muteRole = getMuteRole(guild) ?: return MuteFailureResponse(
                    "The mute role has not been configured!"
                )
                return submitIncident(guild, {
                    MuteIncident(nextIncidentId(guild), muteRole, sender, target, duration!!, reason, toAdd)
                }, {
                    StandardSuccessResponse(
                        "Mute Success (#${it.id})",
                        "You have successfully muted ${it.muted.formatted()}!"
                    )
                }, { MuteFailureResponse("Could not execute mute incident!") })
            }
            Type.TEMPBAN -> submitIncident(guild, {
                TempbanIncident(nextIncidentId(guild), sender, target, 7, duration!!, reason, toAdd)
            }, {
                StandardSuccessResponse(
                    "Tempban Success (#${it.id})",
                    "You have successfully tempbanned ${it.tempbanned.formatted()}!"
                )
            }, { TempbanFailureResponse("Could not execute tempban incident!") })
            Type.BAN -> submitIncident(guild, {
                BanIncident(nextIncidentId(guild), sender, target, 7, reason, toAdd)
            }, {
                StandardSuccessResponse(
                    "Ban Success (#${it.id})",
                    "You have successfully banned ${it.banned.formatted()}!"
                )
            }, { BanFailureResponse("Could not execute ban incident!") })
            else -> StandardErrorResponse("Unmet condition in 'when'!")
        }
    }

    private class PunishFailureResponse(
        description: String
    ) : StandardErrorResponse("Punish Failure!", description)

    private fun getPunishment(guild: Guild, id: Int) =
        punishmentCollection(guild).find(Document("_id", id)).first()

    private fun punishmentCollection(guild: Guild) =
        mongo.getCollection(guild.id, "punishments")

    private fun <T : ExecutableIncident> submitIncident(
        guild: Guild,
        supplier: () -> T,
        onSuccess: (T) -> StandardSuccessResponse,
        onFailure: () -> StandardErrorResponse
    ): Response {
        val logChannel = getIncidentChannel(guild) ?: return StandardErrorResponse(
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
    private fun nextIncidentId(guild: Guild) = incidentCollection(guild).countDocuments() + 1

    private fun getIncidentChannel(
        guild: Guild
    ) = configurationManager[guild].optional<String>(
        "moderation.incident-log"
    )?.let(guild::getTextChannelById)

    private fun reportCollection(guild: Guild) = mongo.getCollection(guild.id, "reports")
    private fun getReportChannel(
        guild: Guild
    ) = configurationManager[guild].optional<String>(
        "moderation.report-log"
    )?.let(guild::getTextChannelById)

    fun submitReport(
        sender: Member,
        target: Member,
        reason: String
    ): Response {
        val guild = sender.guild
        val channel = getReportChannel(guild) ?: return StandardErrorResponse(
            "No Log Channel!", "The report log has not been configured!"
        )
        val collection = reportCollection(guild)
        val id = collection.countDocuments() + 1
        Report(id, sender, target, reason).also {
            collection.insertOne(it.asDocument())
            it.send(channel)
        }
        return StandardSuccessResponse(
            "Report Submit! #$id",
            "You have submit a report against ${target.formatted()}!"
        )
    }

    private fun getMuteRole(guild: Guild) = configurationManager[guild].optional<String>(
        "moderation.mute-role"
    )?.let(guild::getRoleById)

    fun getCase(guild: Guild, id: Long): Case? =
        incidentCollection(guild).find(Document("_id", id)).first()?.let(::Case)

    fun deleteCase(guild: Guild, id: Long): Response {
        val collection = incidentCollection(guild)
        return collection.find(Document("_id", id)).first()?.let {
            collection.deleteOne(Document("_id", id))
            StandardSuccessResponse("Case Deleted!", "Case #$id has been deleted!")
        } ?: StandardErrorResponse("Invalid Case ID!", "There is no case with ID #$id!")
    }

    private fun getHistory(guild: Guild, id: String): List<Case> =
        incidentCollection(guild).find(Document("target", id))
            .map(::Case)
            .toList()

    fun getHistory(member: Member): List<Case> = getHistory(member.guild, member.id)

    fun performTasks(guilds: () -> Collection<Guild>) {
        Source.SCHEDULED_EXECUTOR_SERVICE.scheduleAtFixedRate(
            {
                guilds().forEach { guild ->
                    expireOldIncidents(guild)
                    doPointDecay(guild)
                }
            }, 0, 1, TimeUnit.SECONDS
        )
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
                    unmuteIncident(guild.selfMember, muted, reason)
                }
                "TEMPBAN" -> unbanIncident(guild.selfMember, it["target"] as String, reason)
            }
            collection.updateOne(it, Document("\$set", Document("resolved", true)))
        }
    }

    private fun doPointDecay(guild: Guild) = incidentCollection(guild).let { incidents ->
        incidents.find(Document("points", Document("\$exists", true))).forEach {
            val points = it["points"] as Document
            var decay = points["decay"] as Long
            if (--decay <= 0) incidents.updateOne(it, Document("\$unset", Document("points", "")))
            else incidents.updateOne(it, Document("\$set", Document("points", Document("decay", decay).also {
                it["value"] = points["value"]
            })))
        }
    }

    private fun getPoints(guild: Guild, id: String): Double =
        incidentCollection(guild).find(Document().also {
            it["target"] = id
            it["points"] = Document("\$exists", true)
        }).sumByDouble {
            (it["points"] as Document)["value"] as Double
        }

    fun getPoints(member: Member) = getPoints(member.guild, member.id)
}