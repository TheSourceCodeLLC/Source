package net.sourcebot.api.event

import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.sourcebot.api.command.argument.Arguments

/**
 * This class is responsible for processing command-like messages
 * This class extracts command information and calls the subtype [cascade] implementation
 *
 * @author Hunter Wignall
 * @version September 23, 2020
 */
abstract class AbstractMessageHandler {
    fun onMessageReceived(event: MessageReceivedEvent) {
        val message = event.message
        var content = message.contentRaw
        val prefixes = getViablePrefixes(event)
        var matched = false
        for (prefix in prefixes) {
            val (match) = Regex("^(${Regex.escape(prefix)}).*$").matchEntire(content)?.destructured ?: continue
            matched = true
            content = content.substring(match.length)
            break
        }
        if (!matched) return
        if (content.isBlank()) return
        val args = Arguments.parse(content)
        val label = args.next()?.toLowerCase() ?: return
        cascade(message, label, args)
    }

    protected abstract fun cascade(message: Message, label: String, arguments: Arguments)
    protected abstract fun getViablePrefixes(event: MessageReceivedEvent): List<String>
    abstract fun getPrefix(guild: Guild): String
}