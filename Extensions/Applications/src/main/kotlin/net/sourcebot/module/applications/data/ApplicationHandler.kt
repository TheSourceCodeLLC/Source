package net.sourcebot.module.applications.data

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.TextChannel
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.message.priv.PrivateMessageReceivedEvent
import net.sourcebot.api.configuration.ConfigurationManager
import net.sourcebot.api.database.MongoDB
import net.sourcebot.api.event.EventSubscriber
import net.sourcebot.api.event.EventSystem
import net.sourcebot.api.event.SourceEvent
import net.sourcebot.api.response.StandardErrorResponse
import net.sourcebot.module.applications.Applications
import java.util.concurrent.TimeUnit

class ApplicationHandler(
    private val applications: Applications,
    private val mongodb: MongoDB,
    private val configurationManager: ConfigurationManager
) : EventSubscriber<Applications> {

    private val activeApplicationCache = CacheBuilder.newBuilder().weakKeys()
        .expireAfterWrite(45, TimeUnit.MINUTES)
        .build<User, ActiveApplicationModel>()

    private val applicationGuildCache = CacheBuilder.newBuilder().weakKeys()
        .expireAfterWrite(10, TimeUnit.MINUTES)
        .build(object : CacheLoader<Guild, ApplicationCache>() {
            override fun load(guild: Guild) = ApplicationCache(mongodb.getCollection(guild.id, "applications"))
        })

    override fun subscribe(
        module: Applications,
        jdaEvents: EventSystem<GenericEvent>,
        sourceEvents: EventSystem<SourceEvent>
    ) {
        jdaEvents.listen(applications, this::onMessageReceived)
    }

    fun getApplicationChannel(guild: Guild): TextChannel? {
        val channelId: String = configurationManager[guild].required("applications.channel") { "" }

        if (channelId.isEmpty()) return null
        return guild.getTextChannelById(channelId)
    }

    fun hasActiveApplication(user: User): Boolean = activeApplicationCache.getIfPresent(user) != null

    fun startApplication(user: User, guildId: String, appModel: ApplicationModel) {
        val activeAppModel = ActiveApplicationModel(user, guildId, mutableMapOf(), appModel)
        insertActiveApplication(user, activeAppModel)

        val question = appModel.questions[0]
        val appName = appModel.name

        val channel = user.openPrivateChannel().complete()
        channel.sendMessage("Starting the **$appName** application `(Guild ID: $guildId)`").complete()
        channel.sendMessage("**Question 1.** $question `(Application: $appName, Guild ID: $guildId)`").queue()
    }

    fun insertActiveApplication(user: User, activeAppModel: ActiveApplicationModel) {
        activeApplicationCache.put(user, activeAppModel)
    }

    operator fun get(guild: Guild): ApplicationCache = applicationGuildCache[guild]

    private fun onMessageReceived(event: PrivateMessageReceivedEvent) {
        val user = event.author
        val activeAppModel = activeApplicationCache.getIfPresent(user) ?: return
        val channel = event.channel
        val message = event.message

        val previousAnswerNumber = activeAppModel.answerMap.size
        val previousAnswer = message.contentRaw

        activeAppModel.addAnswer(previousAnswerNumber, previousAnswer)

        val answerMap = activeAppModel.answerMap
        val guildId = activeAppModel.guildId
        val appModel = activeAppModel.appModel
        val questions = appModel.questions
        val appModelName = appModel.name

        val guild = event.jda.getGuildById(guildId)
        if (guild == null) {
            val errorResponse = StandardErrorResponse(
                "Unknown Guild!",
                "Unable to find a guild with an id of `$guildId`"
            )

            channel.sendMessage(errorResponse.asEmbed(user)).queue()
            activeApplicationCache.invalidate(user)
            return
        }

        val questionNumber = answerMap.size + 1

        if (questionNumber > questions.size) {
            val success = sendCompletedApplication(guild, message, activeAppModel)
            if (success) {
                channel.sendMessage("**$appModelName** application successfully completed! `(Guild ID: $guildId)`")
                    .queue()
            }

            activeApplicationCache.invalidate(user)
            return
        }

        // -1 because questions start at 0
        val question = questions[questionNumber - 1]
        channel.sendMessage("**Question $questionNumber.** $question `(Application: $appModelName, Guild ID: $guildId)`")
            .queue()

        activeApplicationCache.invalidate(user)
        activeApplicationCache.put(user, activeAppModel)
    }

    private fun sendCompletedApplication(guild: Guild, message: Message, activeApp: ActiveApplicationModel): Boolean {
        val appChannel = getApplicationChannel(guild)
        val user = message.author
        if (appChannel == null) {
            val msgChannel = message.channel

            val errorResponse = StandardErrorResponse(
                "Not Configured!",
                "This guild has not set up the channel in which completed apps are sent to, because of this you can not complete any apps."
            )
            msgChannel.sendMessage(errorResponse.asEmbed(user)).queue()
            return false
        }

        val stringBuilder = StringBuilder("**Discord Tag:** ${user.asTag}\n**Discord Id:** ${user.id}\n```diff")
        val appQuestions = activeApp.appModel.questions
        val answerMap = activeApp.answerMap

        for (index in appQuestions.indices) {
            val questionNumber = index + 1
            val question = "+ $questionNumber. ${appQuestions[index]}"
            val answer = if (answerMap.containsKey(index)) answerMap[index]!! else "Answer not found!"

            stringBuilder.append("\n\n$question\n- $answer")
        }

        // This is unsafe and mega ugly, waiting for pagination system in source
        // Unsafe because answers can cause the message to go over the msg char limit
        appChannel.sendMessage("$stringBuilder```").queue()
        return true
    }

}