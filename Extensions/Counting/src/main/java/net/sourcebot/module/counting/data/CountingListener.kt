package net.sourcebot.module.counting.data

import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.TextChannel
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent
import net.dv8tion.jda.api.events.message.guild.GuildMessageUpdateEvent
import net.sourcebot.Source
import net.sourcebot.api.data.GuildDataManager
import net.sourcebot.api.module.SourceModule

class CountingListener(
    private val source: Source,
    private val dataManager: GuildDataManager
) {
    fun listen(
        module: SourceModule
    ) {
        source.jdaEventSystem.listen(module, this::onReceive)
        source.jdaEventSystem.listen(module, this::onEdit)
    }

    private val lastMessages = HashMap<String, CountingMessage>()
    private val records = HashMap<String, Long>()
    private var listening = true

    private fun onReceive(event: GuildMessageReceivedEvent) {
        if (!listening) return
        if (event.author.isBot) return
        if (event.message.contentRaw.startsWith(source.commandHandler.prefix)) return
        val guildData = dataManager[event.guild]
        val data: CountingData = guildData.optional("counting") ?: return
        val channel = data.channel?.let {
            event.guild.getTextChannelById(it)
        } ?: return
        if (event.channel != channel) return
        val last = lastMessages[channel.id]
        if (last == null) {
            event.message.delete().queue()
            lastMessages[channel.id] = restart(
                "Could not determine last number!",
                channel, data
            )
            return
        }
        val lastNumber = last.number
        val next = event.message
        val nextNumber = event.message.contentRaw.toLongOrNull()
        if (next.author.id == last.author) {
            next.delete().queue()
            lastMessages[channel.id] = restart(
                "Sorry, ${next.author.asMention}, you may not count twice in a row!",
                channel, data
            )
            return
        }
        if (nextNumber == null) {
            next.delete().queue()
            lastMessages[channel.id] = restart(
                "Sorry, ${next.author.asMention}, messages may only be numbers!",
                channel, data
            )
            return
        }
        if (nextNumber != lastNumber + 1) {
            lastMessages[channel.id] = restart(
                "${next.author.asMention} is bad at counting.",
                channel, data
            )
            return
        }
        lastMessages[channel.id] = CountingMessage(next)
        records[channel.id] = nextNumber
    }

    private fun onEdit(event: GuildMessageUpdateEvent) {
        val data: CountingData = dataManager[event.guild].optional("counting") ?: return
        val channel = data.channel?.let {
            event.guild.getTextChannelById(it)
        } ?: return
        if (event.channel != channel) return
        lastMessages[channel.id] = restart(
            "${event.author.asMention}, editing messages is not allowed!",
            channel, data
        )
    }

    private fun restart(
        failMessage: String,
        channel: TextChannel,
        data: CountingData
    ): CountingMessage {
        listening = false
        var toSend = failMessage
        val current = records[channel.id] ?: 1
        if (current > data.record) {
            toSend += "\nNew Record! New: $current. Old: ${data.record}."
            data.record = current
            dataManager.saveData(channel.guild)
        }
        channel.sendMessage(
            toSend + "\nRestarting... Current record: ${data.record}\n1"
        ).complete()
        return CountingMessage(1, channel.jda.selfUser.id).also { listening = true }
    }
}

class CountingMessage {
    val number: Long
    val author: String

    constructor(message: Message) {
        this.number = message.contentRaw.toLong()
        this.author = message.author.id
    }

    constructor(number: Long, author: String) {
        this.number = number
        this.author = author
    }
}