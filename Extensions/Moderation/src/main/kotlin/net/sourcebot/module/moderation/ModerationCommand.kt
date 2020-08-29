package net.sourcebot.module.moderation

import net.dv8tion.jda.api.entities.Message
import net.sourcebot.api.alert.Alert
import net.sourcebot.api.alert.ErrorAlert
import net.sourcebot.api.alert.SuccessAlert
import net.sourcebot.api.command.RootCommand
import net.sourcebot.api.command.argument.*

abstract class ModerationCommand internal constructor(
    final override val name: String,
    final override val description: String
) : RootCommand() {
    final override val permission = "moderation.$name"
    final override val guildOnly = true
}

class BanCommand : ModerationCommand(
    "ban", "Ban a specific user."
) {
    override val argumentInfo = ArgumentInfo(
        Argument("target", "The user to ban."),
        OptionalArgument("delDays", "The number of days (0-7) of messages to delete", 7),
        Argument("reason", "Why this user should be banned.")
    )

    override fun execute(message: Message, args: Arguments): Alert {
        val target = args.next(Adapter.member(message.guild), "You did not specify a valid user to ban!")
        val delDays = args.next(Adapter.int()) ?: 7
        val reason = args.slurp(" ", "You did not specify a ban reason!")
        target.ban(delDays, reason).queue()
        //TODO: Case ID, success
        return SuccessAlert(
            "Ban Success",
            "Banned ${String.format("%#s", target.user)}: $reason"
        )
    }
}

class KickCommand : ModerationCommand(
    "kick", "Kick a specific user."
) {
    override val argumentInfo = ArgumentInfo(
        Argument("target", "The user to kick."),
        Argument("reason", "Why this user should be kicked.")
    )

    override fun execute(message: Message, args: Arguments): Alert {
        val target = args.next(Adapter.member(message.guild), "You did not specify a valid member to kick!")
        val reason = args.slurp(" ", "You did not specify a kick reason!")
        target.kick(reason).queue()
        // TODO: Form Case ID & return alert
        return SuccessAlert(
            "Kick Success",
            "Kicked ${String.format("%#s", target.user)}: $reason"
        )
    }
}

class MuteCommand : ModerationCommand(
    "mute", "Mute a specific user."
) {
    override val argumentInfo = ArgumentInfo(
        Argument("target", "The user to be muted."),
        Argument("duration", "The amount of time the user should be muted for."),
        Argument("reason", "Why this user should be muted.")
    )

    override fun execute(message: Message, args: Arguments): Alert {
        val target = args.next(Adapter.member(message.guild), "You did not specify a valid user to mute!")
        val duration = args.next("You did not specify a valid mute duration!")
        // Try to parse duration string
        val reason = args.slurp(" ", "You did not specify a mute reason!")
        //TODO: Mute user
        return SuccessAlert(
            "Mute Success",
            "Muted ${String.format("%#s", target.user)} for $duration: $reason"
        )
    }
}

class TempbanCommand : ModerationCommand(
    "tempban", "Temporarily ban a user."
) {
    override val argumentInfo = ArgumentInfo(
        Argument("target", "The user to tempban."),
        OptionalArgument("delDays", "The number of days (0-7) of messages to delete.", 7),
        Argument("duration", "How long this tempban should last."),
        Argument("reason", "Why this user should be tempbanned.")
    )

    override fun execute(message: Message, args: Arguments): Alert {
        val target = args.next(Adapter.member(message.guild), "You did not specify a valid user to tempban!")
        val delDays = args.next(Adapter.int()) ?: 7
        val duration = args.next("You did not specify a valid tempban duration!")
        val reason = args.slurp(" ", "You did not specify a tempban reason!")
        //TODO: Tempban, Case ID
        return SuccessAlert(
            "Tempban Success",
            "Tempbanned ${String.format("%#s", target.user)} for $duration: $reason"
        )
    }
}

class UnbanCommand : ModerationCommand(
    "unban", "Unban a user."
) {
    override val argumentInfo = ArgumentInfo(
        Argument("target", "The user ID to unban."),
        Argument("reason", "Why this user should be unbanned.")
    )

    override fun execute(message: Message, args: Arguments): Alert {
        val target = args.next("You did not specify a user ID to unban!")
        val reason = args.slurp(" ", "You did not specify an unban reason!")
        val user = try {
            message.guild.retrieveBanById(target).complete()
        } catch (ex: Exception) {
            return ErrorAlert(
                "Unban Failure",
                "I couldn't find a banned user with that ID!"
            )
        }.user
        //TODO: Case ID
        return SuccessAlert(
            "Unban Success",
            "Unbanned ${"%#s".format(user)}: $reason"
        )
    }
}