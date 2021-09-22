package net.sourcebot.module.moderation

import net.dv8tion.jda.api.entities.*
import net.sourcebot.Source
import net.sourcebot.api.DurationUtils
import net.sourcebot.api.durationOf
import net.sourcebot.api.formatLong
import net.sourcebot.api.response.Response
import net.sourcebot.api.response.StandardErrorResponse
import net.sourcebot.api.response.StandardSuccessResponse
import net.sourcebot.api.response.StandardWarningResponse
import net.sourcebot.module.moderation.data.*
import net.sourcebot.module.moderation.data.Incident.Type
import net.sourcebot.module.moderation.data.Incident.Type.*
import net.sourcebot.module.moderation.data.RoleUpdateIncident.Action
import org.bson.Document
import java.time.Duration
import java.time.Instant
import java.util.*

@Suppress("NestedLambdaShadowedImplicitParameter")
class PunishmentHandler(private val guild: Guild) {
    private val configManager = Source.CONFIG_MANAGER
    private val mongo = Source.MONGODB

    private val blacklists = mongo.getCollection(guild.id, "blacklists")
    private val incidents = mongo.getCollection(guild.id, "incidents")
    private val offenses = mongo.getCollection(guild.id, "offenses")
    private val reports = mongo.getCollection(guild.id, "reports")
    private val selfMember = guild.selfMember

    private fun muteRole() = configManager[guild].optional<String>(
        "moderation.mute-role"
    )?.let(guild::getRoleById)

    private fun blacklistRole() = configManager[guild].optional<String>(
        "moderation.blacklist-role"
    )?.let(guild::getRoleById)

    fun incidentChannel() = configManager[guild].optional<String>(
        "moderation.incident-log"
    )?.let(guild::getTextChannelById)

    fun reportChannel() = configManager[guild].optional<String>(
        "moderation.report-log"
    )?.let(guild::getTextChannelById)

    fun clearIncident(sender: Member, channel: TextChannel, amount: Int, reason: String) = submitIncident(
        { ClearIncident(nextIncidentId(), sender, channel, amount, reason) },
        { ClearSuccessResponse(it.id, amount, channel.name) },
        ::ClearFailureResponse
    )

    private class ClearSuccessResponse(id: Long, amount: Int, channel: String) : PunishmentSuccessResponse(
        "Clear Success (#$id)",
        "You have cleared $amount messages in channel `$channel`!"
    )

    private class ClearFailureResponse : PunishmentFailureResponse(
        "Clear Failure!", "Could not execute clear incident!"
    )

    fun warnIncident(sender: Member, member: Member, reason: String): PunishmentResponse {
        if (sender == member) return WarnFailureResponse("You may not warn yourself!")
        if (member.user.isBot) return WarnFailureResponse("You may not warn bots!")
        if (!selfMember.canInteract(member)) return WarnFailureResponse(
            "I do not have permission to warn that member!"
        )
        if (!sender.canInteract(member)) return WarnFailureResponse(
            "You do not have permission to warn that member!"
        )
        return submitWarn(sender, member, reason)
    }

    private fun submitWarn(sender: Member, member: Member, reason: String) = submitIncident(
        { WarnIncident(nextIncidentId(), sender, member, reason) },
        { WarnSuccessResponse(it.id, it.member, it.reason) },
        { WarnFailureResponse("Could not execute warn incident!") }
    )

    private class WarnSuccessResponse(id: Long, warned: Member, reason: String) : PunishmentSuccessResponse(
        "Warn Success (#$id)",
        "Warned ${warned.formatLong()} for '$reason' !"
    )

    private class WarnFailureResponse(description: String) : PunishmentFailureResponse(
        "Warn Failure!", description
    )

    fun manualKickIncident(sender: Member, user: User): KickIncident {
        return KickIncident(nextIncidentId(), sender, user, "Manually Kicked")
    }

    fun kickIncident(sender: Member, member: Member, reason: String): PunishmentResponse {
        if (sender == member) return KickFailureResponse("You may not kick yourself!")
        if (member.user.isBot) return KickFailureResponse("You may not kick bots!")
        if (!selfMember.canInteract(member)) return KickFailureResponse(
            "I do not have permission to kick that member!"
        )
        if (!sender.canInteract(member)) return KickFailureResponse(
            "You do not have permission to kick that member!"
        )
        return submitKick(sender, member, reason)
    }

