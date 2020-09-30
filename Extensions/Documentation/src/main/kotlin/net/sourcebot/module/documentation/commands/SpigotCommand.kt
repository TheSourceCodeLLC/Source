package net.sourcebot.module.documentation.commands

import net.dv8tion.jda.api.entities.Message
import net.sourcebot.api.command.argument.Argument
import net.sourcebot.api.command.argument.ArgumentInfo
import net.sourcebot.api.command.argument.Arguments
import net.sourcebot.api.response.InfoResponse
import net.sourcebot.api.response.Response
import net.sourcebot.module.documentation.commands.bootstrap.JavadocCommand
import net.sourcebot.module.documentation.dochandlers.JenkinsHandler


class SpigotCommand : JavadocCommand(
    "spigot", "Allows the user to query the Spigot Documentation."
) {
    override val argumentInfo: ArgumentInfo = ArgumentInfo(
        Argument("query", "The item you are searching for in the Spigot documentation.")
    )

    private val jenkinsHandler = JenkinsHandler(
        "https://hub.spigotmc.org/javadocs/spigot/overview-tree.html",
        "https://avatars0.githubusercontent.com/u/4350249?s=200&v=4",
        "Spigot Javadocs"
    )

    override fun execute(message: Message, args: Arguments): Response {
        return if (args.hasNext()) {
            val query = args.next("Unable to find query w/o version!")

            jenkinsHandler.retrieveResponse(message, query)
        } else {
            val authorName = message.author.name
            val description =
                "You can find the Spigot Documentation at [hub.spigotmc.org](https://hub.spigotmc.org/javadocs/spigot/)"
            InfoResponse(authorName, description)
        }
    }
}