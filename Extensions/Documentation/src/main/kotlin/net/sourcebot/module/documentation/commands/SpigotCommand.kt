package net.sourcebot.module.documentation.commands

import me.hwiggy.kommander.arguments.Adapter
import me.hwiggy.kommander.arguments.Arguments
import me.hwiggy.kommander.arguments.Synopsis
import net.dv8tion.jda.api.entities.Message
import net.sourcebot.api.response.Response
import net.sourcebot.api.response.StandardInfoResponse
import net.sourcebot.module.documentation.commands.bootstrap.JavadocCommand
import net.sourcebot.module.documentation.handler.JenkinsHandler

class SpigotCommand : JavadocCommand(
    "spigot",
    "Allows the user to query the Spigot Documentation."
) {
    override val synopsis = Synopsis {
        optParam("query", "The Spigot element you are seeking help for.", Adapter.single())
    }

    private val jenkinsHandler = JenkinsHandler(
        "https://hub.spigotmc.org/javadocs/spigot/overview-tree.html",
        "Spigot Javadocs"
    )

    override fun execute(sender: Message, arguments: Arguments.Processed): Response {
        val query = arguments.optional<String>("query")
        return if (query != null) {
            jenkinsHandler.retrieveResponse(sender.author, query)
        } else {
            val authorName = sender.author.name
            val description =
                "You can find the Spigot Documentation at [hub.spigotmc.org](https://hub.spigotmc.org/javadocs/spigot/)"
            StandardInfoResponse(authorName, description)
        }
    }
}