    private fun submitKick(sender: Member, member: Member, reason: String) = submitIncident(
        { KickIncident(nextIncidentId(), sender, member.user, reason) },
        { KickSuccessResponse(it.id, it.user, it.reason) },
        { KickFailureResponse("Could not execute kick incident!") }
    )

    private class KickSuccessResponse(id: Long, kicked: User, reason: String) : PunishmentSuccessResponse(
        "Kick Success (#$id)",
        "Kicked ${kicked.formatLong()} for '$reason' !"
    )

    private class KickFailureResponse(description: String) : PunishmentFailureResponse(
        "Kick Failure!", description
    )

    fun muteIncident(sender: Member, member: Member, duration: Duration, reason: String): PunishmentResponse {
        if (sender == member) return MuteFailureResponse("You may not mute yourself!")
        if (member.user.isBot) return MuteFailureResponse("You may not mute bots!")
        if (!selfMember.canInteract(member)) return MuteFailureResponse(
            "I do not have permission to mute that member!"
        )
        if (!sender.canInteract(member)) return MuteFailureResponse(
            "You do not have permission to mute that member!"
        )
        val muteRole = muteRole() ?: return MuteFailureResponse(
            "The mute role has not been configured!"
        )
        return submitMute(muteRole, sender, member, duration, reason)
    }

    private fun submitMute(
        muteRole: Role, sender: Member, member: Member, duration: Duration, reason: String
    ) = submitIncident(
        { MuteIncident(nextIncidentId(), muteRole, sender, member, duration, reason) },
        { MuteSuccessResponse(it.id, it.member, it.duration, it.reason) },
        { MuteFailureResponse("Could not execute mute incident!") }
    )

    private class MuteSuccessResponse(
        id: Long, member: Member, duration: Duration, reason: String
    ) : PunishmentSuccessResponse(
        "Mute Success (#$id)",
        "Muted ${member.formatLong()} for '$reason' ! (${duration.formatLong()})"
    )

    private class MuteFailureResponse(description: String) : PunishmentFailureResponse(
        "Mute Failure!", description
    )

    fun blacklistIncident(sender: Member, member: Member, id: Int): PunishmentResponse {
        if (sender == member) return BlacklistFailureResponse("You may not blacklist yourself!")
        if (member.user.isBot) return BlacklistFailureResponse("You may not blacklist bots!")
        if (!selfMember.canInteract(member)) return BlacklistFailureResponse(
            "I do not have permission to blacklist that member!"
        )
        if (!sender.canInteract(member)) return BlacklistFailureResponse(
            "You do not have permission to blacklist that member!"
        )
        val blacklistRole = blacklistRole() ?: return BlacklistFailureResponse(
            "The blacklist role has not been configured!"
        )
        val blacklist = getBlacklist(id) ?: return PunishmentFailureResponse(
            "Invalid Blacklist!", "There is no blacklist with the ID '$id'!"
        )
        val duration = Duration.ofSeconds(blacklist["duration"] as Long)
        val reason = blacklist["reason"] as String
        return submitBlacklist(blacklistRole, sender, member, duration, reason)
    }

    private fun getBlacklist(id: Int) = blacklists.find().sortedBy { it["duration"] as Long }.getOrNull(id - 1)

    private fun submitBlacklist(
        muteRole: Role,
        sender: Member,
        member: Member,
        duration: Duration,
        reason: String,
    ) = submitIncident(
        { BlacklistIncident(nextIncidentId(), muteRole, sender, member, duration, reason) },
        { BlacklistSuccessResponse(it.id, it.member, it.duration, it.reason) },
        { BlacklistFailureResponse("Could not execute blacklist incident!") }
    )

    private class BlacklistSuccessResponse(
        id: Long, member: Member, duration: Duration, reason: String
    ) : PunishmentSuccessResponse(
        "Blacklist Success (#$id)",
        "Blacklisted ${member.formatLong()} for '$reason' ! (${duration.formatLong()})"
    )

    private class BlacklistFailureResponse(
        description: String
    ) : PunishmentFailureResponse("Blacklist Failure!", description)

