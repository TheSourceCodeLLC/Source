package net.sourcebot.api.menus

import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.message.MessageDeleteEvent
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.sourcebot.api.response.Response
import net.sourcebot.api.response.StandardInfoResponse

class MenuHandler : ListenerAdapter() {
    companion object {
        private val messageCache = HashMap<String, Message>()
        private val menus = HashMap<String, Menu<*>>()

        val options = arrayOf("1️⃣", "2️⃣", "3️⃣", "4️⃣", "5️⃣")
        private const val prevPage = "◀"
        private const val nextPage = "▶"
        private const val cancel = "⏹"

        @JvmStatic
        @JvmOverloads
        fun <T> createSelectionMenu(
            choices: List<T>,
            optsPerPage: Int = 5,
            transformer: (T) -> String,
            choiceRenderer: (T) -> Response
        ) = SelectionMenu(choices, optsPerPage, { page ->
            """
                React to choose one of the following options:
                
                ${
                options.zip(page).joinToString("\n\n") { (emoji, option) ->
                    "$emoji ${transformer(option)}"
                }
            }
            """.trimIndent()
        }, choiceRenderer)

        @JvmStatic
        @JvmOverloads
        fun createSelectionMenu(
            options: List<String>,
            optsPerPage: Int = 5,
            choiceRenderer: (String) -> Response
        ) = createSelectionMenu(options, optsPerPage, { it }, choiceRenderer)

        @JvmStatic
        @JvmOverloads
        fun <T> createSlideMenu(
            options: List<T>,
            optsPerPage: Int = 5,
            descriptor: (List<T>) -> String
        ) = SlideMenu(options, optsPerPage, descriptor)

        @JvmStatic
        fun link(message: Message, menu: Menu<*>) {
            messageCache[message.id] = message
            menus[message.id] = menu
            if (menu.hasPrev()) message.addReaction(prevPage).queue({}, {})
            repeat(menu.numOptions()) { message.addReaction(options[it]).queue({}, {}) }
            if (menu.hasNext()) message.addReaction(nextPage).queue({}, {})
            if (menu.closable()) message.addReaction(cancel).queue({}, {})
        }
    }

    override fun onMessageReactionAdd(event: MessageReactionAddEvent) {
        val reaction = event.reactionEmote
        if (!reaction.isEmoji) return
        val emoji = reaction.emoji
        val user = event.user!!
        if (user.isBot) return
        val message = messageCache[event.messageId] ?: return
        val menu = menus[message.id] ?: return
        val response = if (menu is SelectionMenu<*> && emoji in options) {
            val toSend = menu.choose(options.indexOf(emoji))
            message.channel.sendMessage(toSend.asMessage(user)).queue()
            null
        } else when (emoji) {
            cancel -> if (!menu.closable()) return else null
            prevPage -> if (!menu.hasPrev()) return else menu.previous().render()
            nextPage -> if (!menu.hasNext()) return else menu.next().render()
            else -> return
        }
        message.delete().queue {
            if (response != null) {
                message.channel.sendMessage(response.asMessage(user)).queue {
                    link(it, menu)
                }
            }
        }
    }

    override fun onMessageDelete(event: MessageDeleteEvent) {
        menus.remove(event.messageId)
        messageCache.remove(event.messageId)
    }
}

class SelectionMenu<T> internal constructor(
    options: List<T>,
    optsPerPage: Int,
    descriptor: (List<T>) -> String,
    private val choiceRenderer: (T) -> Response
) : Menu<T>(options, optsPerPage, descriptor) {
    fun choose(option: Int) = choiceRenderer(page[option])
}

class SlideMenu<T> internal constructor(
    options: List<T>,
    optsPerPage: Int,
    descriptor: (List<T>) -> String
) : Menu<T>(options, optsPerPage, descriptor) {
    override fun closable() = false
}

class MenuResponse(val menu: Menu<*>) : StandardInfoResponse()