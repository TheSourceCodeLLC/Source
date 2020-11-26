package net.sourcebot.module.economy.command

import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.User
import net.sourcebot.Source
import net.sourcebot.api.command.argument.Adapter
import net.sourcebot.api.command.argument.ArgumentInfo
import net.sourcebot.api.command.argument.Arguments
import net.sourcebot.api.command.argument.OptionalArgument
import net.sourcebot.api.formatPlural
import net.sourcebot.api.formatted
import net.sourcebot.api.response.Response
import net.sourcebot.api.response.StandardInfoResponse
import org.bson.Document

class CoinLeaderboardCommand : EconomyRootCommand(
    "coinleaderboard", "See the Guild's Coin Leaderboard."
) {
    override val argumentInfo = ArgumentInfo(
        OptionalArgument("page", "The page of the leaderboard to view", 1)
    )

    override fun execute(message: Message, args: Arguments): Response {
        val page = args.next(
            Adapter.int(1)
        ) ?: 1
        return LeaderboardQueryResponse(page - 1)
    }

    override fun postResponse(response: Response, forWhom: User, message: Message) {
        if (response !is LeaderboardQueryResponse) return
        val page = response.page
        val guild = message.guild
        val profiles = Source.MONGODB.getCollection(guild.id, "profiles")
        val leaderboard = profiles.find().skip(10 * page).limit(10).sort(
            Document("data.economy.balance", -1)
        ).map {
            it["_id"] as String to it.getEmbedded(listOf("data", "economy", "balance"), 0L)
        }.withIndex().joinToString("\n") { it ->
            val index = it.index
            val (id, balance) = it.value
            val member = guild.getMemberById(id)
            val name = member?.formatted() ?: id
            "**#${(index + 1) + (page * 10)} $name**: ${formatPlural(balance, "coin")}"
        }
        message.editMessage(
            StandardInfoResponse(
                "Coin Leaderboard", leaderboard
            ).asEmbed(forWhom)
        ).queue()
    }

    class LeaderboardQueryResponse(
        internal val page: Int
    ) : StandardInfoResponse(
        "Coin Leaderboard", "Please wait while I query the coin leaderboard."
    )
}