package net.sourcebot.module.documentation.utility

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.MessageBuilder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.User
import net.sourcebot.api.alert.Alert
import net.sourcebot.api.alert.SourceColor

class DocAlert : EmbedBuilder(), Alert {
    override fun asMessage(user: User): Message {
        this.setColor(SourceColor.INFO.color)
        this.setFooter("Ran By: ${user.asTag}", user.effectiveAvatarUrl)
        return MessageBuilder(super.build()).build()
    }
}