    fun addBlacklist(
        duration: Duration,
        reason: String
    ): Response {
        blacklists.insertOne(Document().also {
            it["duration"] = duration.seconds
            it["reason"] = reason
        })
        return StandardSuccessResponse(
            "Blacklist Added!", "Added blacklist '$reason'!"
        )
    }

    fun removeBlacklist(id: Int): Response {
        val found = getBlacklist(id) ?: return StandardErrorResponse(
            "Invalid Blacklist!", "There is no blacklist with the ID '$id'!"
        )
        blacklists.deleteOne(found)
        return StandardSuccessResponse(
            "Blacklist Deleted!", "Deleted blacklist '${found["reason"] as String}'!"
        )
    }

    fun getBlacklists() = blacklists.find().sortedBy {
        it["duration"] as Long
    }.map {
        DurationUtils.formatSeconds(it["duration"] as Long) to it["reason"] as String
    }.withIndex().associateByTo(TreeMap(), { it.index }, { it.value })

    fun tempbanIncident(
        sender: Member, tempbanned: Member, delDays: Int, duration: Duration, reason: String
    ): PunishmentResponse {
        if (sender == tempbanned) return TempbanFailureResponse("You may not tempban yourself!")
        if (tempbanned.user.isBot) return TempbanFailureResponse("You may not tempban bots!")
        if (!selfMember.canInteract(tempbanned)) return TempbanFailureResponse(
            "I do not have permission to tempban that member!"
        )
        if (!sender.canInteract(tempbanned)) return TempbanFailureResponse(
            "You do not have permission to tempban that member!"
        )
        return submitTempban(sender, tempbanned, delDays, duration, reason)
    }

    private fun submitTempban(
        sender: Member, member: Member, delDays: Int, duration: Duration, reason: String
    ) = submitIncident(
        { TempbanIncident(nextIncidentId(), sender, member, delDays, duration, reason) },
        { TempbanSuccessResponse(it.id, it.member, it.duration, it.reason) },
        { TempbanFailureResponse("Could not execute tempban incident!") }
    )

    private class TempbanSuccessResponse(
        id: Long, member: Member, duration: Duration, reason: String
    ) : PunishmentSuccessResponse(
        "Tempban Success (#$id)",
        "Tempbanned ${member.formatLong()} for '$reason'! (${duration.formatLong()})"
    )

    private class TempbanFailureResponse(
        description: String
    ) : PunishmentFailureResponse("Tempban Failure!", description)

    fun manualBanIncident(sender: Member, member: User, delDays: Int, reason: String = "N/A"): BanIncident {
        return BanIncident(nextIncidentId(), sender, member, delDays, "Manually Banned For: $reason")
    }

    fun banIncident(
        sender: Member, member: Member, delDays: Int, reason: String
    ): PunishmentResponse {
        if (sender == member) return BanFailureResponse("You may not ban yourself!")
        if (member.user.isBot) return BanFailureResponse("You may not ban bots!")
        if (!selfMember.canInteract(member)) return BanFailureResponse(
            "I do not have permission to ban that member!"
        )
        if (!sender.canInteract(member)) return BanFailureResponse(
            "You do not have permission to ban that member!"
        )
        return submitBan(sender, member, delDays, reason)
    }

    private fun submitBan(
        sender: Member,
        member: Member,
        delDays: Int,
        reason: String
    ) = submitIncident(
        { BanIncident(nextIncidentId(), sender, member.user, delDays, reason) },
        { BanSuccessResponse(it.id, it.user, it.reason) },
        { BanFailureResponse("Could not execute ban incident!") }
    )

    private class BanSuccessResponse(
        id: Long, user: User, reason: String
    ) : PunishmentSuccessResponse(
        "Ban Success (#$id)",
        "Banned ${user.formatLong()} for '$reason'!"
    )

    private class BanFailureResponse(
        description: String
    ) : PunishmentFailureResponse("Ban Failure!", description)

