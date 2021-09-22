package net.sourcebot.module.moderation.command

import me.hwiggy.kommander.InvalidSyntaxException
import me.hwiggy.kommander.arguments.Adapter
import me.hwiggy.kommander.arguments.Arguments
import me.hwiggy.kommander.arguments.Synopsis
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Message
import net.sourcebot.api.command.argument.SourceAdapter
import net.sourcebot.api.response.Response
import net.sourcebot.module.moderation.Moderation
import java.time.Duration

class TempbanCommand : ModerationRootCommand(
    "tempban", "Temporarily ban a member for a specific reason."
) {
    override val synopsis = Synopsis {
        reqParam("target", "The Member to tempban.", Adapter.single())
        optParam(
            "delDays", "The number of days (0-7) of messages to delete.", Adapter.int(
                0, 7, "Deletion days must be between 0 and 7 days!"
            ), 7
        )
        reqParam("duration", "How long this Member should be tempbanned.", SourceAdapter.duration())
        reqParam("reason", "The reason this Member is being tempbanned.", Adapter.slurp(" "))
    }

    override fun execute(sender: Message, arguments: Arguments.Processed): Response {
        val target = arguments.required<String, Member>("target", "You did not specify a valid member to tempban!") {
            SourceAdapter.member(sender.guild, it)
        }
        val delDays = arguments.optional("delDays", 7)
        val duration = arguments.required<Duration>("duration", "You did not specify a valid duration to tempban for!")
        if (duration.isZero) throw InvalidSyntaxException("The duration may not be zero seconds!")
        val reason = arguments.required<String>("reason", "You did not specify a tempban reason!")
        return Moderation.getPunishmentHandler(sender.guild) {
            tempbanIncident(sender.member!!, target, delDays, duration, reason)
        }
    }
}