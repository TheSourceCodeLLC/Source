package net.sourcebot.module.freegames.emitter

import com.github.kittinunf.fuel.core.requests.CancellableRequest
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.result.Result
import com.google.gson.JsonParser
import net.dv8tion.jda.api.entities.Guild
import net.sourcebot.Source
import net.sourcebot.api.configuration.config
import net.sourcebot.api.response.StandardEmbedResponse
import net.sourcebot.api.response.StandardErrorResponse
import net.sourcebot.api.response.StandardSuccessResponse
import net.sourcebot.api.response.StandardWarningResponse
import net.sourcebot.module.freegames.FreeGames
import net.sourcebot.module.freegames.data.Game
import net.sourcebot.module.freegames.data.Platform
import net.sourcebot.module.freegames.event.FreeGameEvent
import org.bson.Document
import java.time.Instant
import java.util.concurrent.TimeUnit

class FreeGameEmitter {

    private val mongo = Source.MONGODB

    /**
     * Starts the executor service that emits any newly found free games to all guilds that have this feature activated
     * This executor service also monitors for expired games
     */
    fun startEmitting() {
        Source.SCHEDULED_EXECUTOR_SERVICE.scheduleAtFixedRate({
            val guilds = Source.SHARD_MANAGER.guilds

            val steamGames = retrieveFreeSteamGames() ?: emptyList()
            val epicGames = retrieveFreeEpicGamesGames() ?: emptyList()
            if (steamGames.isEmpty() && epicGames.isEmpty()) return@scheduleAtFixedRate

            guilds.filter { getFreeGamesChannel(it) != null }
                .forEach {
                    emitToGuild(it, steamGames, epicGames)
                    removeExpiredListings(it)
                    removeExpiredListingsByComparison(it, steamGames.plus(epicGames))
                }
        }, 0, 2, TimeUnit.HOURS)
    }

