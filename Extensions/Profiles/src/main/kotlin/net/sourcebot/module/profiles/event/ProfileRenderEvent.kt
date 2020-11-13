package net.sourcebot.module.profiles.event

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.Member
import net.sourcebot.api.configuration.JsonConfiguration
import net.sourcebot.api.event.SourceEvent

/**
 * Event that is submit when a user profile has been queried for render.
 * Use this event to render custom profile fields from the JSON profile.
 *
 * @author Hunter Wignall
 * @version November 5, 2020
 */
class ProfileRenderEvent(
    val embed: EmbedBuilder,
    val member: Member,
    val profile: JsonConfiguration
) : SourceEvent {
    operator fun component1() = embed
    operator fun component2() = member
    operator fun component3() = profile
}