package net.sourcebot.module.moderation.command

import me.hwiggy.kommander.arguments.Adapter
import me.hwiggy.kommander.arguments.Arguments
import me.hwiggy.kommander.arguments.Synopsis
import net.dv8tion.jda.api.entities.Message
import net.sourcebot.api.response.Response
import net.sourcebot.module.moderation.Moderation

class UnbanCommand : ModerationRootCommand(
    "unban", "Unban a user for a specific reason."
) {
    override val synopsis = Synopsis {
        reqParam("target", "The ID of the User to unban.", Adapter.single())
        reqParam("reason", "The reason this user is being unbanned.", Adapter.slurp(" "))
    }

    override fun execute(sender: Message, arguments: Arguments.Processed): Response {
        val target = arguments.required<String>("target", "You did not specify a user ID to unban!")
        val reason = arguments.required<String>("reason", "You did not specify an unban reason!")
        return Moderation.getPunishmentHandler(sender.guild) {
            unbanIncident(sender.member!!, target, reason)
        }
    }
}