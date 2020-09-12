package net.sourcebot.module.moderation

import net.dv8tion.jda.api.entities.Message
import net.sourcebot.api.command.InvalidSyntaxException
import net.sourcebot.api.command.RootCommand
import net.sourcebot.api.command.argument.*
import net.sourcebot.api.response.ErrorResponse
import net.sourcebot.api.response.Response
import net.sourcebot.api.response.SuccessResponse
import net.sourcebot.module.moderation.data.KickIncident

abstract class ModerationCommand internal constructor(
    final override val name: String,
    final override val description: String
) : RootCommand() {
    final override val permission = "moderation.$name"
    final override val guildOnly = true
}

class KickCommand : ModerationCommand(
    "kick", "Kick a specific member."
) {
    override val argumentInfo = ArgumentInfo(
        Argument("target", "The user to kick."),
        Argument("reason", "Why this user should be kicked.")
    )

    override fun execute(message: Message, args: Arguments): Response {
        val target = args.next(Adapter.member(message.guild), "You did not specify a valid member to kick!")
        val reason = args.slurp(" ", "You did not specify a kick reason!")
        val incident = KickIncident(message.member!!, target, reason)
        if (incident.execute()) {
            val case = incident.sendLog(message.textChannel)
            return SuccessResponse(
                "Kick Success (#$case)",
                "Kicked user ${"%#s".format(target.user)}"
            )
        }
        return ErrorResponse(
            "Kick Failure",
            "I could not kick that member!"
        )
    }
}

class MuteCommand : ModerationCommand(
    "mute", "Mute a specific member."
) {
    override val argumentInfo = ArgumentInfo(
        Argument("target", "The user to be muted."),
        Argument("duration", "The amount of time the user should be muted for."),
        Argument("reason", "Why this user should be muted.")
    )

    override fun execute(message: Message, args: Arguments): Response {
        val target = args.next(Adapter.member(message.guild), "You did not specify a valid member to mute!")
        val duration = args.next(Adapter.duration(), "You did not specify a valid mute duration!")
        if (duration.isEmpty()) throw InvalidSyntaxException("Duration may not be empty!")
        val reason = args.slurp(" ", "You did not specify a mute reason!")
        //TODO: Mute user
        return SuccessResponse(
            "Mute Success",
            "Muted ${String.format("%#s", target.user)} for $duration: $reason"
        )
    }
}

class TempbanCommand : ModerationCommand(
    "tempban", "Temporarily ban a specific member."
) {
    override val argumentInfo = ArgumentInfo(
        Argument("target", "The member to tempban."),
        OptionalArgument("delDays", "The number of days (0-7) of messages to delete.", 7),
        Argument("duration", "How long this tempban should last."),
        Argument("reason", "Why this member should be tempbanned.")
    )

    override fun execute(message: Message, args: Arguments): Response {
        val target = args.next(Adapter.member(message.guild), "You did not specify a valid member to tempban!")
        val delDays = args.next(Adapter.int()) ?: 7
        val duration = args.next(Adapter.duration(), "You did not specify a valid tempban duration!")
        if (duration.isEmpty()) throw InvalidSyntaxException("Duration may not be empty!")
        val reason = args.slurp(" ", "You did not specify a tempban reason!")
        //TODO: Tempban, Case ID
        return SuccessResponse(
            "Tempban Success",
            "Tempbanned ${String.format("%#s", target.user)} for $duration: $reason"
        )
    }
}

class BanCommand : ModerationCommand(
    "ban", "Ban a specific member."
) {
    override val argumentInfo = ArgumentInfo(
        Argument("target", "The member to ban."),
        OptionalArgument("delDays", "The number of days (0-7) of messages to delete", 7),
        Argument("reason", "Why this member should be banned.")
    )

    override fun execute(message: Message, args: Arguments): Response {
        val target = args.next(Adapter.member(message.guild), "You did not specify a valid member to ban!")
        val delDays = args.next(Adapter.int()) ?: 7
        val reason = args.slurp(" ", "You did not specify a ban reason!")
        target.ban(delDays, reason).queue()
        //TODO: Case ID, success
        return SuccessResponse(
            "Ban Success",
            "Banned ${String.format("%#s", target.user)}: $reason"
        )
    }
}

class UnmuteCommand : ModerationCommand(
    "unmute", "Unmute a specific member."
) {
    override val argumentInfo = ArgumentInfo(
        Argument("target", "The member to unmute."),
        Argument("reason", "Why this member should be unmuted.")
    )

    override fun execute(message: Message, args: Arguments): Response {
        val target = args.next(Adapter.member(message.guild), "You did not specify a valid member to unmute!")
        val reason = args.slurp(" ", "You did not specify an unmute reason!")
        //TODO: Unmute
        return SuccessResponse(
            "Unmute Success",
            "Unmuted ${"%#S".format(target.user)}: $reason"
        )
    }
}

class UnbanCommand : ModerationCommand(
    "unban", "Unban a specific user."
) {
    override val argumentInfo = ArgumentInfo(
        Argument("target", "The user ID to unban."),
        Argument("reason", "Why this user should be unbanned.")
    )

    override fun execute(message: Message, args: Arguments): Response {
        val target = args.next("You did not specify a user ID to unban!")
        val reason = args.slurp(" ", "You did not specify an unban reason!")
        val user = try {
            message.guild.retrieveBanById(target).complete()
        } catch (ex: Exception) {
            return ErrorResponse(
                "Unban Failure",
                "I couldn't find a banned user with that ID!"
            )
        }.user
        //TODO: Case ID
        return SuccessResponse(
            "Unban Success",
            "Unbanned ${"%#s".format(user)}: $reason"
        )
    }
}