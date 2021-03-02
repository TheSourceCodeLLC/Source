package net.sourcebot.api.command

import net.dv8tion.jda.api.entities.Member
import net.sourcebot.api.formatLong
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit

class GuildCooldown(private val duration: Duration) {
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
                now.plus(duration)
            } else {
                val difference = Duration.between(now, v).truncatedTo(ChronoUnit.SECONDS)
                returnVal = when {
                    difference.isZero || difference.isNegative -> onSuccess()
                    else -> onFailure(difference.formatLong())
                }
                v
            }
        }
        return returnVal!!
    }
}