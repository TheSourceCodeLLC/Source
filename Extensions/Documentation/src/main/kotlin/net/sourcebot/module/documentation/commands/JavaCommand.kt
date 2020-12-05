package net.sourcebot.module.documentation.commands

import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.utils.MarkdownUtil
import net.sourcebot.api.command.argument.Adapter
import net.sourcebot.api.command.argument.ArgumentInfo
import net.sourcebot.api.command.argument.Arguments
import net.sourcebot.api.command.argument.OptionalArgument
import net.sourcebot.api.response.Response
import net.sourcebot.api.response.StandardInfoResponse
import net.sourcebot.module.documentation.commands.bootstrap.JavadocCommand
import net.sourcebot.module.documentation.handler.JenkinsHandler

class JavaCommand : JavadocCommand(
    "java",
    "Allows the user to query the Java Documentation."
) {
    private val defaultVersion = 14
    override val argumentInfo: ArgumentInfo = ArgumentInfo(
        OptionalArgument(
            "version",
            "The version of the java docs you would like to query, default is $defaultVersion."
        ),
        OptionalArgument("query", "The item you are searching for in the Java documentation.")
    )

    private val javadocCache: MutableMap<Int, JenkinsHandler> = mutableMapOf()

    private val jenkinsCompute = { version: Int ->
        val connectionString =
            if (version >= 11) "https://docs.oracle.com/en/java/javase/$version/docs/api/overview-tree.html"
            else "https://docs.oracle.com/javase/$version/docs/api/allclasses-noframe.html"
        JenkinsHandler(connectionString, "Java $version Javadocs")
    }

    override fun execute(message: Message, args: Arguments): Response {
        val version = args.next(
            Adapter.int(7, 15, "Java Version should be between 7 and 15!")
        ) ?: defaultVersion
        val jenkinsHandler = javadocCache.computeIfAbsent(version, jenkinsCompute)
        val query = args.next()
        return if (query != null) {
            jenkinsHandler.retrieveResponse(message.author, query)
        } else StandardInfoResponse(
            message.author.name,
            "You can find the Java Documentation at ${
                MarkdownUtil.maskedLink("docs.oracle.com", jenkinsHandler.url)
            }"
        )
    }
}