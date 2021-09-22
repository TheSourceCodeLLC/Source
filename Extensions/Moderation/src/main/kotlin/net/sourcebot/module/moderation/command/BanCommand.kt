package net.sourcebot.module.moderation.command

import me.hwiggy.kommander.arguments.Adapter
import me.hwiggy.kommander.arguments.Arguments
import me.hwiggy.kommander.arguments.Synopsis
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Message
import net.sourcebot.api.command.argument.SourceAdapter
import net.sourcebot.api.response.Response
import net.sourcebot.module.moderation.Moderation

class BanCommand : ModerationRootCommand(
    "ban", "Ban a member for a specific reason."
) {
    override val synopsis = Synopsis {
        reqParam("target", "The Member to ban.", Adapter.single())
        optParam(
            "delDays", "The number of days (0-7) of messages to delete.", Adapter.int(
                0, 7, "Deletion days must be between 0 and 7!"
            )
        )
        reqParam("reason", "The reason this Member is being banned.", Adapter.slurp(" "))
    }

    override fun execute(sender: Message, arguments: Arguments.Processed): Response {
        val target = arguments.required<String, Member>("target", "You did not specify a valid member to ban!") {
            SourceAdapter.member(sender.guild, it)
        }
        val delDays = arguments.optional("delDays", 0)
        val reason = arguments.required<String>("reason", "You did not specify a ban reason!")
        return Moderation.getPunishmentHandler(sender.guild) {
            banIncident(sender.member!!, target, delDays, reason)
        }
    }
}