package net.sourcebot.module.trivia.command

import me.hwiggy.kommander.arguments.Adapter
import me.hwiggy.kommander.arguments.Arguments
import me.hwiggy.kommander.arguments.Synopsis
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.User
import net.sourcebot.api.command.RootCommand
import net.sourcebot.api.command.SourceCommand
import net.sourcebot.api.response.*
import net.sourcebot.api.urlDecoded
import net.sourcebot.module.trivia.data.Game
import net.sourcebot.module.trivia.data.OpenTDB

class TriviaCommand : RootCommand() {
    override val name: String = "trivia"
    override val description: String = "Manage active trivia game, or start a new one."
    override val guildOnly: Boolean = true
    override val aliases = listOf("triv")
    override val permission = name

    private val activeGames = HashMap<String, Game>()

    init {
        register(
            TriviaStartCommand(),
            TriviaStopCommand(),
            TriviaCategoriesCommand()
        )
    }

    private inner class TriviaStartCommand : Bootstrap(
        "start", "Starts a game of trivia"
    ) {
        override var cleanupResponse = false
        override val synopsis = Synopsis {
            optParam(
                "amount", "Amount of questions to ask. 1-50", Adapter.int(
                    1, 50, "Amount of questions must be between 1 and 50!"
                ), 5
            )
            optParam("category", "The category number to pull questions from.", Adapter.int())
        }

        override fun execute(sender: Message, arguments: Arguments.Processed): Response {
            val amount = arguments.optional("amount", 5)
            val category = arguments.optional<Int>("category")
            if (category != null && !OpenTDB.isValidCategory(category)) {
                return StandardErrorResponse(
                    "Unknown Trivia Category ID '$category'",
                    "Available Categories:\n" + OpenTDB.categories.joinToString("\n") { "**${it.id}**: ${it.name.urlDecoded()}" }
                )
            }
            val activeGame = activeGames[sender.guild.id]
            if (activeGame != null) return StandardWarningResponse(
                "Trivia In Progress!",
                "There is already an active game in ${activeGame.getChannel()}!"
            )
            val game = Game(amount, category)
            activeGames[sender.guild.id] = game
            return game.start { activeGames.remove(sender.guild.id) }
        }

        override fun postResponse(response: Response, forWhom: User, message: Message) {
            if (response !is Game.TriviaStartResponse) return
            activeGames[message.guild.id]?.setMessage(message)
        }
    }

    private inner class TriviaStopCommand : Bootstrap(
        "stop", "Stop the active Trivia game."
    ) {
        override fun execute(sender: Message, arguments: Arguments.Processed): Response {
            val activeGame = activeGames.remove(sender.guild.id) ?: return StandardErrorResponse(
                "Trivia Stop", "There is no active Trivia game!"
            )
            activeGame.stop(Game.StopCause.ABORTED)
            //The response will be available as an edit to the original message
            return EmptyResponse()
        }
    }

    private class TriviaCategoriesCommand : Bootstrap(
        "categories", "List the available Trivia Categories."
    ) {
        override var cleanupResponse: Boolean = false
        override fun execute(
            sender: Message,
            arguments: Arguments.Processed
        ) = StandardInfoResponse(
            "Trivia Categories",
            OpenTDB.categories.joinToString("\n") { "**${it.id}**: ${it.name.urlDecoded()}" }
        )
    }
}

abstract class Bootstrap(
    final override val name: String,
    final override val description: String
) : SourceCommand() {
    final override val guildOnly = true
    final override val permission = "trivia.$name"
}