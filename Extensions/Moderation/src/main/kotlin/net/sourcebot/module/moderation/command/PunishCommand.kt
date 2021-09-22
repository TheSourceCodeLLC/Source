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
        reqParam("target", "The Member to punish.", Adapter.single())
        reqParam(
            "id", "The ID of the offense to apply.", Adapter.int(
                1, error = "Offense ID must be at least 1!"
            )
        )
    }

    override fun execute(message: Message, args: Arguments.Processed): Response {
        val target = args.required<String, Member>("target", "You did not specify a member to punish!") {
            SourceAdapter.member(message.guild, it)
        }
        val id = args.required<Int>("id", "You did not specify an offense ID!")
        return Moderation.getPunishmentHandler(message.guild) { punishMember(message.member!!, target, id) }
    }
}