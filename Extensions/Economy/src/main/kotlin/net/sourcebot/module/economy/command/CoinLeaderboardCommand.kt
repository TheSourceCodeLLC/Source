package net.sourcebot.module.economy.command

import me.hwiggy.kommander.InvalidSyntaxException
import me.hwiggy.kommander.arguments.Adapter
import me.hwiggy.kommander.arguments.Arguments
import me.hwiggy.kommander.arguments.Synopsis
import net.dv8tion.jda.api.entities.Message
import net.sourcebot.Source
import net.sourcebot.api.formatLong
import net.sourcebot.api.formatPlural
import net.sourcebot.api.response.Response
import net.sourcebot.api.response.StandardInfoResponse
import net.sourcebot.module.profiles.Profiles
import org.bson.Document
import kotlin.math.ceil

class CoinLeaderboardCommand : EconomyRootCommand(
    "coinleaderboard", "See the Guild's Coin Leaderboard."
) {
    override val aliases = listOf("clb", "coinlb")
    override val synopsis = Synopsis {
        optParam(
            "page", "The page of the leaderboard to view.",
            Adapter.int(1, error = "Page must be at least 1!"), 1
        )
    }

    override fun execute(sender: Message, arguments: Arguments.Processed): Response {
        val guild = sender.guild
        val profiles = Source.MONGODB.getCollection(guild.id, "profiles")
        val pages = ceil(
            profiles.countDocuments(Profiles.VALID_PROFILE) / 10.0
        ).toInt()
        val page = arguments.optional("page", 1)
        if (page > pages) throw InvalidSyntaxException("Page must be between 1 and $pages!")
        val leaderboard = profiles.find(Profiles.VALID_PROFILE).skip(10 * page).limit(10).sort(
            Document("data.economy.balance", -1)
        ).map {
            it["_id"] as String to it.getEmbedded(listOf("data", "economy", "balance"), 0L)
        }.withIndex().joinToString("\n") { (index, data) ->
            val (id, balance) = data
            val name = guild.jda.getUserById(id)?.formatLong() ?: id
            "**#${(index + 1) + (page * 10)} $name**: ${formatPlural(balance, "coin")}"
        }
        return StandardInfoResponse(
            "Coin Leaderboard", """
                    $leaderboard
                    
                    Page ${page + 1} of $pages
                """.trimIndent()
        )
    }
}