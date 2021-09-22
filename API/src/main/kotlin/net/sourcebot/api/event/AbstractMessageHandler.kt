package net.sourcebot.api.event

import me.hwiggy.kommander.arguments.Arguments
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter

/**
 * This class is responsible for processing command-like messages
 * This class extracts command information and calls the subtype [cascade] implementation
 *
 * @author Hunter Wignall
 * @version September 23, 2020
 */
abstract class AbstractMessageHandler : ListenerAdapter() {
    override fun onMessageReceived(event: MessageReceivedEvent) {
        if (event.author.isBot) return
        val message = event.message
        var content = message.contentRaw
        val prefix = getPrefix(event)
        if (!content.startsWith(prefix)) return
        content = content.substring(prefix.length)
        if (content.isBlank()) return
        val args = Arguments.parse(content)
        val label = args.next()?.toLowerCase() ?: return
        cascade(message, label, args)
    }

    protected abstract fun cascade(message: Message, label: String, arguments: Arguments)
    protected abstract fun getPrefix(event: MessageReceivedEvent): String
}