package net.sourcebot.module.trivia.command

import net.dv8tion.jda.api.entities.Message
import net.sourcebot.api.command.Command
import net.sourcebot.api.command.InvalidSyntaxException
import net.sourcebot.api.command.RootCommand
import net.sourcebot.api.command.argument.Adapter
import net.sourcebot.api.command.argument.ArgumentInfo
import net.sourcebot.api.command.argument.Arguments
import net.sourcebot.api.command.argument.OptionalArgument
import net.sourcebot.api.response.*
import net.sourcebot.api.urlDecoded
import net.sourcebot.module.trivia.data.Game
import net.sourcebot.module.trivia.data.OpenTDB
import java.util.*

class TriviaCommand : RootCommand() {
    override val name: String = "trivia"
    override val description: String = "Manage active trivia game, or start a new one."
    override val guildOnly: Boolean = true
    override val aliases: Array<String> = arrayOf("triv")

    private val activeGames = HashMap<String, Game>()

    init {
        addChildren(
            TriviaStartCommand(),
            TriviaStopCommand(),
            TriviaCategoriesCommand()
        )
    }

    private inner class TriviaStartCommand : Bootstrap(
        "start", "Starts a game of trivia"
    ) {
        override var cleanupResponse = false
        override val argumentInfo: ArgumentInfo = ArgumentInfo(
            OptionalArgument("amount", "Amount of questions to ask. 1-50", 5),
            OptionalArgument("category", "The category to pull questions from.")
        )

        override fun execute(message: Message, args: Arguments): Response {
            val amount = args.next(Adapter.int()) ?: 5
            if (amount < 1 || amount > 50) throw InvalidSyntaxException(
                "Amount of questions must be between 1 and 50!"
            )
            val category = args.next(Adapter.int())
            if (category != null && !OpenTDB.isValidCategory(category)) {
                return ErrorResponse(
                    "Unknown Trivia Category ID '$category'",
                    "Available Categories:\n" + OpenTDB.categories.joinToString("\n") { "**${it.id}**: ${it.name.urlDecoded()}" }
                )
            }
            val activeGame = activeGames[message.guild.id]
            if (activeGame != null) return WarningResponse(
                "Trivia In Progress!",
                "There is already an active game! [[Jump](${activeGame.getJumpUrl()})]"
            )
            val game = Game(amount, category)
            activeGames[message.guild.id] = game
            return game.start { activeGames.remove(message.guild.id) }
        }

        override fun postResponse(response: Response, message: Message) {
            if (response !is Game.TriviaStartResponse) return
            activeGames[message.guild.id]?.setMessage(message)
        }
    }

    private inner class TriviaStopCommand : Bootstrap(
        "stop", "Stop the active Trivia game."
    ) {
        override fun execute(message: Message, args: Arguments): Response {
            val activeGame = activeGames.remove(message.guild.id) ?: return ErrorResponse(
                "Trivia Stop", "There is no active Trivia game!"
            )
            activeGame.stop()
            //The response will be available as an edit to the original message
            return EmptyResponse()
        }
    }

    private class TriviaCategoriesCommand : Bootstrap(
        "categories", "List the available Trivia Categories."
    ) {
        override var cleanupResponse: Boolean = false
        override fun execute(
            message: Message,
            args: Arguments
        ) = InfoResponse(
            "Trivia Categories",
            OpenTDB.categories.joinToString("\n") { "**${it.id}**: ${it.name.urlDecoded()}" }
        )
    }
}

abstract class Bootstrap(
    final override val name: String,
    final override val description: String
) : Command() {
    final override val guildOnly = true
    final override val permission by lazy { "trivia.$name" }
}