package net.sourcebot.api.event

import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import java.util.regex.Pattern

/**
 * This class is responsible for processing command-like messages
 * This class extracts command information and calls the subtype [cascade] implementation
 *
 * @param[prefix] The prefix to use for this [AbstractMessageHandler]
 *
 * @author Hunter Wignall
 * @version April 25, 2020
 */
abstract class AbstractMessageHandler constructor(private val prefix: String) {
    private val ARGS_PATTERN = Pattern.compile("(\".*?\"|\\S+)")
    fun onMessageReceived(event: MessageReceivedEvent) {
        val message = event.message
        var content = message.contentRaw
        if (!content.startsWith(prefix)) return
        content = content.substring(prefix.length)
        if (content.isBlank()) return
        val args = ARGS_PATTERN.matcher(content).results()
            .map {
                it.group().replace(Regex("\"(.+)\""), "$1")
            }.toArray<String> { arrayOfNulls(it) }
        val label = args[0].toLowerCase()
        cascade(message, label, args.copyOfRange(1, args.size))
    }

    protected abstract fun cascade(message: Message, label: String, args: Array<String>)
}