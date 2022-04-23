package net.sourcebot.module.freegames.event

import com.github.kittinunf.fuel.core.requests.CancellableRequest
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.result.Result
import com.google.gson.JsonParser
import net.dv8tion.jda.api.entities.Guild
import net.sourcebot.Source
import net.sourcebot.api.response.StandardEmbedResponse
import net.sourcebot.api.response.StandardErrorResponse
import net.sourcebot.api.response.StandardSuccessResponse
import net.sourcebot.api.response.StandardWarningResponse
import net.sourcebot.module.freegames.data.Game
import org.bson.Document
import java.time.Instant
import java.util.concurrent.TimeUnit

class FreeGameEmitter {

    private val configManager = Source.CONFIG_MANAGER
    private val mongo = Source.MONGODB

    /**
     * Starts the executor service which emits any newly found free games to all guilds that have this feature activated
     */
    fun startEmitting() {
        Source.SCHEDULED_EXECUTOR_SERVICE.scheduleAtFixedRate({
            val guilds = Source.SHARD_MANAGER.guilds

            val steamGames = retrieveFreeSteamGames() ?: emptyList()
            val epicGames = retrieveFreeEpicGamesGames() ?: emptyList()

            guilds.parallelStream()
                .filter { getFreeGamesChannel(it) != null }
                .forEach { emitToGuild(it, steamGames, epicGames) }
        }, 0, 2, TimeUnit.HOURS)
    }

    /**
     * Calls the free EpicGames and Steam game retrieval functions and then emits the NewGameEvent.
     * This function will only emit games that have not already been posted
     *
     * @param guild The guild to emit the NewGameEvent to
     * @param callback Sends a Response object back to the initiator
     */
    fun emitToGuild(guild: Guild, callback: (response: StandardEmbedResponse) -> Unit) {
        val steamGames = retrieveFreeSteamGames()
        val epicGames = retrieveFreeEpicGamesGames()

        if (steamGames == null || epicGames == null) {
            callback.invoke(StandardErrorResponse("Uh Oh!", "Failed to refresh the free game listings!"))
            return
        }

        Source.EXECUTOR_SERVICE.submit {
            val status = emitToGuild(guild, steamGames, epicGames)
            if (status == 1) {
                callback.invoke(StandardSuccessResponse("Success!", "Successfully refreshed the free game listings"))
            } else {
                callback.invoke(
                    StandardWarningResponse(
                        "No New Updates!",
                        "There are no new free games or expired listings!"
                    )
                )
            }
        }
    }

    /**
     * Emits free EpicGames and Steam Games through the NewGameEvent. This function does NOT call the retrieval free game functions!
     * This function will only emit games that have not already been posted
     *
     * @param guild The guild to emit the NewGameEvent to
     * @param steamGames The Steam games to emit
     * @param epicGames The EpicGames games to emit
     * @return 0 if no games are emitted, 1 if games are emitted
     */
    private fun emitToGuild(guild: Guild, steamGames: List<Game>, epicGames: List<Game>): Int {
        val gameArray = mutableListOf<Game>()

        isSteamEnabled(guild)?.let {
            gameArray.addAll(steamGames)
        }
        isEpicGamesEnabled(guild)?.let {
            gameArray.addAll(epicGames)
        }

        val freeGamesCollection = mongo.getCollection(guild.id, "free-game-log")
        gameArray.removeIf {
            freeGamesCollection.find(Document("url", it.url.toLowerCase())).first() != null
        }
        if (gameArray.size == 0) return 0
        Source.SOURCE_EVENTS.fireEvent(FreeGameEvent(guild, gameArray))

        return 1
    }

