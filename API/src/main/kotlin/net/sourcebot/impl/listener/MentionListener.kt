package net.sourcebot.impl.listener

import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.sourcebot.Source
import net.sourcebot.api.command.argument.Arguments
import net.sourcebot.api.event.EventSubscriber
import net.sourcebot.api.event.EventSystem
import net.sourcebot.api.event.SourceEvent
import net.sourcebot.api.response.EmptyResponse
import net.sourcebot.impl.BaseModule

class MentionListener : EventSubscriber<BaseModule> {
    override fun subscribe(
        module: BaseModule,
        jdaEvents: EventSystem<GenericEvent>,
        sourceEvents: EventSystem<SourceEvent>
    ) {
        jdaEvents.listen(module, this::onMention)
    }

    private fun onMention(event: MessageReceivedEvent) {
        val self = event.jda.selfUser
        val message = event.message
        if (!message.mentionedUsers.contains(self)) return
        val content = message.contentRaw
        val pattern = Regex("^<@!?${self.id}> ?")
        if (!pattern.matches(content)) return
        val match = pattern.matchEntire(content)!!.groupValues[0]
        val arguments = Arguments.parse(content.substring(match.length))
        val (command, response) = Source.COMMAND_HANDLER.runCommand(event.message, "help", arguments)
        if (command != null && response !is EmptyResponse)
            Source.COMMAND_HANDLER.respond(command, message, response)
    }
}