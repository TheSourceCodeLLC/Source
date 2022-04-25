package net.sourcebot.module.economy.listener

import net.dv8tion.jda.api.audit.ActionType
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.guild.member.update.GuildMemberUpdateNicknameEvent
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent
import net.sourcebot.Source
import net.sourcebot.api.asMessage
import net.sourcebot.api.configuration.required
import net.sourcebot.api.event.EventSubscriber
import net.sourcebot.api.event.EventSystem
import net.sourcebot.api.event.SourceEvent
import net.sourcebot.api.formatPlural
import net.sourcebot.api.response.StandardErrorResponse
import net.sourcebot.module.economy.Economy
import net.sourcebot.module.profiles.event.ProfileRenderEvent

class EconomyListener : EventSubscriber<Economy> {
    override fun subscribe(
        module: Economy,
        jdaEvents: EventSystem<GenericEvent>,
        sourceEvents: EventSystem<SourceEvent>
    ) {
        sourceEvents.listen(module, this::onProfileRender)
        jdaEvents.listen(module, this::onNameChange)
        jdaEvents.listen(module, this::randomCoins)
    }

    private fun onProfileRender(event: ProfileRenderEvent) {
        val (embed, member) = event
        val economy = Economy[member]
        embed.addField(
            "Economy:", """
            **Balance:** ${formatPlural(economy.balance, "coin")}
            ${economy.daily?.let { "**Daily Streak:** ${formatPlural(it.count, "day")}" } ?: ""}
        """.trimIndent(), false
        )
    }

    private val forbiddenNames = arrayOf(
        "everyone", "here"
    )

    private fun onNameChange(event: GuildMemberUpdateNicknameEvent) {
        if (event.newNickname == null) return
        val member = event.member
        if (event.newNickname in forbiddenNames) return refuseNick(
            member,
            event.oldNickname,
            "Forbidden Nickname!",
            "You may not nick yourself '${event.newNickname}!'"
        )
        val changer = event.guild.retrieveAuditLogs().type(ActionType.MEMBER_UPDATE).complete()[0]
        if (changer.user != member.user) return
        if (Source.PERMISSION_HANDLER.memberHasPermission(member, "economy.ignore-nickname-cost")) return
        val cost = Source.CONFIG_MANAGER[event.guild].required("economy.nickname-cost") { 0L }
        val economy = Economy[event.member]
        if (cost > economy.balance) return refuseNick(
            member,
            event.oldNickname,
            "Invalid Balance!",
            "You cannot afford a nickname change! (Cost: $cost coins)"
        )
        economy.addBalance(-cost)
    }

    private fun refuseNick(member: Member, old: String?, title: String, desc: String) {
        member.modifyNickname(old).queue()
        member.user.openPrivateChannel().queue {
            it.sendMessage(StandardErrorResponse(title, desc).asMessage(member)).queue()
        }
    }

    private fun randomCoins(event: GuildMessageReceivedEvent) {

    }
}