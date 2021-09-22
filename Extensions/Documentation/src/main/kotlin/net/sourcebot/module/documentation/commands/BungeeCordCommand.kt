package net.sourcebot.module.documentation.commands

import me.hwiggy.kommander.arguments.Adapter
import me.hwiggy.kommander.arguments.Arguments
import me.hwiggy.kommander.arguments.Synopsis
import net.dv8tion.jda.api.entities.Message
import net.sourcebot.api.response.Response
import net.sourcebot.api.response.StandardInfoResponse
import net.sourcebot.module.documentation.commands.bootstrap.JavadocCommand
import net.sourcebot.module.documentation.handler.JenkinsHandler

class BungeeCordCommand : JavadocCommand(
    "bungeecord",
    "Allows the user to query the BungeeCord Documentation."
) {
    override val synopsis = Synopsis {
        optParam("query", "The item you are searching for in the BungeeCord documentation.", Adapter.single())
    }
    override val aliases = listOf("bungee")

    private val jenkinsHandler = JenkinsHandler(
        "https://papermc.io/javadocs/waterfall/allclasses-noframe.html",
        "BungeeCord Javadocs"
    )

    override fun execute(sender: Message, arguments: Arguments.Processed): Response {
        return arguments.optional<String>("query")?.let { it ->
            jenkinsHandler.retrieveResponse(sender.author, it)
        } ?: StandardInfoResponse(
            sender.author.name,
            "You can find the BungeeCord Documentation at [papermc.io](https://papermc.io/javadocs/waterfall/)"
        )
    }
}