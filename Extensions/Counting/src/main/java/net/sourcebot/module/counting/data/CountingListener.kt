package net.sourcebot.module.counting.data

import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.TextChannel
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent
import net.sourcebot.Source
import net.sourcebot.api.module.SourceModule

class CountingListener(
    private val source: Source,
    private val dataController: CountingDataController
) {
    fun listen(
        module: SourceModule
    ) = source.jdaEventSystem.listen(module, this::onReceive)

    private val lastMessages = HashMap<String, CountingMessage>()

    private fun onReceive(event: GuildMessageReceivedEvent) {
        if (event.author.isBot) return
        if (event.message.contentRaw.startsWith(source.commandHandler.prefix)) return
        val data = dataController.getData(event.guild)
        val channel = data.channel?.let {
            event.guild.getTextChannelById(it)
        } ?: return
        if (event.channel != channel) return
        val last = lastMessages[channel.id]
        if (last == null) {
            event.message.delete().queue()
            lastMessages[channel.id] = restart(channel, data.record)
            return
        }
        val lastNumber = last.number
        val next = event.message
        val nextNumber = event.message.contentRaw.toLongOrNull()
        if (next.author.id == last.author) {
            next.delete().queue()
            channel.sendMessage(
                "Sorry, ${next.author.asMention}, you may not count twice in a row!"
            ).queue()
            lastMessages[channel.id] = restart(channel, data.record)
            return
        }
        if (nextNumber == null) {
            next.delete().queue()
            channel.sendMessage(
                "Sorry, ${next.author.asMention}, messages may only be numbers!"
            ).queue()
            lastMessages[channel.id] = restart(channel, data.record)
            return
        }
        if (nextNumber != lastNumber + 1) {
            next.delete().queue()
            channel.sendMessage(
                "${next.author.asMention} is bad at counting."
            ).queue()
            lastMessages[channel.id] = restart(channel, data.record)
            return
        }
        lastMessages[channel.id] = CountingMessage(next)
    }

    private fun checkRecord(channel: TextChannel) {

    }

    private fun restart(channel: TextChannel, record: Long): CountingMessage {
        channel.sendMessage(
            "Restarting... Current record: $record"
        ).queue()
        return channel.sendMessage("1").complete().let(::CountingMessage)
    }
}

class CountingMessage(message: Message) {
    val number = message.contentRaw.toLong()
    val author = message.author.id
}