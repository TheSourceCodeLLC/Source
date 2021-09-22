package net.sourcebot.module.moderation.command

import me.hwiggy.kommander.arguments.Adapter
import me.hwiggy.kommander.arguments.Arguments
import me.hwiggy.kommander.arguments.Synopsis
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Message
import net.sourcebot.api.command.argument.SourceAdapter
import net.sourcebot.api.response.Response
import net.sourcebot.module.moderation.Moderation

class UnmuteCommand : ModerationRootCommand(
    "unmute", "Unmute a member for a specific reason."
) {
    override val synopsis = Synopsis {
        reqParam("target", "The Member to unmute.", Adapter.single())
        reqParam("reason", "The reason this Member is being unmuted.", Adapter.slurp(" "))
    }

    override fun execute(message: Message, args: Arguments.Processed): Response {
        val target = args.required<String, Member>("target", "You did not specify a valid member to unmute!") {
            SourceAdapter.member(message.guild, it)
        }
        val reason = args.required<String>("reason", "You did not specify an unmute reason!")
        return Moderation.getPunishmentHandler(message.guild) {
            unmuteIncident(message.member!!, target, reason)
        }
    }
}