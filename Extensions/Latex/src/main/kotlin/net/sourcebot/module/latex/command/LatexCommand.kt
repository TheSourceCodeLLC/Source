package net.sourcebot.module.latex.command

import net.dv8tion.jda.api.entities.Message
import net.sourcebot.api.command.InvalidSyntaxException
import net.sourcebot.api.command.RootCommand
import net.sourcebot.api.command.argument.Argument
import net.sourcebot.api.command.argument.ArgumentInfo
import net.sourcebot.api.command.argument.Arguments
import net.sourcebot.api.response.EmptyResponse
import net.sourcebot.api.response.Response
import net.sourcebot.module.latex.Latex

class LatexCommand : RootCommand() {
    override val name = "latex"
    override val description = "Parse LaTeX into an image."
    override val permission = "latex.latex"
    override val aliases = arrayOf("tex")

    override val argumentInfo = ArgumentInfo(
        Argument("expression", "The expression you would like to parse.")
    )

    private val validator = Regex("`(.+)`")
    override fun execute(message: Message, args: Arguments): Response {
        val expression = args.next("You did not specify a LaTeX expression!")
        validator.matchEntire(expression)?.also {
            val image = Latex.parse(validator.matchEntire(expression)!!.groupValues[1])
            message.delete().queue {
                Latex.send(message.author, message.channel, image)
            }
        } ?: throw InvalidSyntaxException(
            "LaTeX expression must be surrounded by single backticks!"
        )
        return EmptyResponse()
    }
}