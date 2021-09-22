package net.sourcebot.module.documentation.commands

import me.hwiggy.kommander.arguments.Adapter
import me.hwiggy.kommander.arguments.Arguments
import me.hwiggy.kommander.arguments.Synopsis
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.utils.MarkdownUtil
import net.sourcebot.api.ifPresentOrElse
import net.sourcebot.api.response.Response
import net.sourcebot.api.response.StandardInfoResponse
import net.sourcebot.module.documentation.commands.bootstrap.JavadocCommand
import net.sourcebot.module.documentation.handler.JenkinsHandler

class JDACommand : JavadocCommand(
    "jda",
    "Allows the user to query the JDA Documentation."
) {
    override val synopsis = Synopsis {
        optParam("query", "The JDA element you are seeking help for.", Adapter.single())
    }
    private val jenkinsHandler = JenkinsHandler(
        "https://ci.dv8tion.net/job/JDA/javadoc/allclasses.html",
        "JDA Javadocs"
    )

    override fun execute(sender: Message, arguments: Arguments.Processed): Response {
        return arguments.optional<String>("query").ifPresentOrElse(
            { jenkinsHandler.retrieveResponse(sender.author, it) },
            {
                val link = MarkdownUtil.maskedLink(
                    "ci.dv8tion.net", "https://ci.dv8tion.net/job/JDA/javadoc/index.html"
                )
                StandardInfoResponse(sender.author.name, "You can find the JDA Documentation at $link")
            }
        )
    }
}