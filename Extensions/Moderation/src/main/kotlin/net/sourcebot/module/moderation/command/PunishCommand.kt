package net.sourcebot.module.moderation.command

import net.dv8tion.jda.api.entities.Message
import net.sourcebot.api.command.argument.Adapter
import net.sourcebot.api.command.argument.Argument
import net.sourcebot.api.command.argument.ArgumentInfo
import net.sourcebot.api.command.argument.Arguments
import net.sourcebot.api.response.Response
import net.sourcebot.module.moderation.Moderation

class PunishCommand : ModerationRootCommand(
    "punish", "Punish a member using a defined offense."
) {
    override val argumentInfo = ArgumentInfo(
        Argument("target", "The member to punish"),
        Argument("id", "The ID of the offense to apply")
    )

    override fun execute(message: Message, args: Arguments): Response {
        val target = args.next(Adapter.member(message.guild), "You did not specify a member to punish!")
        val id = args.next(
            Adapter.int(1, error = "Offense ID must be at least 1!"),
            "You did not specify an offense ID!"
        )
        return Moderation.getPunishmentHandler(message.guild) { punishMember(message.member!!, target, id) }
    }
}