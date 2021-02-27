package net.sourcebot.module.experience.command

import net.dv8tion.jda.api.entities.Message
import net.sourcebot.Source
import net.sourcebot.api.command.argument.Adapter
import net.sourcebot.api.command.argument.ArgumentInfo
import net.sourcebot.api.command.argument.Arguments
import net.sourcebot.api.command.argument.OptionalArgument
import net.sourcebot.api.formatPlural
import net.sourcebot.api.formatted
import net.sourcebot.api.response.Response
import net.sourcebot.api.response.StandardInfoResponse
import net.sourcebot.module.profiles.Profiles
import org.bson.Document
import kotlin.math.ceil

class ExperienceLeaderboardCommand : ExperienceRootCommand(
    "xpleaderboard", "See the Guild's XP Leaderboard."
) {
    override val aliases = arrayOf("xplb")
    override val argumentInfo = ArgumentInfo(
        OptionalArgument("page", "The page of the leaderboard to view", 1)
    )

    override fun execute(message: Message, args: Arguments): Response {
        val guild = message.guild
        val profiles = Source.MONGODB.getCollection(guild.id, "profiles")
        val pages = ceil(
            profiles.countDocuments(Profiles.VALID_PROFILE) / 10.0
        ).toInt()
        val page = (args.next(
            Adapter.int(1, pages, "Page must be between 1 and $pages!")
        ) ?: 1) - 1
        val leaderboard = profiles.find(Profiles.VALID_PROFILE).skip(10 * page).limit(10).sort(
            Document("data.experience.amount", -1)
        ).map {
            it["_id"] as String to it.getEmbedded(listOf("data", "experience", "amount"), 0L)
        }.withIndex().joinToString("\n") { (index, data) ->
            val (id, balance) = data
            val name = guild.getMemberById(id)?.formatted() ?: id
            "**#${(index + 1) + (page * 10)} $name**: ${formatPlural(balance, "point")}"
        }
        return StandardInfoResponse(
            "Experience Leaderboard", """
                    $leaderboard
                    
                    Page ${page + 1} of $pages
                """.trimIndent()
        )
    }
}