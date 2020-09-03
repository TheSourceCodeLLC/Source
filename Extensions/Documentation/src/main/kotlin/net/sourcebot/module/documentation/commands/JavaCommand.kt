package net.sourcebot.module.documentation.commands

import net.dv8tion.jda.api.entities.Message
import net.sourcebot.api.command.RootCommand
import net.sourcebot.api.command.argument.Argument
import net.sourcebot.api.command.argument.ArgumentInfo
import net.sourcebot.api.command.argument.Arguments
import net.sourcebot.api.response.ErrorResponse
import net.sourcebot.api.response.InfoResponse
import net.sourcebot.api.response.Response
import net.sourcebot.module.documentation.utility.DocSelectorStorage
import net.sourcebot.module.documentation.utility.JenkinsHandler

class JavaCommand : RootCommand() {
    override val name: String = "java"
    override val description: String = "Allows the user to query the Java Documentation."
    override var cleanupResponse: Boolean = false
    override val argumentInfo: ArgumentInfo = ArgumentInfo(
        Argument("version", "The version of the java docs you would like to query, default is 13."),
        Argument("query", "The item you are searching for in the Java documentation.")
    )

    private val iconUrl = "https://upload.wikimedia.org/wikipedia/en/thumb/3/30/Java_programming_language_logo.svg/1200px-Java_programming_language_logo.svg.png"

    private val javadocCache: MutableMap<Int, JenkinsHandler> = mutableMapOf()

    override fun execute(message: Message, args: Arguments): Response {
        var jenkinsHandler = javadocCache[13]

        if (jenkinsHandler == null) {
            val connectionString = "https://docs.oracle.com/en/java/javase/13/docs/api/overview-tree.html"

            jenkinsHandler = JenkinsHandler(connectionString, iconUrl, "Java 13 Javadocs")
            javadocCache[13] = jenkinsHandler
        }

        if (args.hasNext()) {
            var query = args.next("Unable to find query w/o version!")
            if (args.hasNext()) {
                try {
                    val version = query.toInt()
                    query = args.next("Unable to find query w/ version!")

                    jenkinsHandler = javadocCache[version]
                    if (jenkinsHandler == null) {
                        val connectionString = if (version >= 11) "https://docs.oracle.com/en/java/javase/$version/docs/api/overview-tree.html"
                        else "https://docs.oracle.com/javase/$version/docs/api/allclasses-noframe.html"

                        jenkinsHandler = JenkinsHandler(connectionString, iconUrl, "Java $version Javadocs")
                        javadocCache[version] = jenkinsHandler
                    }
                } catch (ex: Exception) {
                    jenkinsHandler = javadocCache[13]
                }
            }

            if (jenkinsHandler == null) {
                return ErrorResponse(message.author.name, "Uh Oh, something went wrong! Please try again.")
            }

            return jenkinsHandler.retrieveDocAlert(message, message.author, query)
        } else {
            val authorName = message.author.name
            val description =
                "You can find the Java Documentation at [docs.oracle.com](https://docs.oracle.com/javase/13/docs/)"
            return InfoResponse(authorName, description)
        }


    }

    override fun postResponse(response: Response, message: Message) {
        DocSelectorStorage.updateSelector(message)
    }
}