package net.sourcebot.module.moderation.data

import net.dv8tion.jda.api.entities.TextChannel
import org.bson.Document
import java.time.Instant

interface Incident {
    val id: Long
    val source: String
    val target: String
    val reason: String
    val type: Type
    val time: Instant
    val expiry: Instant?

    enum class Type { CLEAR, WARN, KICK, MUTE, TEMPBAN, BAN, UNMUTE, UNBAN }

    fun asDocument(): Document
    fun execute()
    fun sendLog(logChannel: TextChannel)
}

abstract class SimpleIncident(
    final override val expiry: Instant
) : Incident {
    final override val time: Instant = Instant.now()

    override fun asDocument() = Document("id", id).apply {
        this["source"] = source
        this["target"] = target
        this["reason"] = reason
        this["type"] = type.name
        this["time"] = time.toEpochMilli()
        this["expiry"] = expiry.toEpochMilli()
    }
}

abstract class OneshotIncident : Incident {
    final override val expiry: Instant? = null
    final override val time: Instant = Instant.now()

    override fun asDocument() = Document("id", id).apply {
        this["source"] = source
        this["target"] = target
        this["reason"] = reason
        this["type"] = type.name
        this["time"] = time.toEpochMilli()
    }
}
