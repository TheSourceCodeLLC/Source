package net.sourcebot.module.freegames.listener

import net.dv8tion.jda.api.MessageBuilder
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.events.GenericEvent
import net.sourcebot.Source
import net.sourcebot.api.event.EventSubscriber
import net.sourcebot.api.event.EventSystem
import net.sourcebot.api.event.SourceEvent
import net.sourcebot.module.freegames.FreeGames
import net.sourcebot.module.freegames.event.FreeGameEvent
import org.bson.Document

class FreeGameListener : EventSubscriber<FreeGames> {

    private val configManager = Source.CONFIG_MANAGER
    private val mongo = Source.MONGODB

    override fun subscribe(
        module: FreeGames,
        jdaEvents: EventSystem<GenericEvent>,
        sourceEvents: EventSystem<SourceEvent>
    ) {
        sourceEvents.listen(module, this::onFreeGame)
    }

    private fun onFreeGame(event: FreeGameEvent) {
        val guild = event.guild
        val freeGamesCollection = mongo.getCollection(guild.id, "free-game-log")

        var shouldPing = true
        event.games.windowed(10, 10, true)
            .forEach {
                val msgBuilder = MessageBuilder()
                msgBuilder.setEmbeds(it.map { game -> game.toEmbed() })

                if (shouldPing) getFreeGamesRole(guild)?.let { role ->
                    msgBuilder.setContent(role.asMention)
                    shouldPing = false
                }

                getFreeGamesChannel(guild)?.let { channel ->
                    channel.sendMessage(msgBuilder.build()).queue { msg ->
                        val documentList = it.map { game ->
                            Document("url", game.url.lowercase())
                                .append("messageId", msg.id)
                                .append("expirationEpoch", game.expirationEpoch)
                        }

                        freeGamesCollection.insertMany(documentList)
                    }
                }
            }
    }

    private fun getFreeGamesChannel(guild: Guild) = configManager[guild].optional<String>("free-games.channel")
        ?.let { guild.getTextChannelById(it) }

    private fun getFreeGamesRole(guild: Guild) = configManager[guild].optional<String>("free-games.role")
        ?.let { guild.getRoleById(it) }

}