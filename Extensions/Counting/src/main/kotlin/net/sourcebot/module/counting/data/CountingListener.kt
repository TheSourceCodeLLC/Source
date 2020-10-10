package net.sourcebot.module.counting.data

import com.fasterxml.jackson.annotation.JsonCreator
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.TextChannel
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent
import net.dv8tion.jda.api.events.message.guild.GuildMessageUpdateEvent
import net.sourcebot.api.command.CommandHandler
import net.sourcebot.api.configuration.ConfigurationManager
import net.sourcebot.api.configuration.JsonConfiguration
import net.sourcebot.api.event.EventSubscriber
import net.sourcebot.api.event.EventSystem
import net.sourcebot.api.event.SourceEvent
import net.sourcebot.module.counting.Counting

class CountingListener(
    private val commandHandler: CommandHandler,
    private val configurationManager: ConfigurationManager
) : EventSubscriber<Counting> {
    override fun subscribe(
        module: Counting,
        jdaEvents: EventSystem<GenericEvent>,
        sourceEvents: EventSystem<SourceEvent>
    ) {
        jdaEvents.listen(module, this::onReceive)
        jdaEvents.listen(module, this::onEdit)
    }

    private val lastMessages = HashMap<String, CountingMessage>()
    private val records = HashMap<String, Long>()

    private fun onReceive(event: GuildMessageReceivedEvent) {
        if (event.author.isBot) return
        val guildData = configurationManager[event.guild]
        val data: JsonConfiguration = guildData.required("counting") {
            JsonConfiguration(
                mapOf(
                    "channel" to null,
                    "record" to 1
                )
            )
        }
        val channel = data.optional<String>("channel")?.let {
            event.guild.getTextChannelById(it)
        } ?: return
        if (event.channel != channel) return
        val message = event.message
        if (commandHandler.isValidCommand(message.contentRaw) == false) {
            message.delete().queue()
            return restart(
                "Sorry, ${message.author.asMention}, that was not a valid command!", message, data
            )
        } else if (commandHandler.isValidCommand(message.contentRaw) == true) return
        var lastMessage = lastMessages[channel.id]
        if (lastMessage == null) {
            var unknown = false
            lastMessage = data.required("lastMessage") {
                unknown = true
                message.delete().queue()
                channel.sendMessage("Could not determine last number!\n1").queue()
                CountingMessage(1L, event.jda.selfUser.id)
            }.also { lastMessages[channel.id] = it }
            if (unknown) return
        }
        val lastNumber = lastMessage.number
        val input = message.contentRaw
        if (message.author.id == lastMessage.author) {
            message.delete().queue()
            return restart(
                "Sorry, ${message.author.asMention}, you may not count twice in a row!", message, data
            )
        }
        if (input.startsWith("0")) {
            message.delete().queue()
            return restart(
                "No funny business, ${message.author.asMention}.", message, data
            )
        }
        val nextNumber = input.toLongOrNull()
        if (nextNumber == null) {
            message.delete().queue()
            return restart(
                "Sorry, ${message.author.asMention}, messages may only be numbers!", message, data
            )
        }
        if (nextNumber != lastNumber + 1) return restart(
            "${message.author.asMention} is bad at counting.", message, data
        )
        CountingMessage(message).also {
            lastMessages[channel.id] = it
            data["lastMessage"] = it
        }
        records[channel.id] = nextNumber
        configurationManager[channel.guild].let {
            it["counting"] = data
            configurationManager.saveData(channel.guild, it)
        }
    }

    private fun onEdit(event: GuildMessageUpdateEvent) {
        val data: JsonConfiguration = configurationManager[event.guild].optional("counting") ?: return
        val channel = data.optional<String>("channel")?.let {
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
        data: JsonConfiguration
    ) {
        val channel = message.channel as TextChannel
        var toSend = failMessage
        val current = records[channel.id] ?: 1
        var record = data.required<Long>("record")
        if (current > record) {
            toSend += "\nNew Record! New: $current. Old: $record."
            record = data.set("record", current)
        }
        channel.sendMessage(
            toSend + "\nRestarting... Current record: ${record}\n1"
        ).complete()

        CountingMessage(1, message.jda.selfUser.id).also {
            lastMessages[channel.id] = it
            data["lastMessage"] = it
        }
        val config = configurationManager[channel.guild].also { it["counting"] = data }
        configurationManager.saveData(channel.guild, config)
    }
}

class CountingMessage {
    val number: Long
    val author: String

    constructor(message: Message) {
        this.number = message.contentRaw.toLong()
        this.author = message.author.id
    }

    @JsonCreator constructor(number: Long, author: String) {
        this.number = number
        this.author = author
    }
}