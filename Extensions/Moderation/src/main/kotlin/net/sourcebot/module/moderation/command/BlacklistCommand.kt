package net.sourcebot.module.moderation.command

import me.hwiggy.kommander.arguments.Adapter
import me.hwiggy.kommander.arguments.Arguments
import me.hwiggy.kommander.arguments.Synopsis
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Message
import net.sourcebot.api.command.argument.SourceAdapter
import net.sourcebot.api.response.Response
import net.sourcebot.api.response.StandardInfoResponse
import net.sourcebot.module.moderation.Moderation
import net.sourcebot.module.moderation.PunishmentHandler
import java.time.Duration

class BlacklistCommand : ModerationRootCommand(
    "blacklist", "Manage Guild blacklists."
) {
    override val aliases = listOf("bl", "devmute")
    override val synopsis = Synopsis {
        reqParam("target", "The Member to blacklist.", SourceAdapter.member())
        reqParam("id", "The ID of the blacklist to apply.", Adapter.int(1, error = "Blacklist ID must be at least 1!"))
    }

    override fun execute(sender: Message, arguments: Arguments.Processed): Response {
        val target = arguments.required<Member>("target", "You did not specify a valid member to blacklist!")
        val id = arguments.required<Int>("id", "You did not specify a valid blacklist ID to apply!")
        return Moderation.getPunishmentHandler(sender.guild) {
            blacklistIncident(sender.member!!, target, id)
        }
    }

    private inner class BlacklistsAddCommand : ModerationCommand(
        "add", "Add a Guild blacklist."
    ) {
        override val synopsis = Synopsis {
            reqParam(
                "duration", "How long the blacklist should last.",
                SourceAdapter.duration("15m", "60m", "Duration must be between 15 and 60 minutes!")
            )
            reqParam("reason", "The reason for this blacklist.", Adapter.slurp(" "))
        }

        override fun execute(sender: Message, arguments: Arguments.Processed): Response {
            val duration = arguments.required<Duration>("duration", "You did not specify a duration for the blacklist!")
            val reason = arguments.required<String>("reason", "You did not specify a reason for the blacklist!")
            return Moderation.getPunishmentHandler(sender.guild) { addBlacklist(duration, reason) }
        }
    }

    private inner class BlacklistsRemoveCommand : ModerationCommand(
        "remove", "Remove a Guild blacklist."
    ) {
        override val synopsis = Synopsis {
            reqParam("id", "The ID of the blacklist to remove", Adapter.int(1, error = "The ID must be at least 1!"))
        }

        override fun execute(sender: Message, arguments: Arguments.Processed): Response {
            val id = arguments.required<Int>("id", "You did not specify a blacklist ID to remove!")
            return Moderation.getPunishmentHandler(sender.guild) { removeBlacklist(id) }
        }
    }

    private inner class BlacklistsListCommand : ModerationCommand(
        "list", "List the Guild blacklists."
    ) {
        override val cleanupResponse = false
        override fun execute(sender: Message, arguments: Arguments.Processed): Response {
            val blacklists = Moderation.getPunishmentHandler(sender.guild, PunishmentHandler::getBlacklists)
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