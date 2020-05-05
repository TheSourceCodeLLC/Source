package net.sourcebot.base

import net.dv8tion.jda.api.entities.Message
import net.sourcebot.api.alert.Alert
import net.sourcebot.api.alert.info.EightBallAlert
import net.sourcebot.api.command.Command
import net.sourcebot.api.command.argument.Argument
import net.sourcebot.api.command.argument.ArgumentInfo
import net.sourcebot.api.command.argument.Arguments

class EightBallCommand : Command() {
    private val answers = arrayOf(
        "It is certain.", "It is decidedly so.",
        "Without a doubt.", "Yes – definitely.",
        "You may rely on it.", "As I see it, yes.",
        "Most likely.", "Outlook good.",
        "Yes.", "Signs point to yes.",
        "Reply hazy, try again.", "Ask again later.",
        "Better not tell you now.", "Cannot predict now.",
        "Concentrate and ask again.", "Don't count on it.",
        "My reply is no.", "My sources say no.",
        "Outlook not so good.", "Very doubtful."
    )
    override val name = "8ball"
    override val description = "Answers your question"
    override val argumentInfo = ArgumentInfo(
        Argument("question", "The question you want answered")
    )

    override fun execute(message: Message, args: Arguments): Alert {
        val question = args.slurp(" ", "Missing argument for parameter `question`!")
        val answer = answers.random()
        return EightBallAlert(question, answer)
    }
}