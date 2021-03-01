package net.sourcebot.module.economy.events

import net.dv8tion.jda.api.entities.Member
import net.sourcebot.api.event.Cancellable
import net.sourcebot.api.event.SourceEvent

class CoinChangeEvent(
    val member: Member, val delta: Long
) : SourceEvent, Cancellable {
    val changelog = ArrayList<Pair<Long, String>>()
    override var cancelled = false

    operator fun component1() = member
    operator fun component2() = delta

    fun getComputedDelta() = delta + changelog.sumOf { it.first }
}