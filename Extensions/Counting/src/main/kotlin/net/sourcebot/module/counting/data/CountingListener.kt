package net.sourcebot.module.counting.data

import com.fasterxml.jackson.annotation.JsonCreator
import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.cache.LoadingCache
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.TextChannel
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.message.guild.GenericGuildMessageEvent
import net.dv8tion.jda.api.events.message.guild.GuildMessageDeleteEvent
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent
import net.dv8tion.jda.api.events.message.guild.GuildMessageUpdateEvent
import net.sourcebot.Source
import net.sourcebot.api.asMessage
import net.sourcebot.api.configuration.JsonConfiguration
import net.sourcebot.api.event.EventSubscriber
import net.sourcebot.api.event.EventSystem
import net.sourcebot.api.event.SourceEvent
import net.sourcebot.api.ifPresentOrElse
import net.sourcebot.api.response.StandardWarningResponse
import net.sourcebot.module.counting.Counting
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

class CountingListener : EventSubscriber<Counting> {
    private val configurationManager = Source.CONFIG_MANAGER
    override fun subscribe(
        module: Counting,
        jdaEvents: EventSystem<GenericEvent>,
        sourceEvents: EventSystem<SourceEvent>
    ) {
        jdaEvents.listen(module, this::onReceive)
        jdaEvents.listen(module, this::onDelete)
        jdaEvents.listen(module, this::onEdit)
    }

    private val checkpoints = HashMap<String, Long>()
    private val lastMessages = HashMap<String, CountingMessage>()
    private val records = HashMap<String, Long>()
    private val recentDeletes = HashSet<String>()
    private fun onReceive(event: GuildMessageReceivedEvent) {
        if (event.author.isBot) return
        val counting = getCountingData(event.guild) ?: return
        val channel = counting.optional<String>("channel")?.let(
            event.guild::getTextChannelById
        ) ?: return
        if (event.channel != channel) return
        val message = event.message
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
            recentDeletes += channel.id
            message.delete().queue()
            return restart(
                "Sorry, ${message.author.asMention}, you may not count twice in a row!",
                channel,
                counting,
                message.author.id
            )
        }
        if (input.startsWith("0")) {
            recentDeletes += channel.id
            message.delete().queue()
            return restart(
                "No funny business, ${message.author.asMention}.", channel, counting, message.author.id
            )
        }
        val nextNumber = input.split(Regex("\\D+"))[0].toLongOrNull()
        if (nextNumber == null) {
            recentDeletes += channel.id
            message.delete().queue()
            return restart(
                "Sorry, ${message.author.asMention}, messages must begin with a number!",
                channel,
                counting,
                message.author.id
            )
        }
        if (nextNumber != lastNumber + 1) return restart(
            "${message.author.asMention} is bad at counting.", channel, counting, message.author.id
        )
        CountingMessage(nextNumber, message.author.id).also {
            lastMessages[channel.id] = it
            counting["lastMessage"] = it
        }
        records[channel.id] = nextNumber
        configurationManager[channel.guild].let {
            it["counting"] = counting
            configurationManager.saveData(channel.guild, it)
        }
    }

    private fun onInvalidEvent(
        event: GenericGuildMessageEvent, message: String, blame: String? = null
    ) {
        val data = getCountingData(event.guild) ?: return
        val channel = data.optional<String>("channel")?.let {
            event.guild.getTextChannelById(it)
        } ?: return
        if (event.channel != channel) return
        if (recentDeletes.remove(channel.id)) return
        return restart(message, event.channel, data, blame)
    }

    private fun onEdit(event: GuildMessageUpdateEvent) = onInvalidEvent(
        event, "${event.author.asMention}, editing messages is not allowed!", event.author.id
    )

    private fun onDelete(event: GuildMessageDeleteEvent) = onInvalidEvent(
        event, "Someone deleted a message!"
    )

    private val violations = HashMap<String, LoadingCache<String, Int>>()
    private fun restart(
        failMessage: String,
        channel: TextChannel,
        data: JsonConfiguration,
        blame: String? = null
    ) {
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
            { toSend += "Resuming from checkpoint!\n$it"; it },
            { toSend += "Restarting...\n1"; 1 }
        )
        channel.sendMessage(toSend).complete()

        CountingMessage(checkpoint, channel.jda.selfUser.id).also {
            lastMessages[channel.id] = it
            data["lastMessage"] = it
        }
        val config = configurationManager[channel.guild].also { it["counting"] = data }
        configurationManager.saveData(channel.guild, config)
        if (blame != null) {
            val muteRole = getMuteRole(channel.guild) ?: return
            val channelViolations = violations.computeIfAbsent(channel.id) {
                CacheBuilder.newBuilder().expireAfterWrite(5, TimeUnit.MINUTES)
                    .build(object : CacheLoader<String, Int>() {
                        override fun load(key: String) = 0
                    })
            }
            val violationLevel = channelViolations[blame] + 1
            channelViolations.put(blame, violationLevel)
            if (violationLevel < 3) return
            channel.guild.addRoleToMember(blame, muteRole).queue {
                val member = channel.guild.getMemberById(blame) ?: return@queue
                val embed = StandardWarningResponse(
                    "Incapable Of Counting!",
                    "Role given to ${member.asMention} due to 3 failures over the past 5 minutes!"
                )
                channel.sendMessage(embed.asMessage(member)).queue()
            }
        }
    }

    internal fun handleCheckpoints(): ScheduledFuture<*> =
        Source.SCHEDULED_EXECUTOR_SERVICE.scheduleAtFixedRate({
            Source.EXECUTOR_SERVICE.submit {
                Source.SHARD_MANAGER.guilds.forEach { guild ->
                    val counting = getCountingData(guild) ?: return@forEach
                    val channel = counting.optional<String>("channel")?.let(
                        guild::getTextChannelById
                    ) ?: return@forEach
                    lastMessages[channel.id]?.let { lastMessage ->
                        val lastNumber = lastMessage.number
                        checkpoints.compute(channel.id) { _, stored ->
                            if (stored == null || lastNumber >= stored + 10) {
                                counting.required("checkpoint") { lastNumber }.also {
                                    channel.sendMessage("Checkpoint: $it").complete()
                                }
                            } else stored
                        }
                    }
                }
            }
        }, 0L, 10L, TimeUnit.MINUTES)

    private fun getCountingData(guild: Guild) =
        configurationManager[guild].optional<JsonConfiguration>("counting")

    private fun getMuteRole(guild: Guild) =
        configurationManager[guild].optional<String>("counting.mute-role")?.let(guild::getRoleById)
}

class CountingMessage @JsonCreator constructor(val number: Long, val author: String)