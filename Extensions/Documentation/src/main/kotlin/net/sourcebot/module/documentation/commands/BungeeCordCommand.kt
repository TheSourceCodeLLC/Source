package net.sourcebot.module.documentation.commands

import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.User
import net.sourcebot.api.command.RootCommand
import net.sourcebot.api.command.argument.Argument
import net.sourcebot.api.command.argument.ArgumentInfo
import net.sourcebot.api.command.argument.Arguments
import net.sourcebot.api.response.InfoResponse
import net.sourcebot.api.response.Response
import net.sourcebot.module.documentation.dochandlers.JenkinsHandler
import net.sourcebot.module.documentation.utility.SelectorModel

class BungeeCordCommand : RootCommand() {
    override val name: String = "bungeecord"
    override val description: String = "Allows the user to query the BungeeCord Documentation."
    override var cleanupResponse: Boolean = false
    override val argumentInfo: ArgumentInfo = ArgumentInfo(
        Argument("query", "The item you are searching for in the BungeeCord documentation.")
    )
    override val aliases: Array<String> = arrayOf("bungee")
    override val permission = "documentation.$name"

    private val jenkinsHandler = JenkinsHandler(
        "https://papermc.io/javadocs/waterfall/allclasses-noframe.html",
        "https://avatars0.githubusercontent.com/u/4350249?s=200&v=4",
        "BungeeCord Javadocs"
    )

    override fun execute(message: Message, args: Arguments): Response {
        return if (args.hasNext()) {
            val query = args.next("Unable to find query w/o version!")

            jenkinsHandler.retrieveResponse(message, query)
        } else {
            val authorName = message.author.name
            val description =
                "You can find the BungeeCord Documentation at [papermc.io](https://papermc.io/javadocs/waterfall/)"
            return InfoResponse(authorName, description)
        }
    }

    override fun postResponse(response: Response, forWhom: User, message: Message) {
        SelectorModel.selectorCache.updateSelector(forWhom, message)
    }
}