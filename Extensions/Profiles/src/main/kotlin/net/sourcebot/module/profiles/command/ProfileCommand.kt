package net.sourcebot.module.profiles.command

import net.dv8tion.jda.api.entities.Message
import net.sourcebot.Source
import net.sourcebot.api.command.argument.Adapter
import net.sourcebot.api.command.argument.ArgumentInfo
import net.sourcebot.api.command.argument.Arguments
import net.sourcebot.api.command.argument.OptionalArgument
import net.sourcebot.api.formatted
import net.sourcebot.api.response.Response
import net.sourcebot.api.response.StandardInfoResponse
import net.sourcebot.module.profiles.Profiles
import net.sourcebot.module.profiles.event.ProfileRenderEvent

class ProfileCommand : RootCommand(
    "profile", "Manage member profiles."
) {
    override val argumentInfo = ArgumentInfo(
        OptionalArgument("target", "The member who's profile you wish to view.")
    )

    override fun execute(message: Message, args: Arguments): Response {
        val target = args.next(Adapter.member(message.guild)) ?: message.member!!
        val profile = Profiles.getProfile(target)
        val embed = StandardInfoResponse(
            "${target.formatted()}'s Profile"
        ).also {
            it.setThumbnail(target.user.effectiveAvatarUrl)
            it.addField(
                "Account Information:", """
                **Nickname:** ${target.effectiveName}
                **Created At:** ${target.timeCreated.format(Source.DATE_TIME_FORMAT)}
                **Joined At:** ${target.timeJoined.format(Source.DATE_TIME_FORMAT)}
            """.trimIndent(), false
            )
        }
        Source.SOURCE_EVENTS.fireEvent(ProfileRenderEvent(embed, profile))
        return embed
    }

    private inner class ProfileSetCommand : Command(
        "set", "Set a field in a member's profile."
    ) {

    }
}