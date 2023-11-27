package net.sourcebot.module.moderation.command

import me.hwiggy.kommander.arguments.Adapter
import me.hwiggy.kommander.arguments.Arguments
import me.hwiggy.kommander.arguments.Synopsis
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Message
import net.sourcebot.api.command.argument.SourceAdapter
import net.sourcebot.api.response.Response
import net.sourcebot.module.moderation.Moderation

class PunishCommand : ModerationRootCommand(
    "punish", "Punish a member using a defined offense."
) {
    override val synopsis = Synopsis {
        reqParam("target", "The Member to punish.", SourceAdapter.member())
        reqParam(
            "id", "The ID of the offense to apply.", Adapter.int(
                1, error = "Offense ID must be at least 1!"
            )
        )
    }

    override fun execute(sender: Message, arguments: Arguments.Processed): Response {
        val target = arguments.required<Member>("target", "You did not specify a member to punish!")
        val id = arguments.required<Int>("id", "You did not specify an offense ID!")
        return Moderation.getPunishmentHandler(sender.guild) { punishMember(sender.member!!, target, id) }
    }
}