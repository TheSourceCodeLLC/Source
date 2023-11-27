package net.sourcebot.module.experience.listener

import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent
import net.sourcebot.api.command.GuildCooldown
import net.sourcebot.api.event.EventSubscriber
import net.sourcebot.api.event.EventSystem
import net.sourcebot.api.event.SourceEvent
import net.sourcebot.api.formatPlural
import net.sourcebot.module.experience.Experience
import net.sourcebot.module.profiles.event.ProfileRenderEvent
import java.time.Duration
import java.util.concurrent.ThreadLocalRandom

class ExperienceListener : EventSubscriber<Experience> {
    private val cooldown = GuildCooldown(Duration.ofMinutes(1))

    override fun subscribe(
        module: Experience,
        jdaEvents: EventSystem<GenericEvent>,
        sourceEvents: EventSystem<SourceEvent>
    ) {
        sourceEvents.listen(module, this::onProfileRender)
        jdaEvents.listen(module, this::onRandomXp)
    }

    private fun onProfileRender(event: ProfileRenderEvent) {
        val (embed, member) = event
        val experience = Experience[member]
        val untilNext = Experience.totalPointsFor(experience.level + 1)
        embed.addField(
            "Experience", """
            ${"**Level:** ${experience.level}"}
            ${"**Total:** ${formatPlural(experience.amount, "point")}"}
            ${"**Until Next:** ${formatPlural(untilNext - experience.amount, "point")}"}
        """.trimIndent(), false
        )
    }

    private fun onRandomXp(event: GuildMessageReceivedEvent) {
        val member = event.member ?: return
        if (member.user.isBot) return
        cooldown.test(member,
            {
                val gain = ThreadLocalRandom.current().nextInt(1, 26)
            }, {}
        )
    }
}