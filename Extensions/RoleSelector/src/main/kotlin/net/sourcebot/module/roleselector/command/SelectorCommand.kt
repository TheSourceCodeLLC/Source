package net.sourcebot.module.roleselector.command

import me.hwiggy.kommander.arguments.Adapter
import me.hwiggy.kommander.arguments.Arguments
import me.hwiggy.kommander.arguments.Group
import me.hwiggy.kommander.arguments.Synopsis
import net.dv8tion.jda.api.MessageBuilder
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Message
import net.sourcebot.api.command.RootCommand
import net.sourcebot.api.command.SourceCommand
import net.sourcebot.api.response.EmptyResponse
import net.sourcebot.api.response.Response
import net.sourcebot.api.response.StandardErrorResponse
import net.sourcebot.api.response.StandardSuccessResponse
import net.sourcebot.module.roleselector.data.Selector
import net.sourcebot.module.roleselector.data.SelectorHandler

// TODO: OVERRIDE DELETE SECONDS WHEN SENDING MENU TO 0
class SelectorCommand(
    private val selectorHandler: SelectorHandler
) : RootCommand() {
    override val name: String = "selector"
    override val description: String = "Manages role selectors."

    init {
        addChildren(
            CreateSelectorCommand(),
            SetSelectorCommand(),
            SendSelectorCommand(),
            RefreshSelectorCommand()
        )
    }

    private inner class CreateSelectorCommand : Bootstrap(
        "create",
        "Creates a role selector."
    ) {
        override val synopsis: Synopsis = Synopsis {
            reqParam("name", "The name of the role selector menu.", Adapter.single())
            reqParam("roleIds", "The ids that will be added to the role selector menu.", Adapter.slurp(" "))
        }

        override fun execute(sender: Message, arguments: Arguments.Processed): Response {
            val guild = sender.guild
            val name: String = arguments.required(
                "name",
                "You must specify a name for the role selector menu!",
                String::toLowerCase
            )

            val idString: String =
                arguments.required("roleIds", "You must specify at least one role id to add to the role selector!")
            val idList: MutableList<String> = idString.split(" ").toMutableList()

            val invalidIdList = mutableListOf<String>()

            idList.forEach {
                val role = guild.getRoleById(it)
                if (role == null) {
                    idList.remove(it)
                    invalidIdList.add(it)
                }
            }

            if (idList.isEmpty()) {
                return StandardErrorResponse(
                    "Uh Oh!",
                    "Failed to create role selector because all provided role ids do not exist!"
                )
            }
            val cache = selectorHandler[guild]
            if (cache.getSelector(name) != null) return StandardErrorResponse(
                "Uh Oh!",
                "A role selector already exists with that name!"
            )
            selectorHandler[guild].createSelector(name, idList)

            if (invalidIdList.size > 0) {
                return StandardErrorResponse(
                    "Invalid Ids!",
                    "Failed to add the roles with the following ids to the role selector: ${invalidIdList.joinToString()}!"
                )

            }

            return StandardSuccessResponse("Success!", "Successfully create a role selector named $name!")
        }

    }

    // TODO: FINISH CHANNELS ARG STUFF + SEND EPHEMERAL WHEN DOING THAT
    private inner class SendSelectorCommand : Bootstrap(
        "send",
        "Sends a role selector."
    ) {
        override val synopsis: Synopsis = Synopsis {
            reqParam("name", "The name of the role selector menu.", Adapter.single())
            optParam("channelId", "The channel id to send the role selector menu in", Adapter.single())
        }

        override fun execute(sender: Message, arguments: Arguments.Processed): Response {
            val guild = sender.guild
            val name: String = arguments.required(
                "name",
                "You must specify a name for the role selector menu!",
                String::toLowerCase
            )

            val cache = selectorHandler[guild]
            val selector = cache.getSelector(name)
                ?: return StandardErrorResponse(
                    "Uh Oh!",
                    "There is no role selector with that name!"
                )
            val msgBuilder =
                MessageBuilder("Select a role here! To remove a role you must unselect and select the option again.")
                    .setActionRows(selector.toActionRow(guild))
            val msg = sender.channel.sendMessage(msgBuilder.build()).complete()

            val channelId = sender.channel.id
            selector.messageIds[channelId]?.add(msg.id) ?: selector.messageIds.put(channelId, mutableListOf(msg.id))
            cache.saveSelector(selector)

            return EmptyResponse()
        }

    }

    private inner class RefreshSelectorCommand : Bootstrap(
        "refresh",
        "Refreshes a role selector."
    ) {
        override val synopsis: Synopsis = Synopsis {
            reqParam("name", "The name of the role selector menu.", Adapter.single())
        }

        override fun execute(sender: Message, arguments: Arguments.Processed): Response {
            val name: String = arguments.required(
                "name",
                "You must specify a name for the role selector menu!",
                String::toLowerCase
            )

            val guild = sender.guild
            val cache = selectorHandler[guild]
            val selector = cache.getSelector(name)
                ?: return StandardErrorResponse(
                    "Uh Oh!",
                    "There is no role selector with that name!"
                )
            val newMessage = MessageBuilder("Select a role here!")
                .setActionRows(selector.toActionRow(guild)).build()

            selector.messageIds.forEach { (channelId, messageIdList) ->
                val channel = guild.getTextChannelById(channelId)
                if (channel == null) {
                    selector.messageIds.remove(channelId)
                    return@forEach
                }

                messageIdList.forEach {
                    try {
                        val message = channel.retrieveMessageById(it).complete()
                        message.editMessage(newMessage).queue()
                    } catch (ex: Exception) {
                        messageIdList.remove(it)
                    }

                }

            }

            cache.saveSelector(selector)
            return StandardSuccessResponse("Success!", "Successfully refreshed the specified selector menu!")
        }

    }

    private inner class SetSelectorCommand : Bootstrap(
        "edit",
        "Edit a role selector's properties."
    ) {
        override val synopsis: Synopsis = Synopsis {
            reqParam("name", "The name of the role selector menu.", Adapter.single())
            reqGroup(
                "property",
                "The property of the role selector to edit.",
                Group.Option.byName<SelectorProperty>()
            ) {
                choice(SelectorProperty.PLACEHOLDER, "Modifies the placeholder text.")
                choice(SelectorProperty.ROLES, "Modifies the role ids.")
                choice(SelectorProperty.ENABLED, "Modifies the state.")
            }
        }

        override fun execute(sender: Message, arguments: Arguments.Processed): Response {
            val name: String = arguments.required(
                "name",
                "You must specify a role selector name!",
                String::toLowerCase
            )

            val property = arguments.required<SelectorProperty>("property", "You did not specify a property to edit!")
            val cache = selectorHandler[sender.guild]
            val selector = cache.getSelector(name) ?: return NoSuchSelectorResponse(name)
            val processed = property.synopsis.process(arguments.parent.slice())
            val alert = property.applicator(selector, sender.guild, processed)
            cache.saveSelector(selector)
            return alert
        }


    }

    private enum class SelectorProperty(
        override val synopsisName: String,
        val synopsis: Synopsis,
        val applicator: (Selector, Guild, Arguments.Processed) -> Response
    ) : Group.Option {
        PLACEHOLDER("placeholder", Synopsis {
            reqParam("placeholder", "The placeholder text in the role selector menu.", Adapter.slurp(" "))
        }, { selector, _, args ->
            val placeholder: String = args.required("placeholder", "You must specify placeholder text!")
            if (placeholder.length > 100) {
                StandardErrorResponse(
                    "Invalid Placeholder Text!",
                    "The placeholder text must be 100 characters or fewer!"
                )
            } else {
                selector.placeholder = placeholder
                StandardSuccessResponse("Success!", "The placeholder text has been updated too $placeholder")
            }
        }),
        ROLES("roles", Synopsis {
            reqGroup(
                "property",
                "Whether to add or remove roles from the role selector menu.",
                Group.Option.byName<RoleProperties>()
            ) {
                choice(RoleProperties.ADD, "Adds roles to the selector menu.")
                choice(RoleProperties.REMOVE, "Removes role from the role selector menu.")
            }
        }, { selector, guild, args ->
            val property = args.required<RoleProperties>("property", "You did not specify a property!")
            val processed = property.synopsis.process(args.parent.slice())
            property.applicator(selector, guild, processed)
        }),
        ENABLED("enabled", Synopsis {
            reqParam("state", "Whether or not the role selector menu is enabled.", Adapter.single())
        }, { selector, _, args ->
            val sBool: String = args.required("state", "You must specify a state (True = enabled)!")
            val newState: Boolean = sBool.toBoolean()
            if (!newState && !sBool.equals("false", true)) {
                StandardErrorResponse("Invalid State!", "A role selector's state can only be true or false!")
            } else {
                selector.isDisabled = !newState
                val str = if (newState) "enabled" else "disabled"
                StandardSuccessResponse("Success!", "The selector with the name of ${selector.name} is now $str!")
            }
        })
    }

    private enum class RoleProperties(
        override val synopsisName: String,
        val synopsis: Synopsis,
        val applicator: (Selector, Guild, Arguments.Processed) -> Response
    ) : Group.Option {

        // TODO: PREVENT ADDING OR REMOVAL OF ROLES ABOVE CURRENT ROLE

        ADD("add", Synopsis {
            reqParam("roleIds", "The role ids to add to the role selector menu.", Adapter.slurp(" "))
        }, { selector, guild, args ->
            val idString: String =
                args.required("roleIds", "You must specify at least one role id to add to the role selector!")
            val idList: MutableList<String> = idString.split(" ").toMutableList()
            val invalidList: MutableList<String> = mutableListOf()

            idList.removeIf {
                try {
                    return@removeIf guild.getRoleById(it) == null
                } catch (ex: Exception) {
                    invalidList.add(it)
                    return@removeIf true
                }
            }

            if (idList.isEmpty()) {
                StandardErrorResponse("Invalid Roles", "The are no roles that have the given role ids!")
            } else {
                val currentIds = selector.roleIds
                currentIds.addAll(idList)
                selector.roleIds = currentIds.distinct().toMutableList()

                var str =
                    if (invalidList.isNotEmpty()) " except for the following ids: ${invalidList.joinToString()}" else ""

                val builder = StringBuilder(str)
                if (builder.length > 300) {
                    builder.setLength(300)
                    builder.append("...")
                    str = builder.toString()
                }

                StandardSuccessResponse("Success!", "Successfully added the requested roles${str}!")
            }
        }),
        REMOVE("remove", Synopsis {
            reqParam("roleIds", "The role ids to remove from the role selector menu.", Adapter.slurp(" "))
        }, { selector, _, args ->
            val idString: String =
                args.required("roleIds", "You must specify at least one role id to add to the role selector!")
            val idList: MutableList<String> = idString.split(" ").toMutableList()
            val invalidList: MutableList<String> = mutableListOf()

            val currentIds = selector.roleIds
            idList.removeIf {
                val bool = !currentIds.contains(it)
                if (bool) invalidList.add(it)
                bool
            }

            if (idList.isEmpty()) {
                StandardErrorResponse(
                    "Invalid Roles",
                    "The are no roles in the selector that have the given role ids!"
                )
            } else {
                currentIds.removeAll(idList)
                selector.roleIds = currentIds.distinct().toMutableList()

                var str =
                    if (invalidList.isNotEmpty()) " except for the following ids: ${invalidList.joinToString()}" else ""

                val builder = StringBuilder(str)
                if (builder.length > 300) {
                    builder.setLength(300)
                    builder.append("...")
                    str = builder.toString()
                }

                StandardSuccessResponse(
                    "Success!",
                    "Successfully removed the requested roles${str}!"
                )
            }
        })
    }

    private abstract class Bootstrap(
        final override val name: String,
        final override val description: String
    ) : SourceCommand() {
        final override val permission = "roleselector.$name"
        final override val guildOnly = true
    }

    private class NoSuchSelectorResponse(name: String) :
        StandardErrorResponse("Invalid Selector!", "There is no selector named `$name`!")
}