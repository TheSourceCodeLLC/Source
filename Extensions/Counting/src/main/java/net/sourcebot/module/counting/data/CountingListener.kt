package net.sourcebot.module.counting.data

import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.TextChannel
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent
import net.dv8tion.jda.api.events.message.guild.GuildMessageUpdateEvent
import net.sourcebot.Source
import net.sourcebot.api.module.SourceModule
import net.sourcebot.module.counting.data.CountingDataController.CountingData

class CountingListener(
    private val source: Source,
    private val dataController: CountingDataController
) {
    fun listen(
        module: SourceModule
    ) {
        source.jdaEventSystem.listen(module, this::onReceive)
        source.jdaEventSystem.listen(module, this::onEdit)
    }

    private val lastMessages = HashMap<String, CountingMessage>()
    private val records = HashMap<String, Long>()

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
            lastMessages[channel.id] = restart(channel, data)
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
            lastMessages[channel.id] = restart(channel, data)
            return
        }
        if (nextNumber == null) {
            next.delete().queue()
            channel.sendMessage(
                "Sorry, ${next.author.asMention}, messages may only be numbers!"
            ).queue()
            lastMessages[channel.id] = restart(channel, data)
            return
        }
        if (nextNumber != lastNumber + 1) {
            channel.sendMessage(
                "${next.author.asMention} is bad at counting."
            ).queue()
            lastMessages[channel.id] = restart(channel, data)
            return
        }
        lastMessages[channel.id] = CountingMessage(next)
        records[channel.id] = nextNumber
    }

    private fun onEdit(event: GuildMessageUpdateEvent) {
        val data = dataController.getData(event.guild)
        val channel = data.channel?.let {
            event.guild.getTextChannelById(it)
        } ?: return
        if (event.channel != channel) return
        channel.sendMessage(
            "${event.author.asMention}, editing messages is not allowed!"
        ).queue()
        lastMessages[channel.id] = restart(channel, data)
    }

    private fun checkRecord(
        channel: TextChannel,
        data: CountingData
    ): Boolean {
        val current = records[channel.id] ?: 1
        return if (current > data.record) {
            channel.sendMessage(
                "New Record! New: $current. Old: ${data.record}."
            ).queue()
            data.record = current
            true
        } else false
    }

    private fun restart(
        channel: TextChannel,
        data: CountingData
    ): CountingMessage {
        if (checkRecord(channel, data)) dataController.save()
        channel.sendMessage(
            "Restarting... Current record: ${data.record}"
        ).queue()
        return channel.sendMessage("1").complete().let(::CountingMessage)
    }
}

class CountingMessage(message: Message) {
    val number = message.contentRaw.toLong()
    val author = message.author.id
}