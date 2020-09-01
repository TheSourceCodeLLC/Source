package net.sourcebot.module.trivia.command

import net.dv8tion.jda.api.entities.Message
import net.sourcebot.api.command.Command
import net.sourcebot.api.command.RootCommand
import net.sourcebot.api.command.argument.Argument
import net.sourcebot.api.command.argument.ArgumentInfo
import net.sourcebot.api.command.argument.Arguments
import net.sourcebot.api.response.ErrorResponse
import net.sourcebot.api.response.Response
import net.sourcebot.api.response.SuccessResponse
import net.sourcebot.module.trivia.data.TriviaGameManager

class TriviaCommand(val gameManager: TriviaGameManager) : RootCommand() {
    override val name: String = "trivia"
    override val description: String = "Manage active trivia game, or start a new one."
    override val guildOnly: Boolean = true
    override val aliases: Array<String> = arrayOf("triv")


    init {
        addChildren(
            TriviaStartCommand(),
        )
    }

    private inner class TriviaStartCommand : Command() {
        override val name: String = "start"
        override val description: String = "Starts a game of trivia"
        override val guildOnly: Boolean = true
        override val permission: String? = "trivia.manage"
        override val argumentInfo: ArgumentInfo = ArgumentInfo(
            Argument("category", "Which category you wish to play."),
            Argument("amount", "Amount of questions you wish to ask. 1-1000"),
            Argument("difficulty", "The difficulty of the questions you wish to ask. easy | medium | hard")
        )

        override fun execute(message: Message, args: Arguments): Response {
            val categoryArg = args.next("You did not specify a category!")
            val category = gameManager.categories.trivia_categories.firstOrNull { it.id == categoryArg.toInt() }
                ?: return CategoryNotFoundResponse(categoryArg)
            val amount = args.next("You did not specify an amount!").toInt().coerceAtMost(1000)
            val difficulty = when (args.next("You did not specify a difficulty!").toLowerCase()) {
                "1", "easy" -> "easy"
                "2", "medium" -> "medium"
                "3", "hard" -> "hard"
                else -> return ErrorResponse("Difficulty not accepted", "Difficulty must be easy|medium|hard or 1|2|3")
            }

            gameManager.prepareGame(category, amount, difficulty, message)

            return SuccessResponse("I have started preparing your game!")

        }

    }

    private inner class CategoryNotFoundResponse(input: String) :
        ErrorResponse(
            "Category $input was not found",
            "Please use the identifier given for the category you wish to use!"
        ) {
        init {
            gameManager.categories.trivia_categories.forEach {
                addField(it.id.toString(), it.name, true)
            }
        }
    }
}