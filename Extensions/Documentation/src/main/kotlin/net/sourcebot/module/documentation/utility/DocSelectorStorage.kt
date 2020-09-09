package net.sourcebot.module.documentation.utility

import me.theforbiddenai.jenkinsparserkotlin.entities.Information
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.User
import net.sourcebot.module.documentation.dochandlers.JenkinsHandler

internal data class DocSelectorStorage(
    var message: Message?,
    var cmdMessage: Message,
    val infoList: List<Information>,
    val jenkinsHandler: JenkinsHandler
) {

    init {
        if (infoList.isEmpty()) {
            throw Error("Info list can not be empty!")
        }
    }

    companion object {
        private val docSelector: MutableMap<User, DocSelectorStorage> = mutableMapOf()

        fun addSelector(user: User, selector: DocSelectorStorage) {
            docSelector[user] = selector
        }

        fun updateSelector(message: Message) {
            if (message.embeds.size == 1) {
                val responseEmbed = message.embeds[0]

                if (responseEmbed.title.equals("Type the id of the option you would like to select in chat:")) {

                    var userTag = responseEmbed.footer?.text ?: return
                    userTag = userTag.replace("Ran By:", "").trim()

                    val user = message.jda.getUserByTag(userTag) ?: return

                    if (hasSelector(user)) {
                        val selector = getSelector(user)!!

                        selector.message = message
                        docSelector[user] = selector
                    }
                }
            }

        }

        fun hasSelector(user: User): Boolean {
            return docSelector.containsKey(user)
        }

        fun removeSelector(user: User) {
            docSelector.remove(user)
        }

        fun removeAndDeleteSelector(user: User) {
            if (!hasSelector(user)) return
            val selector = getSelector(user)!!
            selector.message?.delete()?.queue()

            try {
                selector.cmdMessage.delete().queue()
            } catch (ex: Exception) {

            }
        }

        fun getSelector(user: User): DocSelectorStorage? {
            return docSelector[user]
        }
    }

}