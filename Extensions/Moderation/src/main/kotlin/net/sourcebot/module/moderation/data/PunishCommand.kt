package net.sourcebot.module.moderation.data

import net.dv8tion.jda.api.entities.Message
import net.sourcebot.api.command.argument.Adapter
import net.sourcebot.api.command.argument.Argument
import net.sourcebot.api.command.argument.ArgumentInfo
import net.sourcebot.api.command.argument.Arguments
import net.sourcebot.api.response.Response
import net.sourcebot.module.moderation.command.ModerationCommand

class PunishCommand : ModerationCommand(
    "punish", "Punish a member using a preset punishment."
) {
    override val argumentInfo = ArgumentInfo(
        Argument("target", "The member to punish"),
        Argument("id", "The ID of the punishment to apply!")
    )

    override fun execute(message: Message, args: Arguments): Response {
        val target = args.next(Adapter.member(message.guild), "You did not specify a member to punish!")
        val id = args.next(Adapter.int(), "You did not specify a punishment to apply!")
        return punishmentHandler.punishMember(message.member!!, target, id)
    }
}