package net.sourcebot.module.documentation.commands

import net.dv8tion.jda.api.entities.Message
import net.sourcebot.api.command.RootCommand
import net.sourcebot.api.command.argument.Argument
import net.sourcebot.api.command.argument.ArgumentInfo
import net.sourcebot.api.command.argument.Arguments
import net.sourcebot.api.response.ErrorResponse
import net.sourcebot.api.response.InfoResponse
import net.sourcebot.api.response.Response
import net.sourcebot.module.documentation.dochandlers.KotlinHandler

class KotlinCommand : RootCommand() {
    override val name: String = "kotlin"
    override val description: String = "Allows the user to query the Kotlin Documentation"
    override val argumentInfo: ArgumentInfo = ArgumentInfo(
        Argument("query", "The item you are searching for in the BungeeCord documentation.")
    )
    override var cleanupResponse: Boolean = false
    override val permission = "documentation.$name"

    private val kotlinHandler = KotlinHandler()

    override fun execute(message: Message, args: Arguments): Response {
        val user = message.author

        if (!args.hasNext()) {
            val description =
                "You can find the Kotlin Documentation at [kotlinlang.org](https://kotlinlang.org/docs/reference/)"
            return InfoResponse(user.name, description)
        }

        val query = args.next("Unable to find query w/o version!")
            .replace("#", ".").removeSuffix("()")


        val notFoundResponse = ErrorResponse(user.name, "Unable to find `$query` in the Kotlin Documentation!")

        val results = kotlinHandler.search(query)
        return if (results.size > 0) {
            results[0].createResponse()
        } else {
            notFoundResponse
        }

    }

}