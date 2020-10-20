package net.sourcebot.module.documentation.commands

import net.dv8tion.jda.api.entities.Message
import net.sourcebot.api.command.argument.Argument
import net.sourcebot.api.command.argument.ArgumentInfo
import net.sourcebot.api.command.argument.Arguments
import net.sourcebot.api.response.Response
import net.sourcebot.api.response.StandardInfoResponse
import net.sourcebot.module.documentation.commands.bootstrap.JavadocCommand
import net.sourcebot.module.documentation.handler.JenkinsHandler

class JDACommand : JavadocCommand(
    "jda", "Allows the user to query the JDA Documentation."
) {
    override val argumentInfo: ArgumentInfo = ArgumentInfo(
        Argument("query", "The item you are searching for in the JDA documentation.")
    )
    private val jenkinsHandler = JenkinsHandler(
        "https://ci.dv8tion.net/job/JDA/javadoc/allclasses.html",
        "https://camo.githubusercontent.com/f2e0860a3b1a34658f23a8bcea96f9725b1f8a73/68747470733a2f2f692e696d6775722e636f6d2f4f4737546e65382e706e67",
        "JDA Javadocs"
    )

    override fun execute(message: Message, args: Arguments): Response {
        return if (args.hasNext()) {
            val query = args.next("Unable to find query w/o version!")

            jenkinsHandler.retrieveResponse(message.author, query)
        } else {
            val authorName = message.author.name
            val description =
                "You can find the JDA Documentation at [ci.dv8tion.net](https://ci.dv8tion.net/job/JDA/javadoc/index.html)"
            StandardInfoResponse(authorName, description)
        }

    }
}