    fun unblacklistIncident(sender: Member, member: Member, reason: String): PunishmentResponse {
        if (sender == member) return UnblacklistFailureResponse("You may not unblacklist yourself!")
        if (member.user.isBot) return UnblacklistFailureResponse("You may not unblacklist bots!")
        if (!selfMember.canInteract(member)) return UnblacklistFailureResponse(
            "I do not have permission to unblacklist that member!"
        )
        if (!sender.canInteract(member)) return UnblacklistFailureResponse(
            "You do not have permission to unblacklist that member!"
        )
        val blacklistRole = blacklistRole() ?: return PunishmentFailureResponse(
            "No Blacklist Role!", "The blacklist role has not been configured!"
        )
        if (!member.roles.contains(blacklistRole)) return UnblacklistFailureResponse(
            "That user is not blacklisted!"
        )
        return submitIncident(
            { UnblacklistIncident(nextIncidentId(), blacklistRole, sender, member, reason) },
            {
                incidents.updateMany(
                    Document("type", "BLACKLIST"),
                    Document("\$set", Document("resolved", true))
                )
                UnblacklistSuccessResponse(it.id, it.member, it.reason)
            },
            { UnblacklistFailureResponse("Could not execute unblacklist incident!") }
        )
    }

    private class UnblacklistSuccessResponse(id: Long, member: Member, reason: String) : PunishmentSuccessResponse(
        "Unblacklist Success (#$id)",
        "Unblacklisted ${member.formatLong()} for '$reason'!"
    )

    private class UnblacklistFailureResponse(description: String) : PunishmentFailureResponse(
        "Unblacklist Failure!", description
    )

    fun unmuteIncident(sender: Member, member: Member, reason: String): PunishmentResponse {
        if (sender == member) return UnmuteFailureResponse("You may not unmute yourself!")
        if (member.user.isBot) return UnmuteFailureResponse("You may not unmute bots!")
        if (!selfMember.canInteract(member)) return UnmuteFailureResponse(
            "I do not have permission to unmute that member!"
        )
        if (!sender.canInteract(member)) return UnmuteFailureResponse(
            "You do not have permission to unmute that member!"
        )
        val muteRole = muteRole() ?: return PunishmentFailureResponse(
            "No Mute Role!", "The mute role has not been configured!"
        )
        if (!member.roles.contains(muteRole)) return UnmuteFailureResponse(
            "That member is not muted!"
        )
        return submitIncident(
            { UnmuteIncident(nextIncidentId(), muteRole, sender, member, reason) },
            {
                incidents.updateMany(
                    Document("type", "MUTE"),
                    Document("\$set", Document("resolved", true))
                )
                UnmuteSuccessResponse(it.id, it.member, it.reason)
            },
            { UnmuteFailureResponse("Could not execute unmute incident!") }
        )
    }

    private class UnmuteSuccessResponse(
        id: Long, member: Member, reason: String
    ) : PunishmentSuccessResponse(
        "Unmute Success (#$id)",
        "Unmuted ${member.formatLong()} for '$reason'!"
    )

    private class UnmuteFailureResponse(
        description: String
    ) : PunishmentFailureResponse("Unmute Failure!", description)

    fun manualUnbanIncident(sender: Member, user: User): UnbanIncident {
        return UnbanIncident(nextIncidentId(), sender, user, "Manually Unbanned")
    }

    fun unbanIncident(sender: Member, user: String, reason: String): PunishmentResponse {
        val ban = kotlin.runCatching {
            guild.retrieveBanById(user).complete()
        }.getOrNull() ?: return PunishmentFailureResponse("Unknown Ban!", "The specified user is not banned!")
        return submitIncident(
            { UnbanIncident(nextIncidentId(), sender, ban.user, reason) },
            {
                incidents.updateMany(
                    Document("type", Document("\$in", listOf("TEMPBAN", "BAN"))),
                    Document("\$set", Document("resolved", true))
                )
                UnbanSuccessResponse(it.id, it.user, it.reason)
            },
            { UnbanFailureResponse("Could not execute unban incident!") }
        )
    }

    private class UnbanSuccessResponse(id: Long, user: User, reason: String) : PunishmentSuccessResponse(
        "Unban Success (#$id)",
        "Unbanned ${user.formatLong()} for '$reason'!"
    )

    private class UnbanFailureResponse(description: String) : PunishmentFailureResponse(
        "Unban Failure!", description
    )

