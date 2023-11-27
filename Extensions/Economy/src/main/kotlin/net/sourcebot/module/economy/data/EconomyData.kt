package net.sourcebot.module.economy.data

import net.dv8tion.jda.api.entities.Member
import net.sourcebot.Source
import net.sourcebot.api.configuration.JsonConfiguration
import net.sourcebot.module.economy.events.CoinChangeEvent
import java.time.Instant

class EconomyData(json: JsonConfiguration, private val member: Member) {
    var balance by json.delegateRequired { 0L }
    var daily by json.delegateOptional<DailyRecord>()

    fun addBalance(delta: Long): Pair<Long, List<Pair<Long, String>>> {
        val event = CoinChangeEvent(member, delta)
        Source.SOURCE_EVENTS.fireEvent(event)
        if (event.cancelled) return Pair(0, emptyList())
        val computed = event.getComputedDelta()
        balance += computed
        return computed to event.changelog
    }
}

data class DailyRecord(val count: Long, val expiry: Instant)