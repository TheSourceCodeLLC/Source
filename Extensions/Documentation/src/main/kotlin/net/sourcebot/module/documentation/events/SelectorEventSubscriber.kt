package net.sourcebot.module.documentation.events

import net.dv8tion.jda.api.entities.ChannelType
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.sourcebot.api.event.EventSubscriber
import net.sourcebot.api.event.EventSystem
import net.sourcebot.api.event.SourceEvent
import net.sourcebot.api.response.StandardErrorResponse
import net.sourcebot.module.documentation.Documentation
import net.sourcebot.module.documentation.utility.DocResponse
import net.sourcebot.module.documentation.utility.SelectorModel

/**
 * This is an [EventSubscriber] which handles the Documentation Selection system
 *
 * @param docModule The main module class
 */
class SelectorEventSubscriber(private val docModule: Documentation) : EventSubscriber<Documentation> {

    private val selectorCache = SelectorModel.selectorCache
    private val deleteSeconds: Long = docModule.source.properties.required("commands.delete-seconds")

    override fun subscribe(
        module: Documentation,
        jdaEvents: EventSystem<GenericEvent>,
        sourceEvents: EventSystem<SourceEvent>
    ) {
        jdaEvents.listen(module, this::onMessageReceived)
    }

    /**
     * Listens to the [MessageReceivedEvent] and checks if the user has a selector, if so it gets the id from the
     * [Message], if it is malformed and invalid it sends an [StandardErrorResponse], if the [Message] text equals cancel
     * it will delete the [Message]s and remove [SelectorModel] from the cache, if the given id is valid it will modify
     * the [Message] to contain the new [DocResponse]
     *
     * @param event The [MessageReceivedEvent]
     */
    private fun onMessageReceived(event: MessageReceivedEvent) {
        val user = event.author
        val channelType: ChannelType = event.channelType

        val message = event.message
        val messageContent = message.contentRaw
        if (messageContent.startsWith("!")) return
        val docStorage: SelectorModel = selectorCache[user] ?: return
        val docMessage = docStorage.docMessage ?: return
        val infoList = docStorage.infoList
        val jenkinsHandler = docStorage.jenkinsHandler

        if (channelType != ChannelType.PRIVATE) {
            message.delete().queue()
        }

        if (messageContent.equals("cancel", true))
            return selectorCache.deleteMessagesAndRemove(user, 0)

        val selectedId = messageContent.toIntOrNull()?.minus(1)
        if (selectedId == null || selectedId > infoList.size)
            return sendInvalidIdResponse(user, docMessage)

        var docResponse = DocResponse()
        docResponse.setAuthor(jenkinsHandler.responseTitle, null, jenkinsHandler.iconUrl)
        docResponse = jenkinsHandler.createDocResponse(docResponse, infoList[selectedId])

        docMessage.editMessage(docResponse.asMessage(user)).queue()
        selectorCache.deleteMessagesAndRemove(user)
    }

    /**
     * Edits the given [Message] to replace it with an invalid selection error response
     *
     * @param user The [User] who sent the id
     * @param docMessage The [Message] that contains the selection menu
     */
    private fun sendInvalidIdResponse(user: User, docMessage: Message) {
        val invalidIdResponse = StandardErrorResponse(user.name, "You entered an invalid selection id!")

        docMessage.editMessage(invalidIdResponse.asMessage(user)).queue()
        selectorCache.deleteMessagesAndRemove(user, deleteSeconds)
    }

}