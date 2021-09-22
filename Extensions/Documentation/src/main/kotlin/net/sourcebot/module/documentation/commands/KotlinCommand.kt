package net.sourcebot.module.documentation.commands

import me.hwiggy.kommander.arguments.Adapter
import me.hwiggy.kommander.arguments.Arguments
import me.hwiggy.kommander.arguments.Synopsis
import net.dv8tion.jda.api.entities.Message
import net.sourcebot.api.response.Response
import net.sourcebot.api.response.StandardErrorResponse
import net.sourcebot.api.response.StandardInfoResponse
import net.sourcebot.module.documentation.commands.bootstrap.DocumentationCommand
import net.sourcebot.module.documentation.handler.KotlinHandler

class KotlinCommand : DocumentationCommand(
    "kotlin", "Allows the user to query the Kotlin Documentation"
) {
    override val synopsis = Synopsis {
        optParam("query", "The item you are searching for in the Kotlin documentation.", Adapter.single())
    }

    private val kotlinHandler = KotlinHandler()

    override fun execute(sender: Message, arguments: Arguments.Processed): Response {
        val user = sender.author
        val query = arguments.optional<String>("query")
            ?.replace("#", ".")
            ?.removeSuffix("()")
        if (query == null) {
            val description =
                "You can find the Kotlin Documentation at [kotlinlang.org](https://kotlinlang.org/docs/reference/)"
            return StandardInfoResponse(user.name, description)
        }
        val notFoundResponse = StandardErrorResponse(user.name, "Unable to find `$query` in the Kotlin Documentation!")
        val results = kotlinHandler.search(query)
        return if (results.size > 0) {
            results[0].createResponse()
        } else {
            notFoundResponse
        }
    }
}