    private fun retrieveFreeSteamGames(): List<Game>? {
        // Testing URL: https://store.steampowered.com/search/results/?ignore_preferences=1&maxprice=5&specials=1&json=1
        // Production URL: https://store.steampowered.com/search/results/?ignore_preferences=1&maxprice=free&specials=1&json=1
        val (_, _, result) = "https://store.steampowered.com/search/results/?ignore_preferences=1&maxprice=free&specials=1&json=1"
            .httpGet()
            .responseString()

        if (result is Result.Failure) return null

        // Key = ID, Value = Header Image URL
        val bundleMap = mutableMapOf<String, String>()
        val packageMap = mutableMapOf<String, String>()
        val appMap = mutableMapOf<String, String>()

        JsonParser.parseString(result.get())
            .asJsonObject["items"]
            .asJsonArray
            .mapNotNull {
                val obj = it.asJsonObject
                val logoUrl = obj["logo"].asString

                with(logoUrl) {
                    when {
                        contains("app") -> {
                            val id = substringAfter("apps/").substringBefore("/")
                            appMap[id] = "https://cdn.akamai.steamstatic.com/steam/apps/$id/header.jpg"
                        }
                        contains("bundles") -> {
                            val idAndHash = substringAfter("bundles/").substringBefore("/capsule")
                            val id = idAndHash.substringBefore("/")
                            bundleMap[id] = "https://cdn.akamai.steamstatic.com/steam/bundles/$idAndHash/header.jpg"
                        }
                        contains("subs") -> {
                            val id = substringAfter("subs/").substringBefore("/")
                            packageMap[id] = "https://cdn.akamai.steamstatic.com/steam/subs/$id/header.jpg"
                        }
                    }
                }

            }

        val requests = mutableListOf<CancellableRequest>()

        // TODO: ACCOUNT FOR MULTIPLE PACKAGE IDS AND SELECT FIRST ONE THAT WORKS (see witch it)
        appMap.forEach { (id, imageUrl) ->
            // These have to be a single request unfortunately because to do multiple app ids filters must be set to price_overview
            requests.add("https://store.steampowered.com/api/appdetails?appids=$id&filters=packages"
                .httpGet()
                .responseString { _, _, appResult ->
                    if (appResult is Result.Failure) return@responseString

                    val packageObj = JsonParser.parseString(appResult.get())
                        .asJsonObject[id]
                        .asJsonObject["data"]
                        .asJsonObject["packages"]
                        .asJsonArray

                    if (packageObj.size() == 0) return@responseString
                    packageMap[packageObj[0].asString] = imageUrl
                })

        }


        // key = package id, value = bundle name
        val bundleNames = mutableMapOf<String, String>()
        val csvBundleStr = bundleMap.keys.joinToString(",")
        requests.add("https://store.steampowered.com/actions/ajaxresolvebundles?bundleids=${csvBundleStr}&cc=US&l=english"
            .httpGet()
            .responseString { _, _, bundleResult ->
                if (bundleResult is Result.Failure) return@responseString
                val array = JsonParser.parseString(bundleResult.get()).asJsonArray
                array.map { it.asJsonObject }.forEach {
                    val packageObj = it["packageids"].asJsonArray
                    if (packageObj.size() == 0) return@forEach

                    val packageId = packageObj[0].asString
                    packageMap[packageId] = bundleMap[it["bundleid"].asString] ?: return@forEach
                    bundleNames[packageId] = it["name"].asString
                }
            })

        requests.forEach { it.join() }

        val gamesList = mutableListOf<Game>()
        val csvPackageStr = packageMap.keys.joinToString(",")
        "https://store.steampowered.com/actions/ajaxresolvepackages?packageids=$csvPackageStr&cc=US&l=english"
            .httpGet()
            .responseString { _, _, packageResult ->
                if (packageResult is Result.Failure) return@responseString
                val array = JsonParser.parseString(packageResult.get()).asJsonArray
                array.map { it.asJsonObject }.parallelStream().forEach {
                    val packageId = it["packageid"].asString
                    val discountExpiration = it["discount_end_rtime"].asLong
                    val imageUrl = packageMap[packageId] ?: return@forEach

                    val title = with(imageUrl) {
                        when {
                            contains("bundles") -> bundleNames[packageId] ?: return@forEach
                            else -> it["name"].asString
                        }
                    }
                    val url = with(imageUrl) {
                        when {
                            contains("apps") -> {
                                val urlId = substringAfter("apps/").substringBefore("/")
                                "https://store.steampowered.com/app/$urlId/"
                            }
                            contains("bundle") -> {
                                val urlId = substringAfter("bundles/").substringBefore("/")
                                "https://store.steampowered.com/bundle/$urlId/"
                            }
                            else -> {
                                "https://store.steampowered.com/sub/$packageId/"
                            }
                        }
                    }
                    gamesList.add(Game(title, url, Game.Platform.STEAM, imageUrl, discountExpiration))
                }
            }.join()

        return gamesList

    }

    private fun retrieveFreeEpicGamesGames(): List<Game>? {
        val (_, _, result) = "https://store-site-backend-static-ipv4.ak.epicgames.com/freeGamesPromotions?locale=en-US"
            .httpGet()
            .responseString()

        if (result is Result.Failure) return null

        return JsonParser.parseString(result.get())
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
                }?.asJsonObject?.get("url")?.asString ?: Game.Platform.EPIC_GAMES.getLogo()
                val expirationEpoch = Instant.parse(
                    it["promotions"]
                        .asJsonObject["promotionalOffers"]
                        .asJsonArray[0]
                        .asJsonObject["promotionalOffers"]
                        .asJsonArray[0]
                        .asJsonObject["endDate"]
                        .asString
                ).epochSecond
                Game(title, url, Game.Platform.EPIC_GAMES, imageUrl, expirationEpoch)
            }
    }

    private fun getFreeGamesChannel(guild: Guild) = configManager[guild].optional<String>("free-games.channel")
        ?.let { guild.getTextChannelById(it) }

    private fun isSteamEnabled(guild: Guild) = configManager[guild].optional<Boolean>("free-games.services.steam")

    private fun isEpicGamesEnabled(guild: Guild) =
        configManager[guild].optional<Boolean>("free-games.services.epic-games")

}