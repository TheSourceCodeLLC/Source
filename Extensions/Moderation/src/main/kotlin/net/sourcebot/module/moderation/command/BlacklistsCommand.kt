package net.sourcebot.module.moderation.command

import net.dv8tion.jda.api.entities.Message
import net.sourcebot.api.command.argument.Adapter
import net.sourcebot.api.command.argument.Argument
import net.sourcebot.api.command.argument.ArgumentInfo
import net.sourcebot.api.command.argument.Arguments
import net.sourcebot.api.response.Response

class BlacklistsCommand : ModerationRootCommand(
    "blacklists", "Manage Guild blacklists."
) {
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
            return punishmentHandler.removeBlacklist(message.guild, id - 1)
        }
    }

    private inner class BlacklistsListCommand : ModerationCommand(
        "list", "List the Guild blacklists."
    ) {
        override fun execute(message: Message, args: Arguments): Response {
            return punishmentHandler.listBlacklists(message.guild)
        }
    }

    init {
        addChildren(
            BlacklistsAddCommand(),
            BlacklistsRemoveCommand()
        )
    }
}