package net.sourcebot.module.moderation.command

import net.dv8tion.jda.api.entities.Message
import net.sourcebot.api.command.argument.Adapter
import net.sourcebot.api.command.argument.Argument
import net.sourcebot.api.command.argument.ArgumentInfo
import net.sourcebot.api.command.argument.Arguments
import net.sourcebot.api.response.Response
import net.sourcebot.api.response.StandardInfoResponse

class BlacklistCommand : ModerationRootCommand(
    "blacklist", "Manage Guild blacklists."
) {
    override val aliases = arrayOf("bl", "devmute")
    override val argumentInfo = ArgumentInfo(
        Argument("target", "The member to blacklist."),
        Argument("id", "The ID of the blacklist to apply.")
    )

    override fun execute(message: Message, args: Arguments): Response {
        val target = args.next(Adapter.member(message.guild), "You did not specify a valid member to blacklist!")
        val id = args.next(
            Adapter.int(1, error = "Blacklist ID must be at least 1!"),
            "You did not specify a valid blacklist ID to apply!"
        )
        return punishmentHandler.blacklistIncident(message.member!!, target, id)
    }

    private inner class BlacklistsAddCommand : ModerationCommand(
        "add", "Add a Guild blacklist."
    ) {
        override val argumentInfo = ArgumentInfo(
            Argument("duration", "How long the blacklist should last."),
            Argument("reason", "The reason for this blacklist.")
        )

        override fun execute(message: Message, args: Arguments): Response {
            val duration = args.next(
                Adapter.duration("15m", "60m", "Duration must be between 15 and 60 minutes!"),
                "You did not specify a duration for the blacklist!"
            )
            val reason = args.slurp(" ", "You did not specify a reason for the blacklist!")
            return punishmentHandler.addBlacklist(message.guild, duration, reason)
        }
    }

    private inner class BlacklistsRemoveCommand : ModerationCommand(
        "remove", "Remove a Guild blacklist."
    ) {
        override val argumentInfo = ArgumentInfo(
            Argument("id", "The ID of the blacklist to remove.")
        )

        override fun execute(message: Message, args: Arguments): Response {
            val id = args.next(
                Adapter.int(1, error = "The ID must be at least 1!"),
                "You did not specify a blacklist ID to remove!"
            )
            return punishmentHandler.removeBlacklist(message.guild, id)
        }
    }

    private inner class BlacklistsListCommand : ModerationCommand(
        "list", "List the Guild blacklists."
    ) {
        override val cleanupResponse = false
        override fun execute(message: Message, args: Arguments): Response {
            val blacklists = punishmentHandler.getBlacklists(message.guild)
            if (blacklists.isEmpty()) return StandardInfoResponse(
                "No Blacklists!", "There are currently no blacklists!"
            )
            return StandardInfoResponse(
                "Blacklist Listing",
                blacklists.entries.joinToString("\n") { (index, pair) ->
                    val (duration, reason) = pair
                    "**${index + 1}.** `$duration - $reason`"
                }
            )
        }
    }

    init {
        addChildren(
            BlacklistsAddCommand(),
            BlacklistsRemoveCommand(),
            BlacklistsListCommand()
        )
    }
}