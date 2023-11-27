package net.sourcebot.module.latex.command

import me.hwiggy.kommander.InvalidSyntaxException
import me.hwiggy.kommander.arguments.Adapter
import me.hwiggy.kommander.arguments.Arguments
import me.hwiggy.kommander.arguments.Synopsis
import net.dv8tion.jda.api.entities.Message
import net.sourcebot.api.command.RootCommand
import net.sourcebot.api.response.EmptyResponse
import net.sourcebot.api.response.Response
import net.sourcebot.module.latex.Latex

class LatexCommand : RootCommand() {
    override val name = "latex"
    override val description = "Parse LaTeX into an image."
    override val permission = "latex.latex"
    override val aliases = listOf("tex")

    override val synopsis = Synopsis {
        reqParam("expression", "The expression you would like to parse.", Adapter.single())
    }

    override fun execute(sender: Message, arguments: Arguments.Processed): Response {
        val expression = arguments.required<String>("expression", "You did not specify a LaTeX expression!")
        if (!expression.startsWith("`") and !expression.endsWith("`")) throw InvalidSyntaxException(
            "LaTeX expression must be surrounded by single backticks!"
        )
        val image = Latex.parse(expression.substring(1, expression.length - 1))
        sender.delete().queue {
            Latex.send(sender.author, sender.channel, image)
        }
        return EmptyResponse()
    }
}