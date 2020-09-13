package net.sourcebot.impl.command

import net.dv8tion.jda.api.OnlineStatus
import net.dv8tion.jda.api.entities.Message
import net.sourcebot.Source
import net.sourcebot.api.command.RootCommand
import net.sourcebot.api.command.argument.Arguments
import net.sourcebot.api.response.InfoResponse
import net.sourcebot.api.response.Response

class GuildInfoCommand : RootCommand() {
    override val name = "guildinfo"
    override val description = "Show information about the current guild."
    override val guildOnly = true
    override val aliases = arrayOf("info", "online", "boosts")
    override val permission = name

    override fun execute(message: Message, args: Arguments): Response {
        val guild = message.guild
        val name = guild.name
        val icon = guild.iconUrl
        val owner = guild.retrieveOwner().complete()
        val created = guild.timeCreated
            .atZoneSameInstant(Source.TIME_ZONE)
            .format(Source.DATE_TIME_FORMAT)
        val membersUnfiltered = guild.members
        val members = membersUnfiltered.filterNot { it.user.isBot }
        val totalMembers = members.count()
        val bots = membersUnfiltered.filter { it.user.isBot }.count()
        val online = members.count { it.onlineStatus != OnlineStatus.OFFLINE }

        val boosts = guild.boostCount
        val boostTier = guild.boostTier.key
        val boosters = guild.boosters.count()


        return InfoResponse(
            "$name Guild Information:",
            "**Creation Date**: $created\n" +
                    "**Owner**: ${owner.asMention}\n"
        ).addField(
            "Member Demographics",
            "**Online Members**: $online\n" +
                    "**Total Members**: $totalMembers\n" +
                    "**Bots**: $bots",
            false
        ).addField(
            "Boost Information",
            "**Total Boosts**: $boosts\n" +
                    "**Boosters**: $boosters\n" +
                    "**Boost Tier**: $boostTier",
            false
        ).setThumbnail(icon) as Response
    }
}