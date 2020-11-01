package net.sourcebot.module.moderation.data

import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.TextChannel
import net.sourcebot.Source
import net.sourcebot.api.DurationUtils
import net.sourcebot.api.formatted
import net.sourcebot.api.response.Response
import net.sourcebot.api.response.StandardInfoResponse
import org.bson.Document
import java.time.Duration
import java.time.Instant

interface Incident {
    val id: Long
    val source: String
    val target: String
    val reason: String
    val type: Type
    val time: Instant
    val expiry: Instant?

    enum class Type {
        ROLE_UPDATE,
        CLEAR,
        WARN,
        KICK,
        BLACKLIST,
        MUTE,
        TEMPBAN,
        BAN,
        UNBLACKLIST,
        UNMUTE,
        UNBAN
    }
}

abstract class ExecutableIncident : Incident {
    final override val time: Instant = Instant.now()
    abstract fun asDocument(): Document
    abstract fun execute()
    abstract fun sendLog(logChannel: TextChannel)
}

abstract class SimpleIncident(
    duration: Duration
) : ExecutableIncident() {
    final override val expiry: Instant = time.plus(duration)

    override fun asDocument() = Document("_id", id).apply {
        this["source"] = source
        this["target"] = target
        this["reason"] = reason
        this["type"] = type.name
        this["time"] = time.toEpochMilli()
        this["expiry"] = expiry.toEpochMilli()
    }
}

abstract class OneshotIncident : ExecutableIncident() {
    final override val expiry: Instant? = null

    override fun asDocument() = Document("_id", id).apply {
        this["source"] = source
        this["target"] = target
        this["reason"] = reason
        this["type"] = type.name
        this["time"] = time.toEpochMilli()
    }
}

@Suppress("NON_EXHAUSTIVE_WHEN") class Case(private val document: Document) : Incident {
    override val id: Long = document["_id"] as Long
    override val source: String = document["source"] as String
    override val target: String = document["target"] as String
    override val reason: String = document["reason"] as String
    override val type: Incident.Type = (document["type"] as String).let(Incident.Type::valueOf)
    override val time: Instant = (document["time"] as Long).let(Instant::ofEpochMilli)
    override val expiry: Instant? = (document["expiry"] as? Long)?.let(Instant::ofEpochMilli)

    private val heading = when (type) {
        Incident.Type.ROLE_UPDATE -> "Role Update"
        else -> type.name.toLowerCase().capitalize()
    }
    private val action = when {
        type.name.contains("ban", true) -> "${type.name}ned"
        type.name.contains("mute", true) -> "${type.name}d"
        type == Incident.Type.ROLE_UPDATE -> "Updated"
        else -> "${type.name}ed"
    }.toLowerCase().capitalize()

    private val targetType = when (type) {
        Incident.Type.CLEAR -> "Channel"
        else -> "User"
    }

    fun render(guild: Guild): Response {
        val sender = guild.getMemberById(source)?.let {
            "${it.formatted()} (${it.id})"
        } ?: source
        val time = Source.DATE_TIME_FORMAT.format(this.time)
        val header = "$heading - #$id"
        return StandardInfoResponse(header).apply {
            appendDescription("**$action By:** $sender\n")
            appendDescription("**$action $targetType:** ")
            appendDescription(
                (if (targetType == "Channel") guild.getTextChannelById(target)?.let {
                    "${it.name} ($target)"
                }
                else guild.getMemberById(target)?.let {
                    "${it.formatted()} ($target)"
                }) ?: target
            )
            appendDescription("\n")
            when (type) {
                Incident.Type.MUTE, Incident.Type.TEMPBAN -> {
                    val duration = expiry!!.minusSeconds(this@Case.time.epochSecond).let {
                        Duration.ofSeconds(it.epochSecond)
                    }.let(DurationUtils::formatDuration)
                    appendDescription("**Duration:** $duration\n")
                }
                Incident.Type.CLEAR -> {
                    appendDescription("**Amount Cleared:** ${document["amount"] as Int}\n")
                }
                Incident.Type.ROLE_UPDATE -> {
                    val kind = (document["action"] as String).toLowerCase().capitalize().let {
                        it + if (it.endsWith("d")) "ed" else "d"
                    }
                    val role = (document["role"] as String).let {
                        val role = guild.getRoleById(it) ?: return@let it
                        "${role.name} ($it)"
                    }
                    appendDescription("**Role $kind**: $role\n")
                }
            }
            appendDescription("**Reason:** $reason\n")
            appendDescription("**Date & Time:** $time")
        }
    }
}
