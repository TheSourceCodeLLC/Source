package net.sourcebot.api.event

import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.sourcebot.api.command.argument.Arguments

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
    protected val guildPrefixSupplier: (Guild) -> String = { defaultPrefix }
) {
    fun onMessageReceived(event: MessageReceivedEvent, checkMention: Boolean = false) {
        val message = event.message
        var content = message.contentRaw
        val prefix = if (message.isFromGuild) guildPrefixSupplier(message.guild) else defaultPrefix
        content = if (!content.startsWith(prefix)) {
            if (checkMention) {
                val id = message.jda.selfUser.id
                val mention = "<@!$id> "
                if (!content.startsWith(mention)) return
                content.substring(mention.length)
            } else return
        } else content.substring(prefix.length)
        if (content.isBlank()) return
        val args = Arguments.parse(content)
        val label = args.next()?.toLowerCase() ?: return
        cascade(message, label, args)
    }

    protected abstract fun cascade(message: Message, label: String, arguments: Arguments)
}