    /**
     * Calls the free EpicGames and Steam game retrieval functions and then emits the NewGameEvent.
     * This function will only emit games that have not already been posted and will delete ones that are expired.
     *
     * @param guild The guild to emit the NewGameEvent to
     * @param callback Sends a Response object back to the initiator
     */
    fun refreshGuild(guild: Guild, callback: (response: StandardEmbedResponse) -> Unit) {
        val steamGames = retrieveFreeSteamGames()
        val epicGames = retrieveFreeEpicGamesGames()

        if (steamGames == null || epicGames == null) {
            callback.invoke(StandardErrorResponse("Uh Oh!", "Failed to refresh the free game listings!"))
            return
        }

        Source.EXECUTOR_SERVICE.submit {
            val status = emitToGuild(guild, steamGames, epicGames) +
                    removeExpiredListings(guild) +
                    removeExpiredListingsByComparison(guild, steamGames.plus(epicGames))

            if (status > 0) {
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
     * This function will only emit games that have not already been posted.
     *
     * @param guild The guild to emit the NewGameEvent to
     * @param steamGames The Steam games to emit
     * @param epicGames The EpicGames games to emit
     * @return 0 if no games are emitted, 1 if games are emitted
     */
    private fun emitToGuild(guild: Guild, steamGames: List<Game>, epicGames: List<Game>): Int {
        val gameList = mutableListOf<Game>()

        if (isSteamEnabled(guild)) gameList.addAll(steamGames)
        if (isEpicGamesEnabled(guild)) gameList.addAll(epicGames)

        val freeGamesCollection = mongo.getCollection(guild.id, "free-game-log")
        gameList.removeIf {
            freeGamesCollection.find(Document("url", it.url.lowercase())).first() != null
        }

        if (gameList.size == 0) return 0
        Source.SOURCE_EVENTS.fireEvent(FreeGameEvent(guild, gameList))

        return 1
    }

    private fun retrieveFreeSteamGames(): List<Game>? {
        // Testing URL: https://store.steampowered.com/search/results/?ignore_preferences=1&maxprice=5&specials=1&json=1
        // Production URL: https://store.steampowered.com/search/results/?ignore_preferences=1&maxprice=free&specials=1&json=1
        val (_, _, result) = "https://store.steampowered.com/search/results/?ignore_preferences=1&maxprice=free&specials=1&json=1"
            .httpGet()
            .responseString()

        if (result is Result.Failure) return null

        // Key = ID, Value = Game Obj
        val appMap = mutableMapOf<String, Game>()
        val bundleMap = mutableMapOf<String, Game>()
        val packageMap = mutableMapOf<String, Game>()

        JsonParser.parseString(result.get())
            .asJsonObject["items"]
            .asJsonArray
            .forEach {
                val obj = it.asJsonObject
                val logoUrl = obj["logo"].asString
                val name = obj["name"].asString

                with(logoUrl) {
                    when {
                        contains("app") -> {
                            val id = substringAfter("apps/").substringBefore("/")
                            appMap[id] = Game(
                                name,
                                "https://store.steampowered.com/app/$id/",
                                Platform.STEAM,
                                "https://cdn.akamai.steamstatic.com/steam/apps/$id/header.jpg"
                            )
                        }
                        contains("bundles") -> {
                            val idAndHash = substringAfter("bundles/").substringBefore("/capsule")
                            val id = idAndHash.substringBefore("/")
                            bundleMap[id] = Game(
                                name,
                                "https://store.steampowered.com/bundle/$id/",
                                Platform.STEAM,
                                "https://cdn.akamai.steamstatic.com/steam/bundles/$idAndHash/header.jpg"
                            )
                        }
                        contains("subs") -> {
                            val id = substringAfter("subs/").substringBefore("/")
                            packageMap[id] = Game(
                                name,
                                "https://store.steampowered.com/sub/$id/",
                                Platform.STEAM,
                                "https://cdn.akamai.steamstatic.com/steam/subs/$id/header.jpg"
                            )
                        }
                    }
                }

            }

        val requests = mutableListOf<CancellableRequest>()

        appMap.forEach { (id, game) ->
            // These have to be a single request unfortunately because to do multiple app ids filters must be set to price_overview
            requests.add(
                "https://store.steampowered.com/api/appdetails?appids=$id&filters=packages"
                    .httpGet()
                    .responseString { _, _, appResult ->
                        if (appResult is Result.Failure) return@responseString

                        val dataObj = JsonParser.parseString(appResult.get())
                            .asJsonObject[id]
                            .asJsonObject["data"]
                            .asJsonObject
                        val packageObj = dataObj["packages"].asJsonArray

                        when (packageObj.size()) {
                            0 -> return@responseString
                            1 -> packageMap[packageObj[0].asString] = game
                            else -> {
                                val pkgGroups = dataObj["package_groups"].asJsonArray[0].asJsonObject
                                val gameName = pkgGroups["title"].asString.substringAfter("Buy").trim()
                                val pkgGroupsInfo = pkgGroups["subs"].asJsonArray.map { it.asJsonObject }
                                /*
                                This retrieves the pkgId of the package that contains the game name in the option text
                                and has text in the percent_savings_text (if this becomes an issue in the future possibly
                                check if this field is equal to "-100% "
                                 */
                                val pkgId = pkgGroupsInfo.filter {
                                    it["percent_savings_text"].asString.isNotBlank() && it["option_text"].asString.contains(
                                        gameName
                                    )
                                }[0]["packageid"]?.asString ?: return@responseString

                                packageMap[pkgId] = game
                            }
                        }
                    })

        }


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
                array.map { it.asJsonObject }.forEach {
                    val packageId = it["packageid"].asString
                    val discountExpiration = it["discount_end_rtime"].asLong

                    val game = packageMap[packageId] ?: return@forEach
                    game.expirationEpoch = discountExpiration

                    gamesList.add(game)
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
                val hasActivePromotion = it["promotions"].asJsonObject["promotionalOffers"].asJsonArray.size() > 0
                return@filter discountPrice == 0 && hasActivePromotion
            }.map {
                val title = it["title"].asString
                val urlSlug = it["customAttributes"].asJsonArray
                    .map { obj -> obj.asJsonObject }
                    .first { obj -> obj["key"].asString.equals("com.epicgames.app.productSlug", true) }["value"]
                    .asString
                val url = "https://store.epicgames.com/en-US/p/$urlSlug/"

                val imageUrl = it["keyImages"].asJsonArray.find { imageElement ->
                    val imageType = imageElement.asJsonObject["type"].asString
                    return@find imageType.equals("OfferImageWide", true) || imageType.equals(
                        "DieselStoreFrontWide",
                        true
                    )
                }?.asJsonObject?.get("url")?.asString ?: Platform.EPIC_GAMES.getLogo()

                val expirationEpoch = Instant.parse(
                    it["promotions"]
                        .asJsonObject["promotionalOffers"]
                        .asJsonArray[0]
                        .asJsonObject["promotionalOffers"]
                        .asJsonArray[0]
                        .asJsonObject["endDate"]
                        .asString
                ).epochSecond
                Game(title, url, Platform.EPIC_GAMES, imageUrl, expirationEpoch)
            }
    }

    /**
     * Removes all expired game listings.
     *
     * @param guild The guild being checked for expired listings
     * @return 0 if no expired listings are found, 1 if expired games are removed
     */
    private fun removeExpiredListings(guild: Guild): Int {
        val freeGameLog = mongo.getCollection(guild.id, "free-game-log")
        var status = 0
        freeGameLog.find()
            .filter { document ->
                val expirationEpoch = (document["expirationEpoch"] as Long)
                expirationEpoch != 0L && expirationEpoch <= Instant.now().epochSecond
            }
            .groupBy { document -> document["messageId"] }
            .forEach { (_, documents) -> removeListingsFromMessage(guild, documents); status = 1 }
        return status
    }

    /**
     * This will only remove listings that have an expirationEpoch of 0 and are no longer free.
     *
     * @param guild The guild being checked for expired listings
     * @param gameList The game list being compared against
     * @return 0 if no listings are removed, 1 if listings are removed
     */
    private fun removeExpiredListingsByComparison(guild: Guild, gameList: List<Game>): Int {
        var status = 0
        mongo.getCollection(guild.id, "free-game-log").find(Document("expirationEpoch", 0))
            .filter { document -> gameList.none { it.url.equals(document["url"] as String, true) } }
            .groupBy { document -> document["messageId"] }
            .forEach { (_, documents) -> removeListingsFromMessage(guild, documents); status = 1 }
        return status
    }

    /**
     * Deletes a group of listings from a message.
     *
     * @param guild The guild the listing is being removed from
     * @param documents The mongodb documents of the listings being removed
     */
    private fun removeListingsFromMessage(guild: Guild, documents: List<Document>) {
        val freeGameLog = mongo.getCollection(guild.id, "free-game-log")
        val channel = getFreeGamesChannel(guild) ?: return

        val msgId = documents[0]["messageId"] as String
        val names = documents.map { it["name"] as String }
        channel.retrieveMessageById(msgId).queue { message ->
            val embeds = message.embeds.filter { embed -> !names.contains(embed?.title?.lowercase() ?: "") }
            if (embeds.isEmpty()) message.delete().queue()
            else message.editMessageEmbeds(embeds).queue()
        }
        documents.forEach { freeGameLog.deleteOne(it) }
    }

    private fun getFreeGamesChannel(guild: Guild) = FreeGames::class.config(guild).optional<String>("channel")
        ?.let { guild.getTextChannelById(it) }

    private fun isSteamEnabled(guild: Guild) = FreeGames::class.config(guild)
        .required<Boolean>("services.steam")

    private fun isEpicGamesEnabled(guild: Guild) = FreeGames::class.config(guild)
        .required<Boolean>("services.epic-games")

}