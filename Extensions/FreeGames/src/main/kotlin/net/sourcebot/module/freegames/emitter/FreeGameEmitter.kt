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
        val csvAppStr = appMap.keys.joinToString(",")
        requests.add("https://store.steampowered.com/broadcast/ajaxgetbatchappcapsuleinfo?appids=$csvAppStr&cc=NL&l=english"
            .httpGet()
            .responseString { _, _, appResult ->
                if (appResult is Result.Failure) return@responseString
                val array = JsonParser.parseString(appResult.get()).asJsonObject["apps"].asJsonArray
                array.map { it.asJsonObject }.forEach {
                    val packageId = it["subid"]?.asString ?: return@forEach
                    packageMap[packageId] = appMap[it["appid"].asString] ?: return@forEach
                }
            })

        val csvBundleStr = bundleMap.keys.joinToString(",")
        requests.add(
            "https://store.steampowered.com/actions/ajaxresolvebundles?bundleids=${csvBundleStr}&cc=US&l=english"
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
                    val game = packageMap[packageId] ?: return@forEach

                    val discountExpiration = (it["discount_end_rtime"].asLong).let { end ->
                        when (end) {
                            0L -> getDiscountEndByClanId(it["creator_clan_ids"].asJsonArray[0].asString)
                            else -> end
                        }
                    }
                    // https://store.steampowered.com/events/ajaxgetadjacentpartnerevents/?clan_accountid=30421086
                    game.expirationEpoch = discountExpiration

                    gamesList.add(game)
                }
            }.join()

        return gamesList
    }

    /**
     * Retrieves a discount end time for a Steam game by fetching the end time for the most recent event.
     * This only works if there is an active event and if the game is contained in that event.
     * if the game is not included in the event, the end time will not be accurate
     *
     * @param clanId The creator clan id for the game
     * @returns The discount end time in epoch seconds or 0 if there is no active event
     */
    private fun getDiscountEndByClanId(clanId: String): Long {
        val (_, _, eventResult) = "https://store.steampowered.com/events/ajaxgetadjacentpartnerevents/?clan_accountid=$clanId"
            .httpGet()
            .responseString()

        if (eventResult is Result.Failure) return 0
        val array = JsonParser.parseString(eventResult.get()).asJsonObject["events"].asJsonArray
        /*
         If this causes issues in the future, check if the appId matches the appId of the game the discount end time is needed from.
         I do not do this currently because I have no idea what happens if it is a bundle
         */
        return array.map { it.asJsonObject }
            // This check is necessary
            .firstOrNull { it["rtime32_end_time"].asLong > Instant.now().epochSecond }
            ?.get("rtime32_end_time")?.asLong ?: 0
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
                val imageUrl = (it["keyImages"].asJsonArray.find { imageElement ->
                    val imageType = imageElement.asJsonObject["type"].asString
                    return@find imageType.equals("OfferImageWide", true) || imageType.equals(
                        "DieselStoreFrontWide",
                        true
                    )
                }?.asJsonObject?.get("url")?.asString ?: Platform.EPIC_GAMES.getLogo()).replace(" ", "%20")
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