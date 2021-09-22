package net.sourcebot.module.experience.command

import me.hwiggy.kommander.arguments.Adapter
import me.hwiggy.kommander.arguments.Arguments
import me.hwiggy.kommander.arguments.Synopsis
import net.dv8tion.jda.api.entities.Message
import net.sourcebot.Source
import net.sourcebot.api.command.InvalidSyntaxException
import net.sourcebot.api.formatLong
import net.sourcebot.api.formatPlural
import net.sourcebot.api.response.Response
import net.sourcebot.api.response.StandardInfoResponse
import net.sourcebot.module.experience.Experience
import net.sourcebot.module.profiles.Profiles
import org.bson.Document
import kotlin.math.ceil

class ExperienceLeaderboardCommand : ExperienceRootCommand(
    "xpleaderboard", "See the Guild's XP Leaderboard."
) {
    override val aliases = listOf("xplb")
    override val synopsis = Synopsis {
        optParam(
            "page", "The page of the leaderboard to view.", Adapter.int(
                1, error = "Page must be at least 1!"
            )
        )
    }

    override fun execute(message: Message, args: Arguments.Processed): Response {
        val guild = message.guild
        val profiles = Source.MONGODB.getCollection(guild.id, "profiles")
        val pages = ceil(
            profiles.countDocuments(Profiles.VALID_PROFILE) / 10.0
        ).toInt()
        val page = args.optional("page", 1) - 1
        if (page > pages - 1) throw InvalidSyntaxException(
            "Page must be between 1 and $pages!"
        )
        val leaderboard = profiles.find(Profiles.VALID_PROFILE).skip(10 * page).limit(10).sort(
            Document("data.experience.amount", -1)
        ).map {
            it["_id"] as String to it.getEmbedded(listOf("data", "experience", "amount"), 0L)
        }.withIndex().joinToString("\n") { (index, data) ->
            val (id, balance) = data
            val name = guild.jda.getUserById(id)?.formatLong() ?: id
            "**#${(index + 1) + (page * 10)} $name**: ${
                "${"Level ${Experience.getLevel(balance)}"} (${formatPlural(balance, "point")})"
            }"
        }
        return StandardInfoResponse(
            "Experience Leaderboard", """
                    $leaderboard
                    
                    Page ${page + 1} of $pages
                """.trimIndent()
        )
    }
}