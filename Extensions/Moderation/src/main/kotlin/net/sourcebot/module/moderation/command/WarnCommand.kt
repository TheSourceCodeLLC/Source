package net.sourcebot.module.moderation.command

import me.hwiggy.kommander.arguments.Adapter
import me.hwiggy.kommander.arguments.Arguments
import me.hwiggy.kommander.arguments.Synopsis
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Message
import net.sourcebot.api.command.argument.SourceAdapter
import net.sourcebot.api.response.Response
import net.sourcebot.module.moderation.Moderation

class WarnCommand : ModerationRootCommand(
    "warn", "Warn a member for a specific reason."
) {
    override val synopsis = Synopsis {
        reqParam("target", "The Member to warn.", SourceAdapter.member())
        reqParam("reason", "The reason this Member is being warned.", Adapter.slurp(" "))
    }

    override fun execute(sender: Message, arguments: Arguments.Processed): Response {
        val target = arguments.required<Member>("target", "You did not specify a valid member to warn!")
        val reason = arguments.required<String>("reason", "You did not specify a warning reason!")
        return Moderation.getPunishmentHandler(sender.guild) {
            warnIncident(sender.member!!, target, reason)
        }
    }
}