package net.sourcebot.api.command

import net.dv8tion.jda.api.entities.Member
import net.sourcebot.api.formatted
import java.time.Duration
import java.time.Instant

class GuildCooldown {
    private val handler = HashMap<String, MutableMap<String, Instant>>()

    fun <T> test(
        member: Member,
        onSuccess: () -> T,
        onFailure: (String) -> T
    ): T {
        val inner = handler.computeIfAbsent(member.guild.id) { HashMap() }
        val now = Instant.now()
        var returnVal: T? = null
        inner.compute(member.id) { _, v ->
            if (v == null || v.isBefore(now)) {
                returnVal = onSuccess()
                now.plusSeconds(5)
            } else {
                returnVal = onFailure(Duration.between(now, v).formatted())
                v
            }
        }
        return returnVal!!
    }
}