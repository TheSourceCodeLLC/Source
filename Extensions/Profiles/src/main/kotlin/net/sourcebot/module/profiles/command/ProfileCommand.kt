package net.sourcebot.module.profiles.command

import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.utils.MarkdownUtil
import net.sourcebot.Source
import net.sourcebot.api.command.InvalidSyntaxException
import net.sourcebot.api.command.argument.*
import net.sourcebot.api.formatted
import net.sourcebot.api.ifPresentOrElse
import net.sourcebot.api.response.Response
import net.sourcebot.api.response.StandardInfoResponse
import net.sourcebot.api.response.StandardSuccessResponse
import net.sourcebot.api.sortFields
import net.sourcebot.api.wrapped
import net.sourcebot.module.profiles.Profiles
import net.sourcebot.module.profiles.event.ProfileRenderEvent

class ProfileCommand : RootCommand(
    "profile", "Manage member profiles."
) {
    override val argumentInfo = ArgumentInfo(
        OptionalArgument("target", "The member who's profile you wish to view.", "self")
    )

    override fun execute(message: Message, args: Arguments): Response {
        val target = args.next(Adapter.member(message.guild)) ?: message.member!!
        val profile = Profiles[target]
        val embed = StandardInfoResponse(
            "${target.formatted()}'s Profile"
        ).also { embed ->
            embed.setThumbnail(target.user.effectiveAvatarUrl)
            embed.addField(
                "Account Information:", """
                **Nickname:** ${target.effectiveName}
                **Created At:** ${target.timeCreated.format(Source.DATE_TIME_FORMAT)}
                **Joined At:** ${target.timeJoined.format(Source.DATE_TIME_FORMAT)}
                ${
                    profile.optional<String>("github")?.let {
                        "**GitHub Username:** ${MarkdownUtil.maskedLink(it, "https://github.com/$it")}"
                    } ?: ""
                }
            """.trimIndent(), false
            )
            profile.optional<String>("bio")?.let {
                embed.addField("Bio", it, false)
            }
        }
        Source.SOURCE_EVENTS.fireEvent(ProfileRenderEvent(embed, target, profile))
        return embed.sortFields().wrapped(target)
    }

    private inner class ProfileSetCommand : Command(
        "set", "Set a field in your profile."
    ) {
        override val aliases = arrayOf("update")
        override val argumentInfo = ArgumentInfo(
            Argument("bio|github", "The field to set the value of."),
            Argument("value", "The value to set at this field.")
        )

        private val GITHUB_USERNAME = "^[a-z\\d](?:[a-z\\d]|-(?=[a-z\\d])){0,38}$".toRegex(RegexOption.IGNORE_CASE)
        override fun execute(message: Message, args: Arguments): Response {
            val field = args.next(
                "You did not specify a field to set the value of!\n" +
                        "Valid fields: `github`, `bio`"
            ).toLowerCase()
            val profile = Profiles[message.member!!]
            return when (field) {
                "github" -> {
                    args.next().ifPresentOrElse({ input ->
                        val username = GITHUB_USERNAME.find(
                            input.replace("https://github.com/", "")
                        )?.groups?.get(0)?.value ?: throw InvalidSyntaxException(
                            "You did not specify a valid GitHub URL / Username!!"
                        )
                        val url = "https://github.com/$username"
                        profile["github"] = username
                        StandardSuccessResponse(
                            "Profile Updated!", "You have set your GitHub URL to: $url"
                        )
                    }, {
                        profile["github"] = null
                        StandardSuccessResponse(
                            "Profile Updated!", "You have removed your GitHub URL!"
                        )
                    })
                }
                "bio" -> {
                    args.slurp(" ").ifPresentOrElse({ input ->
                        profile["bio"] = input
                        StandardSuccessResponse(
                            "Profile Updated!", "You have set your bio to: $input"
                        )
                    }, {
                        profile["bio"] = null
                        StandardSuccessResponse(
                            "Profile Updated!", "You have removed your bio!"
                        )
                    })
                }
                else -> throw InvalidSyntaxException(
                    "You did not specify a valid profile field!\n" +
                            "Valid fields: `github`, `bio`"
                )
            }
        }
    }

    init {
        addChildren(
            ProfileSetCommand()
        )
    }
}