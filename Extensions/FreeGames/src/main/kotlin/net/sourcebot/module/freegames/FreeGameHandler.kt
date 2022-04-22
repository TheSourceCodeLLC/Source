package net.sourcebot.module.freegames

import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.result.Result
import com.google.gson.JsonParser
import net.dv8tion.jda.api.MessageBuilder
import net.dv8tion.jda.api.entities.Guild
import net.sourcebot.Source
import net.sourcebot.module.freegames.`object`.Game
import org.bson.Document
import java.time.Instant

class FreeGameHandler(private val guild: Guild) {
    private val configManager = Source.CONFIG_MANAGER
    private val mongo = Source.MONGODB
    private val freeGamesCollection = mongo.getCollection(guild.id, "free-game-log")

    private val epicgamesAPIURL =
        "https://store-site-backend-static-ipv4.ak.epicgames.com/freeGamesPromotions?locale=en-US"

    private val epicgamesLogo =
        "https://cdn2.unrealengine.com/Unreal+Engine%2Feg-logo-filled-1255x1272-0eb9d144a0f981d1cbaaa1eb957de7a3207b31bb.png"
    private val steamLogo = "https://toppng.com/uploads/preview/steam-logo-png-transparent-11563369816grgulmbuwk.png"

    private fun getFreeGamesChannel() = configManager[guild].optional<String>("free-games.channel")
        ?.let { guild.getTextChannelById(it) }

    private fun getFreeGamesRole() = configManager[guild].optional<String>("free-games.role")
        ?.let { guild.getRoleById(it) }

    private fun isEpicGamesEnabled() = configManager[guild].optional<Boolean>("free-games.services.epic-games")

    private fun isSteamEnabled() = configManager[guild].optional<Boolean>("free-games.services.steam")

    // Returns -1 if this function fails, 0 if there are no games to update, 1 if games are updated
    fun refreshGames(): Int {
        val combinedArray = mutableListOf<Game>()

        isEpicGamesEnabled()?.let {
            val epicgamesArray = retrieveFreeEpicGames() ?: return -1
            combinedArray.addAll(epicgamesArray)
        }

        combinedArray.removeIf { freeGamesCollection.find(Document("url", it.url.toLowerCase())).first() != null }
        if (combinedArray.size == 0) return 0

        var shouldPing = true
        combinedArray.windowed(10, 10, true)
            .forEach {
                val msgBuilder = MessageBuilder()
                msgBuilder.setEmbeds(it.map { game -> game.toEmbed() })

                if (shouldPing) getFreeGamesRole()?.let { role ->
                    msgBuilder.setContent(role.asMention)
                    shouldPing = false
                }

                getFreeGamesChannel()?.let { channel ->
                    channel.sendMessage(msgBuilder.build()).queue { msg ->
                        val documentList = it.map { game ->
                            Document("url", game.url.toLowerCase())
                                .append("messageId", msg.id)
                                .append("expirationEpoch", game.expirationEpoch)
                        }
                        freeGamesCollection.insertMany(documentList)
                    }
                    return 1
                }
            }

        return -1
    }

    private fun retrieveFreeEpicGames(): List<Game>? {
        val (_, _, egResult) = epicgamesAPIURL.httpGet()
            .responseString()

        if (egResult is Result.Failure) return null

        return JsonParser.parseString(egResult.get())
            .asJsonObject["data"]
            .asJsonObject["Catalog"]
            .asJsonObject["searchStore"]
            .asJsonObject["elements"]
            .asJsonArray
            .filter {
                val obj = it.asJsonObject
                val totalPriceObj = obj["price"].asJsonObject["totalPrice"].asJsonObject
                val discountPrice = totalPriceObj["discountPrice"].asInt
                val originalPrice = totalPriceObj["originalPrice"].asInt
                return@filter discountPrice == 0 && originalPrice > 0
            }.map {
                val obj = it.asJsonObject
                val title = obj["title"].asString
                val urlSlug = obj["catalogNs"].asJsonObject["mappings"].asJsonArray[0].asJsonObject["pageSlug"].asString
                val url = "https://store.epicgames.com/en-US/p/$urlSlug"
                val imageUrl = obj["keyImages"].asJsonArray.find { imageElement ->
                    val imageType = imageElement.asJsonObject["type"].asString
                    return@find imageType.equals("OfferImageWide", true) || imageType.equals(
                        "DieselStoreFrontWide",
                        true
                    )
                }?.asJsonObject?.get("url")?.asString ?: epicgamesLogo
                val expirationEpoch = Instant.parse(
                    obj["promotions"]
                        .asJsonObject["promotionalOffers"]
                        .asJsonArray[0]
                        .asJsonObject["promotionalOffers"]
                        .asJsonArray[0]
                        .asJsonObject["endDate"]
                        .asString
                ).epochSecond
                Game(title, url, epicgamesLogo, imageUrl, expirationEpoch)
            }
    }
}