    fun punishMember(sender: Member, target: Member, id: Int): PunishmentResponse {
        if (sender == target) return PunishFailureResponse("You may not punish yourself!")
        if (target.user.isBot) return PunishFailureResponse("You may not punish bots!")
        if (!selfMember.canInteract(target)) return PunishFailureResponse(
            "I do not have permission to punish that member!"
        )
        if (!sender.canInteract(target)) return PunishFailureResponse(
            "You do not have permission to punish that member!"
        )
        val offense = getOffense(id) ?: return PunishmentFailureResponse(
            "Invalid Punishment!", "There is no punishment with the ID `$id`!"
        )
        val points = getPoints(target.user)
        val effective = points + Level.values()[offense["level"] as Int - 1].points
        val reason = "${offense["name"] as String} (Punishments vary based on your history)"
        val (type, duration) =
            (pointMap.ceilingEntry(effective) ?: pointMap.floorEntry(effective)).value
        return when (type) {
            WARN -> submitWarn(sender, target, reason)
            KICK -> submitKick(sender, target, reason)
            MUTE -> {
                val muteRole = muteRole() ?: return MuteFailureResponse(
                    "The mute role has not been configured!"
                )
                submitMute(muteRole, sender, target, duration!!, reason)
            }
            TEMPBAN -> submitTempban(sender, target, 7, duration!!, reason)
            BAN -> submitBan(sender, target, 7, reason)
            else -> PunishmentFailureResponse("Unmet condition in 'when'!")
        }
    }

    private class PunishFailureResponse(description: String) : PunishmentFailureResponse(
        "Punish Failure!", description
    )

    fun getPoints(user: User): Double = incidents.find(Document().also {
        it["target"] = user.id
        it["points"] = Document("\$exists", true)
    }).sumByDouble { (it["points"] as Document)["value"] as Double }

    private fun getOffense(id: Int): Document? = offenses.find().sortedBy {
        it["level"] as Int
    }.getOrNull(id - 1)

    fun getOffenses() = offenses.find().sortedBy { it["level"] as Int }.withIndex().associateByTo(
        HashMap(), { it.index }, { it.value }
    )

    fun addOffense(level: Int, name: String): Response {
        val offense = Document().also {
            it["level"] = level
            it["name"] = name
        }
        offenses.insertOne(offense)
        return StandardSuccessResponse(
            "Offense Added!",
            "Added Level `$level` offense: `$name`!"
        )
    }

    fun removeOffense(id: Int): Response {
        val toRemove = getOffense(id) ?: return StandardErrorResponse(
            "Invalid Offense!", "There is no offense with the ID `$id`!"
        )
        val level = toRemove["level"] as Int
        val name = toRemove["name"] as String
        offenses.deleteOne(toRemove)
        return StandardSuccessResponse(
            "Offense Removed!",
            "Removed Level `$level` offense: `$name`!"
        )
    }

    fun manualRoleUpdate(
        sender: Member, target: Member, role: Role, action: Action
    ) = RoleUpdateIncident(nextIncidentId(), sender, target, role, "Manually Updated", action)

    private fun submitRoleUpdate(
        sender: Member, target: Member, role: Role, reason: String, action: Action, description: String
    ) = submitIncident(
        { RoleUpdateIncident(nextIncidentId(), sender, target, role, reason, action) },
        { RoleUpdateSuccess(it.id, description) },
        ::RoleUpdateFailure
    )

    fun submitRoleAdd(sender: Member, target: Member, role: Role, reason: String) = submitRoleUpdate(
        sender, target, role, reason, Action.ADD,
        "Added role ${role.name} to ${target.formatLong()} for '$reason'!"
    )

    fun submitRoleRemove(sender: Member, target: Member, role: Role, reason: String) = submitRoleUpdate(
        sender, target, role, reason, Action.REMOVE,
        "Removed role ${role.name} from ${target.formatLong()} for '$reason'!"
    )

    private class RoleUpdateSuccess(id: Long, description: String) : PunishmentSuccessResponse(
        "Role Update Success (#$id)", description
    )

    private class RoleUpdateFailure : PunishmentFailureResponse(
        "Role Update Failure!",
        "There was a problem updating that member's roles!"
    )

    fun logIncident(channel: TextChannel, incident: ExecutableIncident) {
        val message = incident.sendLog(channel)
        incidents.insertOne(incident.asDocument().also {
            it["message"] = message.id
        })
    }

