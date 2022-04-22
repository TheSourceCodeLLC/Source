package net.sourcebot.module.freegames

import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.result.Result
import com.google.gson.JsonParser
import net.dv8tion.jda.api.MessageBuilder
import net.dv8tion.jda.api.entities.Guild
import net.sourcebot.Source
import net.sourcebot.module.freegames.`object`.Game
import org.bson.Document
import org.jsoup.Jsoup
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

class FreeGameHandler(private val guild: Guild) {
    private val configManager = Source.CONFIG_MANAGER
    private val mongo = Source.MONGODB
    private val freeGamesCollection = mongo.getCollection(guild.id, "free-game-log")

    private val epicgamesLogo =
        "https://cdn2.unrealengine.com/Unreal+Engine%2Feg-logo-filled-1255x1272-0eb9d144a0f981d1cbaaa1eb957de7a3207b31bb.png"
    private val steamLogo =
        "https://upload.wikimedia.org/wikipedia/commons/thumb/8/83/Steam_icon_logo.svg/2048px-Steam_icon_logo.svg.png"

    private fun getFreeGamesChannel() = configManager[guild].optional<String>("free-games.channel")
        ?.let { guild.getTextChannelById(it) }

    private fun getFreeGamesRole() = configManager[guild].optional<String>("free-games.role")
        ?.let { guild.getRoleById(it) }

    private fun isSteamEnabled() = configManager[guild].optional<Boolean>("free-games.services.steam")

    private fun isEpicGamesEnabled() = configManager[guild].optional<Boolean>("free-games.services.epic-games")

    // Returns -1 if this function fails, 0 if there are no games to update, 1 if games are updated
    fun refreshGames(): Int {
        val combinedArray = mutableListOf<Game>()

        isSteamEnabled()?.let {
            combinedArray.addAll(retrieveFreeSteamGames() ?: return -1)
        }

        isEpicGamesEnabled()?.let {
            combinedArray.addAll(retrieveFreeEpicGames() ?: return -1)
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

    private fun retrieveFreeSteamGames(): List<Game>? {
        // Use this url when testing https://store.steampowered.com/search/?maxprice=5&specials=1
        val freeGameDocument = Jsoup.connect("https://store.steampowered.com/search/?maxprice=free&specials=1")
            .ignoreContentType(true)
            .maxBodySize(0)
            .get()

        val resultListElement = freeGameDocument.getElementById("search_resultsRows") ?: return null
        val urlList = resultListElement.select("a")
            .filter { it.hasAttr("data-ds-appid") }
            .map { "https://store.steampowered.com/app/${it.attr("data-ds-appid")}/" }


        val list = mutableListOf<Game>()
        //Remove comment around drop for when testing
        urlList/*.drop(urlList.size - 2)*/.parallelStream()
            .filter { freeGamesCollection.find(Document("url", it.toLowerCase())).first() == null }
            .forEach {
                val gameDocument = Jsoup.connect(it)
                    .ignoreContentType(true)
                    .maxBodySize(0)
                    .get()


                val title = gameDocument.getElementById("appHubAppName")?.text() ?: return@forEach
                val imageUrl = gameDocument.selectFirst("img.game_header_image_full")?.attr("src") ?: steamLogo

                // I do not know of way to get the exact time the promotion ends, so this will have to suffice
                val estimatedEndStr =
                    (gameDocument.selectFirst("p.game_purchase_discount_countdown")?.text() ?: "Unknown")
                        .substringAfterLast("ends")
                        .trim()

                val estimatedDateStr = if (estimatedEndStr.contains(",")) estimatedEndStr
                else "$estimatedEndStr, ${Calendar.getInstance().get(Calendar.YEAR)}"

                val estimatedEpoch = LocalDate.parse(estimatedDateStr, DateTimeFormatter.ofPattern("MMMM d, yyyy"))
                    .atTime(23, 59, 59)
                    .atZone(ZoneId.systemDefault())
                    .toEpochSecond()

                list.add(Game(title, it, steamLogo, imageUrl, estimatedEpoch))
            }

        return list

    }

    private fun retrieveFreeEpicGames(): List<Game>? {
        val (_, _, egResult) = "https://store-site-backend-static-ipv4.ak.epicgames.com/freeGamesPromotions?locale=en-US"
            .httpGet()
            .responseString()

        if (egResult is Result.Failure) return null

        return JsonParser.parseString(egResult.get())
            .asJsonObject["data"]
            .asJsonObject["Catalog"]
            .asJsonObject["searchStore"]
            .asJsonObject["elements"]
            .asJsonArray
            .map { it.asJsonObject }
            .filter {
                val totalPriceObj = it["price"].asJsonObject["totalPrice"].asJsonObject
                val discountPrice = totalPriceObj["discountPrice"].asInt
                val originalPrice = totalPriceObj["originalPrice"].asInt
                return@filter discountPrice == 0 && originalPrice > 0
            }.map {
                val title = it["title"].asString
                val urlSlug = it["catalogNs"].asJsonObject["mappings"].asJsonArray[0].asJsonObject["pageSlug"].asString
                val url = "https://store.epicgames.com/en-US/p/$urlSlug/"
                val imageUrl = it["keyImages"].asJsonArray.find { imageElement ->
                    val imageType = imageElement.asJsonObject["type"].asString
                    return@find imageType.equals("OfferImageWide", true) || imageType.equals(
                        "DieselStoreFrontWide",
                        true
                    )
                }?.asJsonObject?.get("url")?.asString ?: epicgamesLogo
                val expirationEpoch = Instant.parse(
                    it["promotions"]
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