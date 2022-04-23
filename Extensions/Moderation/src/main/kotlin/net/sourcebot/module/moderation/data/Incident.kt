package net.sourcebot.module.moderation.data

import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.TextChannel
import net.sourcebot.Source
import net.sourcebot.api.DurationUtils
import net.sourcebot.api.durationOf
import net.sourcebot.api.formatLong
import net.sourcebot.api.response.Response
import net.sourcebot.api.response.StandardInfoResponse
import net.sourcebot.api.round
import net.sourcebot.module.moderation.data.Incident.Type
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
        UNBAN,
        CASE_DELETE
    }
}

abstract class ExecutableIncident : Incident {
    final override val time: Instant = Instant.now()
    abstract fun execute()
    abstract fun sendLog(logChannel: TextChannel): Message
    open fun asDocument() = Document("_id", id).also {
        it["source"] = source
        it["target"] = target
        it["reason"] = reason
        it["type"] = type.name
        it["time"] = time.toEpochMilli()
    }
}

abstract class SimpleIncident(
    duration: Duration
) : ExecutableIncident() {
    final override val expiry: Instant = time.plus(duration)

    override fun asDocument() = super.asDocument().also {
        it["expiry"] = expiry.toEpochMilli()
    }
}

abstract class OneshotIncident : ExecutableIncident() {
    final override val expiry: Instant? = null
}

abstract class OneshotPunishment(
    private val level: Level
) : OneshotIncident() {
    final override fun asDocument() = super.asDocument().also {
        it["points"] = Document().also {
            it["value"] = level.points
            it["decay"] = level.decay.toSeconds()
        }
    }
}

abstract class ExpiringPunishment(
    expiry: Duration,
    private val level: Level
) : SimpleIncident(expiry) {
    final override fun asDocument() = super.asDocument().also {
        it["points"] = Document().also {
            it["value"] = level.points
            it["decay"] = level.decay.toSeconds()
        }
    }
}

@Suppress("NON_EXHAUSTIVE_WHEN") class Case(private val document: Document) : Incident {
    override val id: Long = document["_id"] as Long
    override val source: String = document["source"] as String
    override val target: String = document["target"] as String
    override val reason: String = document["reason"] as String
    override val type: Type = (document["type"] as String).let(Type::valueOf)
    override val time: Instant = (document["time"] as Long).let(Instant::ofEpochMilli)
    override val expiry: Instant? = (document["expiry"] as? Long)?.let(Instant::ofEpochMilli)

    val heading = when (type) {
        Type.CASE_DELETE -> "Case Deletion"
        Type.ROLE_UPDATE -> "Role Update"
        else -> type.name.lowercase().capitalize()
    }
    private val action = when {
        type.name.contains("ban", true) -> "${type.name}ned"
        type.name.contains("mute", true) -> "${type.name}d"
        type == Type.ROLE_UPDATE -> "Updated"
        type == Type.CASE_DELETE -> "Deleted"
        else -> "${type.name}ed"
    }.lowercase().capitalize()

    private val targetType = when (type) {
        Type.CLEAR -> "Channel"
        else -> "User"
    }

    fun render(guild: Guild): Response {
        val sender = guild.getMemberById(source)?.let {
            "${it.formatLong()} (${it.id})"
        } ?: source
        val time = Source.DATE_TIME_FORMAT.format(this.time)
        val header = "$heading - #$id"
        return StandardInfoResponse(header).apply {
            appendDescription("**$action By:** $sender\n")
            if (type != Type.CASE_DELETE) {
                appendDescription("**$action $targetType:** ")
                appendDescription(
                    (if (targetType == "Channel") guild.getTextChannelById(target)?.let {
                        "${it.name} ($target)"
                    }
                    else guild.getMemberById(target)?.let {
                        "${it.formatLong()} ($target)"
                    }) ?: target
                )
                appendDescription("\n")
            }
            when (type) {
                Type.MUTE, Type.TEMPBAN, Type.BLACKLIST -> {
                    val duration = expiry!!.minusSeconds(this@Case.time.epochSecond).let {
                        Duration.ofSeconds(it.epochSecond)
                    }.let(DurationUtils::formatDuration)
                    appendDescription("**Duration:** $duration\n")
                }
                Type.CLEAR -> {
                    appendDescription("**Amount Cleared:** ${document["amount"] as Int}\n")
                }
                Type.ROLE_UPDATE -> {
                    val kind = (document["action"] as String).lowercase().capitalize().let {
                        it + if (it.endsWith("d")) "ed" else "d"
                    }
                    val role = (document["role"] as String).let {
                        val role = guild.getRoleById(it) ?: return@let it
                        "${role.name} ($it)"
                    }
                    appendDescription("**Role $kind**: $role\n")
                }
                else -> {}
            }
            appendDescription("**Reason:** $reason\n")
            appendDescription("**Date & Time:** $time")
        }
    }
}

enum class Level(
    val number: Int,
    points: Double,
    val decay: Duration
) {
    ONE(1, 5.0, durationOf("1M")),
    TWO(2, 10.0, durationOf("2M")),
    THREE(3, 50.0, durationOf("3M")),
    FOUR(4, 100.0, durationOf("1y"));

    val points = points.round(1)
}
