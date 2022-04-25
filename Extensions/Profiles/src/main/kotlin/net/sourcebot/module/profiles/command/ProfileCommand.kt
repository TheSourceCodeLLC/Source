package net.sourcebot.module.profiles.command

import me.hwiggy.kommander.arguments.Adapter
import me.hwiggy.kommander.arguments.Arguments
import me.hwiggy.kommander.arguments.Group
import me.hwiggy.kommander.arguments.Synopsis
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.utils.MarkdownUtil
import net.sourcebot.Source
import net.sourcebot.api.command.argument.SourceAdapter
import net.sourcebot.api.configuration.JsonConfiguration
import net.sourcebot.api.configuration.optional
import net.sourcebot.api.formatLong
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
    override val synopsis = Synopsis {
        optParam("target", "The member who's profile you wish to view.", SourceAdapter.member())
    }

    override fun execute(sender: Message, arguments: Arguments.Processed): Response {
        val target = arguments.optional("target", sender.member!!)
        val profile = Profiles[target]
        val embed = StandardInfoResponse(
            "${target.formatLong()}'s Profile"
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
                embed.addField("Bio:", it, false)
            }
        }
        Source.SOURCE_EVENTS.fireEvent(ProfileRenderEvent(embed, target, profile))
        return embed.sortFields().wrapped(target)
    }

    private inner class ProfileSetCommand : Command(
        "set", "Set a field in your profile."
    ) {
        override val aliases = listOf("update")
        override val synopsis = Synopsis {
            reqGroup("field", "The field to set the value of.", Group.Option.byName<ProfileField>()) {
                choice(ProfileField.BIO, "Sets your profile's biography.")
                choice(ProfileField.GITHUB, "Sets your profile's GitHub username.")
            }
        }

        override fun execute(sender: Message, arguments: Arguments.Processed): Response {
            val field = arguments.required<ProfileField>("field", "You did not specify a field to set the value of!")
            val profile = Profiles[sender.member!!]
            val processed = field.synopsis.process(arguments.parent.slice())
            val value = processed.optional<String>("value")
            return value.ifPresentOrElse(
                { field.ifPresent(profile, it) }, { field.orElse(profile) }
            )
        }
    }

    init {
        register(
            ProfileSetCommand()
        )
    }

    enum class ProfileField(
        override val synopsisName: String,
        val synopsis: Synopsis,
        val ifPresent: (JsonConfiguration, String) -> Response,
        val orElse: (JsonConfiguration) -> Response
    ) : Group.Option {
        GITHUB("github", Synopsis {
            optParam("value", "The GitHub username to show on your profile; absent to remove.", Adapter.single())
        }, { profile, input ->
            val username = GITHUB_USERNAME.find(
                input.replace("https://github.com/", "")
            )?.groups?.get(0)?.value ?: throw me.hwiggy.kommander.InvalidSyntaxException(
                "You did not specify a valid GitHub URL / Username!!"
            )
            val url = "https://github.com/$username"
            profile["github"] = username
            StandardSuccessResponse(
                "Profile Updated!", "You have set your GitHub URL to: $url"
            )

        }, { profile ->
            profile["github"] = null
            StandardSuccessResponse(
                "Profile Updated!", "You have removed your GitHub URL!"
            )
        }),
        BIO("bio", Synopsis {
            optParam("value", "The biography to show on your profile; absent to remove.", Adapter.slurp(" "))
        }, { profile, input ->
            profile["bio"] = input
            StandardSuccessResponse(
                "Profile Updated!", "You have set your bio to: $input"
            )
        }, { profile ->
            profile["bio"] = null
            StandardSuccessResponse(
                "Profile Updated!", "You have removed your bio!"
            )
        })
    }
}

private val GITHUB_USERNAME = "^[a-z\\d](?:[a-z\\d]|-(?=[a-z\\d])){0,38}$".toRegex(RegexOption.IGNORE_CASE)