    private fun <T : ExecutableIncident> submitIncident(
        supplier: () -> T,
        onSuccess: (T) -> PunishmentSuccessResponse,
        onFailure: () -> PunishmentFailureResponse
    ): PunishmentResponse {
        val logChannel = incidentChannel() ?: return PunishmentFailureResponse(
            "No Log Channel!", "The incident log has not been configured!"
        )
        return try {
            val incident = supplier()
            incident.execute()
            val message = incident.sendLog(logChannel)
            incidents.insertOne(incident.asDocument().also {
                it["message"] = message.id
            })
            onSuccess(incident)
        } catch (err: Throwable) {
            err.printStackTrace()
            onFailure().apply {
                addField(
                    "Exception:",
                    "${err.javaClass.simpleName}: ${err.message}",
                    false
                )
            }
        }
    }

    private fun nextIncidentId() = incidents.countDocuments() + 1

    fun submitReport(message: Message, target: Member, reason: String): Response {
        val sender = message.member!!
        val fromChannel = message.textChannel
        val channel = reportChannel() ?: return StandardErrorResponse(
            "No Log Channel!", "The report log has not been configured!"
        )
        val id = reports.countDocuments() + 1
        Report(id, sender.id, target.id, reason, fromChannel.id).also {
            reports.insertOne(it.asDocument())
            it.send(channel)
        }
        return StandardSuccessResponse(
            "Report Submit! #$id",
            "You have submit a report against ${target.formatLong()}!"
        )
    }

    fun getReportsAgainst(user: User) = reports.find(
        Document().also {
            it["handling.valid"] = Document("\$ne", false)
            it["deleted"] = Document("\$ne", true)
            it["target"] = user.id
        }
    ).map(::Report).toList()

    fun getHistory(id: String): List<Case> = incidents.find(Document("target", id)).map(::Case).toList()
    fun getCase(id: Long) = incidents.find(Document("_id", id)).first()?.let(::Case)
    fun deleteCase(member: Member, id: Long, reason: String) = submitIncident(
        { CaseDeleteIncident(incidents, id, member, selfMember, reason) },
        {
            val deleted = it.deleted!!
            (deleted["message"] as String?)?.let { incidentChannel()?.deleteMessageById(it)?.queue({}, {}) }
            val response = if (deleted["resolved"] as Boolean? != true) {
                val target = deleted["target"] as String
                when (deleted["type"] as String) {
                    "MUTE" -> runCatching {
                        unmuteIncident(member, guild.getMemberById(target)!!, "Case $id deleted: $reason")
                    }.getOrNull()
                    "BLACKLIST" -> runCatching {
                        unblacklistIncident(member, guild.getMemberById(target)!!, "Case $id deleted: $reason")
                    }.getOrNull()
                    "BAN" -> unbanIncident(member, target, "Case $id deleted: $reason")
                    else -> null
                }
            } else null
            if (response is PunishmentSuccessResponse) response
            else PunishmentSuccessResponse("Case Deleted!", "Case #$id has been deleted!")
        },
        { PunishmentFailureResponse("Case Delete Failure!", "Case #$id could not be deleted!") }
    )

    fun getReport(id: Long) = reports.find(Document("_id", id)).first()?.let(::Report)
    fun deleteReport(id: Long, member: Member, reason: String): Response {
        val query = Document("_id", id)
        val found = reports.find(query).first() ?: return StandardErrorResponse(
            "Invalid Report!", "There is no report with the ID '$id'!"
        )
        if (found["deleted"] as Boolean? == true) return StandardErrorResponse(
            "Invalid Report!", "That report has already been deleted!"
        )
        reports.updateOne(query, Document("\$set", Document().also {
            it["sender"] = member.id
            it["target"] = selfMember.id
            it["reason"] = reason
            it["time"] = Instant.now().toEpochMilli()
            it["handling"] = Document().also {
                it["valid"] = false
                it["handler"] = member.id
            }
            it["deleted"] = true
        }))
        return StandardSuccessResponse(
            "Report Deleted!", "Successfully deleted report #$id for '$reason'!"
        )
    }

    fun markReportHandled(id: Long, valid: Boolean, handler: String) {
        reports.updateOne(
            Document("_id", id),
            Document("\$set", Document("handling", Document().also {
                it["valid"] = valid
                it["handler"] = handler
            }))
        )
    }

