package net.sourcebot.module.moderation.command

import me.hwiggy.kommander.arguments.Adapter
import me.hwiggy.kommander.arguments.Arguments
import me.hwiggy.kommander.arguments.Synopsis
import net.dv8tion.jda.api.entities.Message
import net.sourcebot.api.response.Response
import net.sourcebot.api.response.StandardInfoResponse
import net.sourcebot.module.moderation.Moderation
import net.sourcebot.module.moderation.PunishmentHandler
import net.sourcebot.module.moderation.data.Level

class OffensesCommand : ModerationRootCommand(
    "offenses", "Manage Guild offenses."
) {
    override val aliases = listOf("offense")

    private inner class OffensesListCommand : ModerationCommand(
        "list", "List Guild offenses."
    ) {
        override val cleanupResponse = false
        override fun execute(sender: Message, arguments: Arguments.Processed): Response {
            val offenses = Moderation.getPunishmentHandler(sender.guild, PunishmentHandler::getOffenses)
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
        override val synopsis = Synopsis {
            reqParam(
                "level", "The level of the new offense.", Adapter.int(
                    1, 4, "The offense level must be between 1 and 4!"
                )
            )
            reqParam("name", "The name of the new offense.", Adapter.slurp(" "))
        }

        override fun execute(sender: Message, arguments: Arguments.Processed): Response {
            val level = arguments.required<Int>("level", "You did not specify a valid offense level!")
            val name = arguments.required<String>("name", "You did not specify a name for the offense!")
            return Moderation.getPunishmentHandler(sender.guild) { addOffense(level, name) }
        }
    }

    private inner class OffensesRemoveCommand : ModerationCommand(
        "remove", "Remove a Guild offense."
    ) {
        override val synopsis = Synopsis {
            reqParam(
                "id", "The ID of the offense to remove.", Adapter.int(
                    1, error = "The ID must be at least 1!"
                )
            )
        }

        override fun execute(sender: Message, arguments: Arguments.Processed): Response {
            val id = arguments.required<Int>("id", "You did not specify an offense ID to remove!")
            return Moderation.getPunishmentHandler(sender.guild) { removeOffense(id) }
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