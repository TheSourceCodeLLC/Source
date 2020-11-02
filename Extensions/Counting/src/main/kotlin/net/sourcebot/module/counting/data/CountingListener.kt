package net.sourcebot.module.counting.data

import com.fasterxml.jackson.annotation.JsonCreator
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.TextChannel
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent
import net.dv8tion.jda.api.events.message.guild.GuildMessageUpdateEvent
import net.sourcebot.Source
import net.sourcebot.api.configuration.JsonConfiguration
import net.sourcebot.api.event.EventSubscriber
import net.sourcebot.api.event.EventSystem
import net.sourcebot.api.event.SourceEvent
import net.sourcebot.api.ifPresentOrElse
import net.sourcebot.module.counting.Counting
import java.util.concurrent.TimeUnit

class CountingListener : EventSubscriber<Counting> {
    private val commandHandler = Source.COMMAND_HANDLER
    private val configurationManager = Source.CONFIG_MANAGER
    override fun subscribe(
        module: Counting,
        jdaEvents: EventSystem<GenericEvent>,
        sourceEvents: EventSystem<SourceEvent>
    ) {
        jdaEvents.listen(module, this::onReceive)
        jdaEvents.listen(module, this::onEdit)
    }

    private val checkpoints = HashMap<String, Long>()
    private val lastMessages = HashMap<String, CountingMessage>()
    private val records = HashMap<String, Long>()

    private fun onReceive(event: GuildMessageReceivedEvent) {
        if (event.author.isBot) return
        val config = configurationManager[event.guild]
        val counting = config.optional<JsonConfiguration>("counting") ?: return
        val channel = counting.optional<String>("channel")?.let(
            event.guild::getTextChannelById
        ) ?: return
        if (event.channel != channel) return
        val message = event.message
        if (commandHandler.isValidCommand(message.contentRaw) == false) {
            message.delete().queue()
            return restart(
                "Sorry, ${message.author.asMention}, that was not a valid command!", message, counting
            )
        } else if (commandHandler.isValidCommand(message.contentRaw) == true) return
        var lastMessage = lastMessages[channel.id]
        if (lastMessage == null) {
            var unknown = false
            lastMessage = counting.required("lastMessage") {
                unknown = true
                message.delete().queue()
                channel.sendMessage("Could not determine last number!\n1").queue()
                CountingMessage(1, event.jda.selfUser.id)
            }.also { lastMessages[channel.id] = it }
            if (unknown) return
        }
        val lastNumber = lastMessage.number
        val input = message.contentRaw
        if (message.author.id == lastMessage.author) {
            message.delete().queue()
            return restart(
                "Sorry, ${message.author.asMention}, you may not count twice in a row!", message, counting
            )
        }
        if (input.startsWith("0")) {
            message.delete().queue()
            return restart(
                "No funny business, ${message.author.asMention}.", message, counting
            )
        }
        val nextNumber = input.toLongOrNull()
        if (nextNumber == null) {
            message.delete().queue()
            return restart(
                "Sorry, ${message.author.asMention}, messages may only be numbers!", message, counting
            )
        }
        if (nextNumber != lastNumber + 1) return restart(
            "${message.author.asMention} is bad at counting.", message, counting
        )
        CountingMessage(message).also {
            lastMessages[channel.id] = it
            counting["lastMessage"] = it
        }
        records[channel.id] = nextNumber
        configurationManager[channel.guild].let {
            it["counting"] = counting
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
        var record = data.required<Long>("record") { 1 }
        if (current > record) {
            toSend += "\nNew Record! New: $current. Old: $record."
            record = data.set("record", current)
            channel.manager.setTopic("Record: $current").queue()
        }
        toSend += "\nCurrent record: $record\n"
        val checkpoint: Long = checkpoints.remove(channel.id).ifPresentOrElse(
            { toSend += "Resuming from checkpoint!\n$it" },
            { toSend += "Restarting...\n1"; 1 }
        )
        channel.sendMessage(toSend).complete()

        CountingMessage(checkpoint, message.jda.selfUser.id).also {
            lastMessages[channel.id] = it
            data["lastMessage"] = it
        }
        val config = configurationManager[channel.guild].also { it["counting"] = data }
        configurationManager.saveData(channel.guild, config)
    }

    init {
        Source.SCHEDULED_EXECUTOR_SERVICE.scheduleAtFixedRate({
            Source.SHARD_MANAGER.guilds.forEach {
                val countingChannel = Counting.getCountingChannel(it) ?: return@forEach
                val lastMessage = lastMessages[countingChannel.id] ?: return@forEach
                val lastNumber = lastMessage.number
                checkpoints.compute(countingChannel.id) { _, stored ->
                    if (stored == null || lastNumber > stored) {
                        countingChannel.sendMessage("Checkpoint: $lastNumber").complete()
                        lastNumber
                    } else stored
                }
            }
        }, 0L, 10L, TimeUnit.MINUTES)
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