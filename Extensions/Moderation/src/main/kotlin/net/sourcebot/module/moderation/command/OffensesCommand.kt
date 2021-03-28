package net.sourcebot.module.moderation.command

import net.dv8tion.jda.api.entities.Message
import net.sourcebot.api.command.argument.Adapter
import net.sourcebot.api.command.argument.Argument
import net.sourcebot.api.command.argument.ArgumentInfo
import net.sourcebot.api.command.argument.Arguments
import net.sourcebot.api.response.Response
import net.sourcebot.api.response.StandardInfoResponse
import net.sourcebot.module.moderation.Moderation
import net.sourcebot.module.moderation.PunishmentHandler
import net.sourcebot.module.moderation.data.Level

class OffensesCommand : ModerationRootCommand(
    "offenses", "Manage Guild offenses."
) {
    override val aliases = arrayOf("offense")

    private inner class OffensesListCommand : ModerationCommand(
        "list", "List Guild offenses."
    ) {
        override val cleanupResponse = false
        override fun execute(message: Message, args: Arguments): Response {
            val offenses = Moderation.getPunishmentHandler(message.guild, PunishmentHandler::getOffenses)
            if (offenses.isEmpty()) return StandardInfoResponse(
                "No Offenses!", "There are currently no offenses!"
            )
            val listing = offenses.entries.groupBy({ (_, document) ->
                Level.values()[document["level"] as Int - 1]
            }, { (index, document) ->
                index + 1 to (document["name"] as String)
            })
            return StandardInfoResponse("Offense List").also {
                listing.forEach { (level, offenses) ->
                    it.addField(
                        "Level ${level.number} - ${level.points} Points",
                        offenses.joinToString(separator = "\n") { (id, name) ->
                            "**$id** `$name`"
                        }, false
                    )
                }
            }
        }
    }

    private inner class OffensesAddCommand : ModerationCommand(
        "add", "Add a Guild offense."
    ) {
        override val argumentInfo = ArgumentInfo(
            Argument("level", "The level of the new offense (1-4)."),
            Argument("name", "The name of the new offense.")
        )

        override fun execute(message: Message, args: Arguments): Response {
            val level = args.next(
                Adapter.int(1, 4, "The offense level must be between 1 and 4!"),
                "You did not specify a valid offense level!"
            )
            val name = args.slurp(" ", "You did not specify a name for the offense!")
            return Moderation.getPunishmentHandler(message.guild) { addOffense(level, name) }
        }
    }

    private inner class OffensesRemoveCommand : ModerationCommand(
        "remove", "Remove a Guild offense."
    ) {
        override val argumentInfo = ArgumentInfo(
            Argument("id", "The ID of the offense to remove.")
        )

        override fun execute(message: Message, args: Arguments): Response {
            val id = args.next(
                Adapter.int(1, error = "The ID must be at least 1!"),
                "You did not specify an offense ID to remove!"
            )
            return Moderation.getPunishmentHandler(message.guild) { removeOffense(id) }
        }
    }

    init {
        addChildren(
            OffensesListCommand(),
            OffensesAddCommand(),
            OffensesRemoveCommand()
        )
    }
}