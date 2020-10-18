package net.sourcebot.module.roleapplications.command

import net.dv8tion.jda.api.entities.ChannelType
import net.dv8tion.jda.api.entities.Message
import net.sourcebot.Source
import net.sourcebot.api.command.Command
import net.sourcebot.api.command.RootCommand
import net.sourcebot.api.command.argument.Argument
import net.sourcebot.api.command.argument.ArgumentInfo
import net.sourcebot.api.command.argument.Arguments
import net.sourcebot.api.response.Response
import net.sourcebot.api.response.StandardErrorResponse
import net.sourcebot.api.response.StandardInfoResponse
import net.sourcebot.api.response.StandardSuccessResponse
import net.sourcebot.module.roleapplications.data.ActiveApplicationModel
import net.sourcebot.module.roleapplications.data.ApplicationHandler
import java.time.Instant

class ApplicationsCommand(
    val appHandler: ApplicationHandler
) : RootCommand() {
    override val name = "applications"
    override val description = "Manages applications."
    override val guildOnly = false
    override val aliases = arrayOf("application", "app", "apps")
    override val permission = name

    init {
        addChildren(
            ApplicationsCreateCommand(),
            ApplicationsDeleteCommand(),
            ApplicationsInfoCommand(),
            ApplicationsListCommand(),
            ApplicationsStartCommand(),
            ApplicationsRecoverCommand()
        )
    }

    private inner class ApplicationsStartCommand : Bootstrap(
        "start",
        "Starts an application."
    ) {
        override val argumentInfo: ArgumentInfo = ArgumentInfo(
            Argument("name", "The name of the application being started.")
        )

        override fun execute(message: Message, args: Arguments): Response {
            val user = message.author
            val modelName = args.next("You did not provide the name of the application you are attempting to start!")
            val guild = message.guild

            if (appHandler.getApplicationChannel(guild) == null) {
                return StandardErrorResponse(
                    "Not Configured!",
                    "This guild has not set up the channel in which completed apps are sent to, because of this you can not start any apps."
                )
            }

            val appModel = appHandler[guild].getApplication(modelName) ?: return NoApplicationFound(modelName)

            try {
                user.openPrivateChannel().complete()
            } catch (ex: Exception) {
                return StandardErrorResponse(
                    "DMs Closed!",
                    "Your DMs are required to be open to fill out an application!"
                )
            }

            appHandler.startApplication(user, guild.id, appModel)
            return StandardSuccessResponse(
                "Application Started!",
                "Head over to your DMs to complete the application!"
            )
        }
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
        "Shows application info."
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
            println(appHandler.getApplicationChannel(message.guild))
            val appList = appCache.getApplications()
            if (appList.isEmpty()) {
                return StandardErrorResponse("No applications found!", "This server currently has no applications!")
            }

            return StandardInfoResponse("Application List",
                appList.joinToString(", ") { "`${it.name}`" }
            )
        }
    }

    private inner class ApplicationsRecoverCommand : Bootstrap(
        "recover",
        "Recovers an active application if the bot happens to restart during the process.",
        false
    ) {
        override val argumentInfo: ArgumentInfo = ArgumentInfo(
            Argument("message id", "The id of the start message for the app sent by the bot.")
        )

        override fun execute(message: Message, args: Arguments): Response {
            val msgChannel = message.channel
            val jda = message.jda
            val user = message.author

            if (msgChannel.type != ChannelType.PRIVATE) {
                return StandardErrorResponse("Uh Oh!", "This command must be ran in a DM!")
            }

            if (appHandler.hasActiveApplication(user)) {
                return StandardErrorResponse("Uh Oh!", "You are already in the process of taking an application!")
            }

            val startMsgId = args.next("You did not enter an id for the start message of the application!")

            val startMessage = msgChannel.retrieveMessageById(startMsgId).complete() ?: return StandardErrorResponse(
                "Unknown Message!",
                "Unable to find a message with the id `$startMsgId`"
            )

            if (startMessage.author != jda.selfUser) {
                return StandardErrorResponse("Incorrect Author!", "The start message must be sent by the bot!")
            }

            val rawContent = startMessage.contentRaw
            if (!rawContent.matches("Starting the .* application `\\(Guild ID: \\d+\\)`".toRegex())) {
                return StandardErrorResponse(
                    "Incorrect Message!",
                    "The start message is in the following format `Starting the XYZ application (Guild ID: 1234)`"
                )
            }

            val modelName = rawContent.substringAfter("**").substringBefore("**")
            val guildId = rawContent.substringAfterLast("(").replace("[^0-9]".toRegex(), "")

            val foundGuild = jda.getGuildById(guildId)
                ?: return StandardErrorResponse(
                    "Unknown Guild!",
                    "Unable to find a guild with an id of `$guildId`"
                )

            if (!foundGuild.isMember(user)) {
                return StandardErrorResponse(
                    "Not a member!",
                    "Unable to recover this application because you are not a member of a guild with an id of `$guildId`"
                )
            }

            val appModel = appHandler[foundGuild].getApplication(modelName)
                ?: return StandardErrorResponse(
                    "Unable to identify application!",
                    "Unable to find an application with the name of `$modelName` in the guild with an id of `$guildId`"
                )


            val retrievedHistory = msgChannel.getHistoryAfter(startMessage, 100).complete().retrievedHistory
            val answerMap: MutableMap<Int, String> = mutableMapOf()


            var isNextMsgAnswer = false
            var previousQuestion = ""
            // The filter removes the message which invokes the command
            retrievedHistory.filter { retrievedHistory[0] != it }
                .asReversed() // Reverses so the history starts from the start message
                .forEach {
                    val author = it.author
                    val msgContent = it.contentRaw

                    if (isNextMsgAnswer && author != jda.selfUser) {
                        isNextMsgAnswer = false

                        val questionNumber = previousQuestion.substringBefore(".")
                            .replace("[^0-9]".toRegex(), "")
                            .toInt() - 1
                        answerMap[questionNumber] = msgContent
                        return@forEach
                    }

                    if (author == jda.selfUser) {

                        if (msgContent.matches(".* application successfully completed! `\\(Guild ID: \\d+\\)`".toRegex())) {
                            return StandardErrorResponse(
                                "Application Ended!",
                                "Can not recover an application which has already ended!"
                            )
                        }

                        if (msgContent.matches("\\*\\*Question \\d+\\..*\\(Application: .*, Guild ID: \\d+\\)`".toRegex())) {
                            println(msgContent)
                            isNextMsgAnswer = true
                            previousQuestion = msgContent
                            println("$isNextMsgAnswer : $previousQuestion")
                        }
                    }


                }

            val activeApplication = ActiveApplicationModel(user, foundGuild.id, answerMap, appModel)
            appHandler.insertActiveApplication(user, activeApplication)

            println(answerMap.size)
            val questionNumber = answerMap.size + 1

            if (questionNumber > appModel.questions.size) {
                return StandardErrorResponse(
                    "Unable to find next question!",
                    "The application with the name of `${appModel.name}` was not able to recover because there is no next question!"
                )
            }

            // - 1 because questions start at 0
            val question = appModel.questions[questionNumber - 1]

            user.openPrivateChannel()
                .complete()
                .sendMessage("**Question $questionNumber.** $question `(Application: ${appModel.name}, Guild ID: ${foundGuild.id}`")
                .queue()

            return StandardSuccessResponse(
                "Application Recovered!",
                "The application with the name `$name` for the guild with an id of `$guildId` has been recovered!"
            )
        }
    }

    private class NoApplicationFound(name: String) :
        StandardErrorResponse("No application found!", "There is no application named `$name`!")

    private abstract class Bootstrap(
        final override val name: String,
        final override val description: String,
        final override val guildOnly: Boolean = true
    ) : Command() {
        final override val permission = "applications.$name"
    }
}