    fun logAdvertising(message: Message) = reportChannel()?.let {
        val sender = message.member!!
        val channel = message.textChannel
        val embed = StandardWarningResponse(
            "Potential Advertising", """
                **User:** ${sender.formatLong()} (${sender.id})
                **Channel:** ${channel.name} (${channel.id})
            """.trimIndent()
        )
        it.sendMessage(embed.also {
            it.addField("Message:", message.contentRaw, false)
        }.asMessage(message.author)).queue {
            it.addReaction("✅").queue()
            it.addReaction("❌").queue()
        }
        true
    } ?: false

    internal fun expireOldIncidents() {
        val reason = "The punishment has expired."
        val query = Document().apply {
            this["expiry"] = Document().apply {
                this["\$exists"] = true
                this["\$lte"] = Instant.now().toEpochMilli()
            }
            this["resolved"] = Document("\$ne", true)
        }
        incidents.find(query).forEach {
            when (it["type"] as String) {
                "MUTE" -> {
                    val muted = (it["target"] as String).let(guild::getMemberById)!!
                    unmuteIncident(selfMember, muted, reason)
                }
                "TEMPBAN" -> unbanIncident(selfMember, it["target"] as String, reason)
                "BLACKLIST" -> {
                    val blacklisted = (it["target"] as String).let(guild::getMemberById)!!
                    unblacklistIncident(selfMember, blacklisted, reason)
                }
            }
            incidents.updateOne(it, Document("\$set", Document("resolved", true)))
        }
    }

    internal fun doPointDecay() = incidents.let { incidents ->
        incidents.find(Document("points", Document("\$exists", true))).forEach { incident ->
            val points = incident["points"] as Document
            var decay = points["decay"] as Long
            if (--decay <= 0) incidents.updateOne(incident, Document("\$unset", Document("points", "")))
            else incidents.updateOne(
                incident,
                Document("\$set", Document("points", Document("decay", decay).also {
                    it["value"] = points["value"]
                }))
            )
        }
    }

    companion object {
        private val pointMap = object : TreeMap<Double, Pair<Type, Duration?>>() {
            init {
                register( /* Level 1 - 1 Month Expiry */
                    Pair(5.0, WARN), Pair(10.0, WARN)
                )
                register( /* Level 2 - 3 Month Expiry */
                    Triple(15.0, MUTE, "30m"),
                    Triple(20.0, MUTE, "1h"),
                    Triple(25.0, MUTE, "2h"),
                    Triple(30.0, MUTE, "3h"),
                    Triple(35.0, MUTE, "6h"),
                    Triple(40.0, MUTE, "12h"),
                    Triple(45.0, MUTE, "1d")
                )
                register(
                    /* Level 3 - 6 Month Expiry */
                    Triple(50.0, TEMPBAN, "1d"),
                    Triple(55.0, TEMPBAN, "2d"),
                    Triple(60.0, TEMPBAN, "3d"),
                    Triple(65.0, TEMPBAN, "4d"),
                    Triple(70.0, TEMPBAN, "5d"),
                    Triple(75.0, TEMPBAN, "6d"),
                    Triple(80.0, TEMPBAN, "1w"),
                    Triple(85.0, TEMPBAN, "2w"),
                    Triple(90.0, TEMPBAN, "3w"),
                    Triple(95.0, TEMPBAN, "1M"),
                )
                register( /* Level 4 - 1 Year Expiry */
                    Pair(100.0, BAN)
                )
            }

            fun register(vararg pairs: Pair<Double, Type>) {
                register(*pairs.map { Triple(it.first, it.second, null) }.toTypedArray())
            }

            fun register(vararg triples: Triple<Double, Type, String?>) {
                triples.forEach {
                    val (points, type, duration) = it
                    val value = type to duration?.let(::durationOf)
                    put(points, value)
                }
            }
        }
    }
}

interface PunishmentResponse : Response {
    val success: Boolean
}

open class PunishmentSuccessResponse(
    title: String? = null, description: String? = null
) : StandardSuccessResponse(title, description), PunishmentResponse {
    final override val success = true
}

open class PunishmentFailureResponse(
    title: String? = null, description: String? = null
) : StandardErrorResponse(title, description), PunishmentResponse {
    final override val success = false
}