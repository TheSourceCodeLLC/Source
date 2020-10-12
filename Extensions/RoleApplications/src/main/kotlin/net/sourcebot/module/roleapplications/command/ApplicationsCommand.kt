package net.sourcebot.module.roleapplications.command

import net.dv8tion.jda.api.entities.Message
import net.sourcebot.Source
import net.sourcebot.api.command.Command
import net.sourcebot.api.command.RootCommand
import net.sourcebot.api.command.argument.Argument
import net.sourcebot.api.command.argument.ArgumentInfo
import net.sourcebot.api.command.argument.Arguments
import net.sourcebot.api.response.*
import net.sourcebot.module.roleapplications.data.ApplicationHandler
import java.time.Instant

class ApplicationsCommand(
    val appHandler: ApplicationHandler
) : RootCommand() {
    override val name = "applications"
    override val description = "Manages applications."
    override val guildOnly = true
    override val aliases = arrayOf("application", "app", "apps")
    override val permission = name

    init {
        addChildren(
            ApplicationsCreateCommand(),
            ApplicationsDeleteCommand(),
            ApplicationsInfoCommand(),
            ApplicationsListCommand()
        )
    }

    private inner class ApplicationsCreateCommand : Bootstrap(
        "create",
        "Creates an application."
    ) {
        override val argumentInfo: ArgumentInfo = ArgumentInfo(
            Argument("name", "The name of the application being created."),
            Argument(
                "questions",
                "The questions the application should have (each question must be surrounded in quotes)."
            )
        )

        override fun execute(message: Message, args: Arguments): Response {

            val appCache = appHandler[message.guild]
            val name = args.next("You did not enter a name for the application being created!").toLowerCase()
            if (appCache.getApplication(name) != null) {
                return StandardErrorResponse(
                    "Application already exists!",
                    "An application already exists with that name!"
                )
            }

            // This prevents there from being apps with 0 questions, while maintaining the same error format as the no name response
            val questions: MutableList<String> = mutableListOf(
                args.next("You did not enter a question for the application being created!")
            )

            while (args.hasNext()) {
                questions.add(args.next("Unable to find question"))
            }

            appCache.createApplication(name, questions, message.author.id)

            return StandardSuccessResponse("Application Created!", "An application names `$name` has been created!")
        }

    }

    private inner class ApplicationsDeleteCommand : Bootstrap(
        "delete",
        "Deletes an application."
    ) {
        override val argumentInfo: ArgumentInfo = ArgumentInfo(
            Argument("name", "The name of the application being deleted.")
        )

        override fun execute(message: Message, args: Arguments): Response {

            val appCache = appHandler[message.guild]
            val name = args.next("You did not enter a name for the application being deleted!").toLowerCase()
            val application = appCache.getApplication(name)
                ?: return NoApplicationFound(name)

            appCache.deleteApplication(name)

            return StandardSuccessResponse(
                "Deleted Application!",
                "Deleted an application with then name of `${application.name}`"
            )
        }
    }

    private inner class ApplicationsInfoCommand : Bootstrap(
        "info",
        "Shows application info"
    ) {

        override val argumentInfo: ArgumentInfo = ArgumentInfo(
            Argument("name", "The name of the application being viewed.")
        )

        override fun execute(message: Message, args: Arguments): Response {

            val appCache = appHandler[message.guild]
            val name = args.next("You did not enter a name for the application being viewed!").toLowerCase()
            val application = appCache.getApplication(name)
                ?: return NoApplicationFound(name)


            val createdByUser = message.jda.getUserById(application.creator)
            val creator = createdByUser?.asTag ?: "Unknown"

            val createdTime = Instant.ofEpochMilli(application.created).atZone(Source.TIME_ZONE)
            val created = Source.DATE_TIME_FORMAT.format(createdTime)

            val questionsSB = StringBuilder("")
            val questionSize = application.questions.size

            for (index in 0 until questionSize) {
                val questionNum = index + 1
                val question = application.questions[index]

                questionsSB.append("$questionNum. $question")
                if (questionNum != questionSize) {
                    questionsSB.append("\n")
                }
            }

            val questionsStr = questionsSB.toString()

            val response = StandardInfoResponse(
                "Application Information", "Information for application `${application.name}`:"
            )
            response.addField("Creator", creator, false)
                .addField("Created", created, false)
                .addField("Questions", questionsStr, false)

            return response
        }
    }

    private inner class ApplicationsListCommand : Bootstrap(
        "list",
        "Lists all of the applications."
    ) {

        override fun execute(message: Message, args: Arguments): Response {

            val appCache = appHandler[message.guild]
            val appList = appCache.getApplications()
            if (appList.isEmpty()) {
                return StandardErrorResponse("No applications found!", "This server currently has no applications!")
            }

            return StandardInfoResponse("Application List",
                appList.joinToString(", ") { "`${it.name}`" }
            )
        }
    }

    private class NoApplicationFound(name: String) :
        StandardErrorResponse("No application found!", "There is no application named `$name`!")

    private abstract class Bootstrap(
        final override val name: String,
        final override val description: String
    ) : Command() {
        final override val permission = "applications.$name"
        final override val guildOnly = true
    }
}