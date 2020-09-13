package net.sourcebot.module.documentation.utility

import net.dv8tion.jda.api.entities.ChannelType
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.User
import java.util.concurrent.TimeUnit

/**
 * This is the cache for [SelectorModel]s
 *
 * @property docSelector The map which contains all available [SelectorModel] which are mapped to [User]s
 */
class SelectorCache {

    private val docSelector: MutableMap<User, SelectorModel> = mutableMapOf()

    /**
     * Adds a [SelectorModel] to the cache
     *
     * @param user The user who opened the [SelectorModel]
     * @param selector The [SelectorModel] opened by the [User]
     */
    fun addSelector(user: User, selector: SelectorModel) {
        docSelector[user] = selector
    }

    /**
     * Updates the message in a [SelectorModel]
     *
     * @param user The [User] who created the selector
     * @param message The new [Message] to insert into the [SelectorModel] if found
     */
    fun updateSelector(user: User, message: Message) {
        if (message.embeds.size == 0) return
        val selector = getSelector(user) ?: return

        val newMessageEmbed = message.embeds[0]
        val selectTitle = "Type the id of the option you would like to select in chat:"

        if (!newMessageEmbed.title.equals(selectTitle, true)) return

        selector.docMessage = message
        docSelector[user] = selector
    }

    /**
     * Checks if a [User] has a [SelectorModel] open currently
     *
     * @param user The [User] who created the [SelectorModel]
     * @return true or false depending on whether the [User] has a [SelectorModel] open currently
     */
    fun hasSelector(user: User): Boolean {
        return docSelector.containsKey(user)
    }

    /**
     * Removes a [User] from the [docSelector] map
     *
     * @param user The [User] being removed
     */
    fun removeSelector(user: User) {
        docSelector.remove(user)
    }

    /**
     * Deletes the [SelectorModel.docMessage] and the [SelectorModel.cmdMessage]
     *
     * @param user The [User] who opened the [SelectorModel]
     * @param deleteAfter The amount of time, in seconds, to delete the message after
     */
    fun deleteMessages(user: User, deleteAfter: Long = 0) {
        val selector = getSelector(user) ?: return
        val channelType = selector.cmdMessage.channelType

        selector.docMessage?.delete()?.queueAfter(deleteAfter, TimeUnit.SECONDS)

        if (channelType != ChannelType.PRIVATE) {
            selector.cmdMessage.delete().queueAfter(deleteAfter, TimeUnit.SECONDS)
        }

    }

    /**
     * Deletes the [SelectorModel.docMessage] and the [SelectorModel.cmdMessage], and then removes the selector
     * from the cache
     *
     * @param user The [User] who opened the [SelectorModel]
     * @param deleteAfter The amount of time, in seconds, to delete the message after
     */
    fun deleteMessagesAndRemove(user: User, deleteAfter: Long = 0) {
        deleteMessages(user, deleteAfter)
        removeSelector(user)
    }

    /**
     * Gets the [SelectorModel] for a specific [User]
     *
     * @param user The [User] to get the [SelectorModel] from
     * @return The found [SelectorModel] or null
     */
    fun getSelector(user: User): SelectorModel? {
        return docSelector[user]
    }

}