package net.sourcebot.module.documentation.commands

import me.hwiggy.kommander.arguments.Adapter
import me.hwiggy.kommander.arguments.Arguments
import me.hwiggy.kommander.arguments.Synopsis
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.utils.MarkdownUtil
import net.sourcebot.api.response.Response
import net.sourcebot.api.response.StandardInfoResponse
import net.sourcebot.module.documentation.commands.bootstrap.JavadocCommand
import net.sourcebot.module.documentation.handler.JenkinsHandler

class JavaCommand : JavadocCommand(
    "java",
    "Allows the user to query the Java Documentation."
) {
    private val defaultVersion = 14
    override val synopsis = Synopsis {
        optParam(
            "version", "The version of Java you are seeking help for.",
            Adapter.int(7, 15, "Java version should be between 7 and 15!"),
            defaultVersion
        )
        optParam("query", "The Java element you are seeking help for.", Adapter.single())
    }

    private val javadocCache: MutableMap<Int, JenkinsHandler> = mutableMapOf()

    private val jenkinsCompute = { version: Int ->
        val connectionString =
            if (version >= 11) "https://docs.oracle.com/en/java/javase/$version/docs/api/overview-tree.html"
            else "https://docs.oracle.com/javase/$version/docs/api/allclasses-noframe.html"
        JenkinsHandler(connectionString, "Java $version Javadocs")
    }

    override fun execute(sender: Message, arguments: Arguments.Processed): Response {
        val version = arguments.optional("version", defaultVersion)
        val jenkinsHandler = javadocCache.computeIfAbsent(version, jenkinsCompute)
        val query = arguments.optional<String>("query")
        return if (query != null) {
            jenkinsHandler.retrieveResponse(sender.author, query)
        } else StandardInfoResponse(
            sender.author.name,
            "You can find the Java Documentation at ${
                MarkdownUtil.maskedLink("docs.oracle.com", jenkinsHandler.url)
            }"
        )
    }
}