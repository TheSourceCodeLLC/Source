package net.sourcebot.module.economy.listener

import net.dv8tion.jda.api.audit.ActionType
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.guild.member.update.GuildMemberUpdateNicknameEvent
import net.sourcebot.Source
import net.sourcebot.api.asMessage
import net.sourcebot.api.event.EventSubscriber
import net.sourcebot.api.event.EventSystem
import net.sourcebot.api.event.SourceEvent
import net.sourcebot.api.response.StandardErrorResponse
import net.sourcebot.module.economy.Economy
import net.sourcebot.module.economy.data.EconomyData
import net.sourcebot.module.profiles.event.ProfileRenderEvent

class EconomyListener : EventSubscriber<Economy> {
    override fun subscribe(
        module: Economy,
        jdaEvents: EventSystem<GenericEvent>,
        sourceEvents: EventSystem<SourceEvent>
    ) {
        sourceEvents.listen(module, this::onProfileRender)
        jdaEvents.listen(module, this::onNameChange)
    }

    private fun onProfileRender(event: ProfileRenderEvent) {
        val (embed, member) = event
        val economy = EconomyData[member]
        embed.addField(
            "Economy:", """
            **Balance:** ${economy.balance} coins
        """.trimIndent(), false
        )
    }

    private fun onNameChange(event: GuildMemberUpdateNicknameEvent) {
        if (event.newNickname == null) return
        val member = event.member
        val changer = event.guild.retrieveAuditLogs().type(ActionType.MEMBER_UPDATE).complete()[0]
        if (changer.user != member.user) return
        if (Source.PERMISSION_HANDLER.memberHasPermission(member, "economy.ignore-nickname-cost")) return
        val cost = Source.CONFIG_MANAGER[event.guild].required("economy.nickname-cost") { 0L }
        val economy = EconomyData[event.member]
        if (cost > economy.balance) {
            member.modifyNickname(event.oldNickname).queue()
            member.user.openPrivateChannel().queue {
                it.sendMessage(
                    StandardErrorResponse(
                        "Invalid Balance!", "You cannot afford a nickname change! (Cost: $cost coins)"
                    ).asMessage(member)
                )
            }
            return
        }
        economy.balance -= cost
    }
}