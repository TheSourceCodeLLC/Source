package net.sourcebot.api.event

import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.message.MessageReceivedEvent

/**
 * This class is responsible for processing command-like messages
 * This class extracts command information and calls the subtype [cascade] implementation
 *
 * @param[defaultPrefix] The prefix to use for this [AbstractMessageHandler]
 *
 * @author Hunter Wignall
 * @version September 13, 2020
 */
abstract class AbstractMessageHandler constructor(
    private val defaultPrefix: String,
    private val guildPrefixSupplier: (Guild) -> String = { defaultPrefix }
) {
    fun onMessageReceived(event: MessageReceivedEvent) {
        val message = event.message
        val prefix = if (message.isFromGuild) guildPrefixSupplier(message.guild) else defaultPrefix
        var content = message.contentRaw
        if (!content.startsWith(prefix)) return
        content = content.substring(prefix.length)
        if (content.isBlank()) return
        val args = readArguments(content)
        val label = args[0].toLowerCase()
        cascade(message, label, args.copyOfRange(1, args.size))
    }

    protected abstract fun cascade(message: Message, label: String, args: Array<String>)

    /**
     * Reads arguments from a given input string
     * Arguments are delimited by whitespace unless they are wrapped in quotes.
     * Quotes and other characters may be escaped.
     *
     * @param[input] The input String to read arguments from
     * @return An [Array<String>] of the read arguments
     */
    private fun readArguments(input: String): Array<String> {
        val out = mutableListOf<String>()
        var current = String()
        var shouldEscape = false
        var insideQuotes = false
        for (it in input.toCharArray()) {
            if (shouldEscape) {
                current += it; shouldEscape = false; continue
            }
            if (it == '\\') {
                shouldEscape = true; continue
            }
            if (it == '\"') {
                if (insideQuotes) {
                    insideQuotes = false
                    out += current
                    current = String()
                    continue
                } else {
                    insideQuotes = true; continue
                }
            }
            if (it.isWhitespace() && !insideQuotes) {
                if (current.isNotEmpty()) {
                    out += current
                    current = String()
                }
                continue
            }
            current += it
        }
        if (current.isNotEmpty()) out += current
        return out.toTypedArray()
    }
}