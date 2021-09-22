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

class MuteCommand : ModerationRootCommand(
    "mute", "Temporarily mute a member for a specific reason."
) {
    override val synopsis = Synopsis {
        reqParam("target", "The Member to mute.", Adapter.single())
        reqParam("duration", "How long this Member should be muted for.", SourceAdapter.duration())
        reqParam("reason", "The reason this Member is being muted.", Adapter.slurp(" "))
    }

    override fun execute(message: Message, args: Arguments.Processed): Response {
        val target = args.required<String, Member>("target", "You did not specify a valid member to mute!") {
            SourceAdapter.member(message.guild, it)
        }
        val duration = args.required<Duration>("duration", "You did not specify a valid duration to mute for!")
        if (duration.isZero) throw InvalidSyntaxException("The duration may not be zero seconds!")
        val reason = args.required<String>("reason", "You did not specify a mute reason!")
        return Moderation.getPunishmentHandler(message.guild) {
            muteIncident(message.member!!, target, duration, reason)
        }
    }
}