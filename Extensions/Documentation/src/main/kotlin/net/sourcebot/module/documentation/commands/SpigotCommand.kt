package net.sourcebot.module.documentation.commands

import net.dv8tion.jda.api.entities.Message
import net.sourcebot.api.command.RootCommand
import net.sourcebot.api.command.argument.Argument
import net.sourcebot.api.command.argument.ArgumentInfo
import net.sourcebot.api.command.argument.Arguments
import net.sourcebot.api.response.InfoResponse
import net.sourcebot.api.response.Response
import net.sourcebot.module.documentation.utility.DocSelectorStorage
import net.sourcebot.module.documentation.utility.JenkinsHandler

class SpigotCommand : RootCommand() {
    override val name: String = "spigot"
    override val description: String = "Allows the user to query the Spigot Documentation."
    override var cleanupResponse: Boolean = false
    override val argumentInfo: ArgumentInfo = ArgumentInfo(
        Argument("query", "The item you are searching for in the Spigot documentation.")
    )

    private val jenkinsHandler = JenkinsHandler("https://hub.spigotmc.org/javadocs/spigot/overview-tree.html",
        "https://avatars0.githubusercontent.com/u/4350249?s=200&v=4",
        "Spigot Javadocs")

    override fun execute(message: Message, args: Arguments): Response {
        return if (args.hasNext()) {
            val query = args.next("Unable to find query w/o version!")

            jenkinsHandler.retrieveDocAlert(message, message.author, query)
        } else {
            val authorName = message.author.name
            val description =
                "You can find the Spigot Documentation at [hub.spigotmc.org](https://hub.spigotmc.org/javadocs/spigot/)"
            InfoResponse(authorName, description)
        }


    }

    override fun postResponse(response: Response, message: Message) {
        DocSelectorStorage.updateSelector(message)
    }
}