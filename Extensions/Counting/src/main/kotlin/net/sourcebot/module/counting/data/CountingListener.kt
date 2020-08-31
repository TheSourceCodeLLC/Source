package net.sourcebot.module.counting.data

import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.TextChannel
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent
import net.dv8tion.jda.api.events.message.guild.GuildMessageUpdateEvent
import net.sourcebot.Source
import net.sourcebot.api.configuration.GuildConfigurationManager
import net.sourcebot.api.module.SourceModule

class CountingListener(
    private val source: Source,
    private val configurationManager: GuildConfigurationManager
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
        val guildData = configurationManager[event.guild]
        val data: CountingData = guildData.required("counting") { guildData.set("counting", CountingData()) }
        val channel = data.channel?.let {
            event.guild.getTextChannelById(it)
        } ?: return
        if (event.channel != channel) return
        val message = event.message
        when (source.commandHandler.isValidCommand(message.contentRaw)) {
            false -> {
                event.message.delete().queue()
                return restart(
                    "Sorry, ${message.author.asMention}, that was not a valid command!", message, data
                )
            }
            true -> return
        }
        val lastMessage = lastMessages[channel.id] ?: return restart(
            "Could not determine last number!", message, data
        )
        val lastNumber = lastMessage.number
        val nextNumber = event.message.contentRaw.toLongOrNull()
        if (message.author.id == lastMessage.author) {
            message.delete().queue()
            return restart(
                "Sorry, ${message.author.asMention}, you may not count twice in a row!", message, data
            )
        }
        if (nextNumber == null) {
            message.delete().queue()
            return restart(
                "Sorry, ${message.author.asMention}, messages may only be numbers!", message, data
            )
        }
        if (nextNumber != lastNumber + 1) return restart(
            "${message.author.asMention} is bad at counting.", message, data
        )
        val toSave = CountingMessage(message)
        lastMessages[channel.id] = toSave
        records[channel.id] = nextNumber
        configurationManager[channel.guild]["counting"] = data
    }

    private fun onEdit(event: GuildMessageUpdateEvent) {
        val data: CountingData = configurationManager[event.guild].optional("counting") ?: return
        val channel = data.channel?.let {
            event.guild.getTextChannelById(it)
        } ?: return
        if (event.channel != channel) return
        return restart(
            "${event.author.asMention}, editing messages is not allowed!", event.message, data
        )
    }

    private fun restart(
        failMessage: String,
        message: Message,
        data: CountingData
    ) {
        val channel = message.channel as TextChannel
        var toSend = failMessage
        val current = records[channel.id] ?: 1
        if (current > data.record) {
            toSend += "\nNew Record! New: $current. Old: ${data.record}."
            data.record = current
        }
        channel.sendMessage(
            toSend + "\nRestarting... Current record: ${data.record}\n1"
        ).complete()

        val lastMessage = CountingMessage(1, message.jda.selfUser.id)
        configurationManager[channel.guild]["counting"] = data
        configurationManager.saveData(channel.guild)
        lastMessages[channel.id] = lastMessage
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