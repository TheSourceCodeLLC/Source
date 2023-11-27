package net.sourcebot.module.roleselector.command

import me.hwiggy.kommander.arguments.Adapter
import me.hwiggy.kommander.arguments.Arguments
import me.hwiggy.kommander.arguments.Group
import me.hwiggy.kommander.arguments.Synopsis
import net.dv8tion.jda.api.MessageBuilder
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.TextChannel
import net.sourcebot.api.command.RootCommand
import net.sourcebot.api.command.SourceCommand
import net.sourcebot.api.getHighestRole
import net.sourcebot.api.response.*
import net.sourcebot.module.roleselector.data.Selector
import net.sourcebot.module.roleselector.data.SelectorHandler

// TODO: CREATE DELETE/LIST COMMANDS
class SelectorCommand(
    private val selectorHandler: SelectorHandler
) : RootCommand() {
    override val name: String = "selector"
    override val description: String = "Manages role selectors."

    init {
        register(
            CreateSelectorCommand(),
            SetSelectorCommand(),
            SendSelectorCommand(),
            RefreshSelectorCommand(),
            InfoSelectorCommand(),
            ListSelectorCommand(),
            DeleteSelectorCommand()
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
                String::lowercase
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
            if (cache[name] != null) return StandardErrorResponse(
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

    private inner class ListSelectorCommand : Bootstrap(
        "list",
        "Sends a list of all of the role selectors."
    ) {
        override fun execute(sender: Message, arguments: Arguments.Processed): Response {
            val guild = sender.guild
            val cache = selectorHandler[guild]
            val selectorList = cache.getSelectors()

            var selectorNames = selectorList.joinToString { it.name }

            // Future proofing errors, but adding pagination to this seems extremely useless as hitting this limit is very unlikely
            selectorNames = if (selectorNames.length >= 4096) "${selectorNames.take(4093)}..." else selectorNames

            return StandardInfoResponse("Selector List", selectorNames)
        }
    }

    private inner class SendSelectorCommand : Bootstrap(
        "send",
        "Sends a role selector."
    ) {
        override val synopsis: Synopsis = Synopsis {
            reqParam("name", "The name of the role selector menu.", Adapter.single())
            optParam("channelId", "The text channel id to send the role selector menu in", Adapter.single())
        }

        override fun execute(sender: Message, arguments: Arguments.Processed): Response {
            val guild = sender.guild
            val name: String = arguments.required(
                "name",
                "You must specify a name for the role selector menu!",
                String::lowercase
            )

            val cache = selectorHandler[guild]
            val selector = cache[name]
                ?: return NoSuchSelectorResponse(name)

            val providedId: String = arguments.optional("channelId") ?: sender.channel.id
            val currentChannelId = sender.channel.id
            val channel: TextChannel = try {
                guild.getTextChannelById(providedId) ?: throw Exception("Provided channel id does not exist.")
            } catch (ex: Exception) {
                return StandardErrorResponse("Invalid Id!", "There is no text channel in this guild with that id.")
            }

            val msgBuilder = MessageBuilder(selector.message)
                .setActionRows(selector.toActionRow(guild))
            val msg = channel.sendMessage(msgBuilder.build()).complete()

            selector.messageIds[providedId]?.add(msg.id) ?: selector.messageIds.put(providedId, mutableListOf(msg.id))
            cache.saveSelector(selector)

            if (currentChannelId != providedId) {
                return StandardSuccessResponse(
                    "Success!",
                    "Successfully sent the role selector to the provided channel!"
                )
            }

            return EmptyResponse()
        }

    }

    private inner class DeleteSelectorCommand : Bootstrap(
        "delete",
        "Deletes a role selector."
    ) {
        override val synopsis: Synopsis = Synopsis {
            reqParam("name", "The name of the role selector menu.", Adapter.single())
        }

        override fun execute(sender: Message, arguments: Arguments.Processed): Response {
            val name: String = arguments.required(
                "name",
                "You must specify a name for the role selector menu!",
                String::lowercase
            )

            val guild = sender.guild
            val cache = selectorHandler[guild]
            val selector = cache[name]
                ?: return NoSuchSelectorResponse(name)

            val messages = cache.retrieveMessages(guild, selector)
            messages.forEach { it.delete().queue() }

            cache.deleteSelector(selector.name)
            return StandardSuccessResponse("Success!", "Successfully delete the $name selector!")
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
                String::lowercase
            )

            val guild = sender.guild
            val cache = selectorHandler[guild]
            val selector = cache[name]
                ?: return NoSuchSelectorResponse(name)
            val newMessage = MessageBuilder(selector.message)
                .setActionRows(selector.toActionRow(guild)).build()

            val messages = cache.retrieveMessages(guild, selector)
            messages.forEach { it.editMessage(newMessage).queue() }

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
                choice(SelectorProperty.PERMISSION, "Modifies the permission.")
                choice(SelectorProperty.MESSAGE, "Modifies the message sent with the role selector.")
            }
        }

        override fun execute(sender: Message, arguments: Arguments.Processed): Response {
            val name: String = arguments.required(
                "name",
                "You must specify a role selector name!",
                String::lowercase
            )

            val property = arguments.required<SelectorProperty>("property", "You did not specify a property to edit!")
            val cache = selectorHandler[sender.guild]
            val selector = cache[name] ?: return NoSuchSelectorResponse(name)
            val processed = property.synopsis.process(arguments.parent.slice())
            val alert = property.applicator(selector, sender.member!!, processed)
            cache.saveSelector(selector)
            return alert
        }

    }

    private inner class InfoSelectorCommand : Bootstrap(
        "info",
        "Displays a role selector's information."
    ) {
        override val cleanupResponse: Boolean = false
        override val synopsis: Synopsis = Synopsis {
            reqParam("name", "The name of the role selector menu.", Adapter.single())
        }

        override fun execute(sender: Message, arguments: Arguments.Processed): Response {
            val name: String = arguments.required(
                "name",
                "You must specify a role selector name!",
                String::lowercase
            )

            val guild = sender.guild
            val cache = selectorHandler[guild]
            val selector = cache[name] ?: return NoSuchSelectorResponse(name)

            val selectorName = selector.name
            val roles = selector.roleIds.mapNotNull { guild.getRoleById(it)?.name }.joinToString()
            val permission = if (selector.hasPermission()) selector.permission else "N/A"
            val isEnabled = !selector.isDisabled
            val placeHolder = selector.placeholder
            var messageText = selector.message
            messageText = if (messageText.length > 250) "${messageText.take(250)}..." else messageText

            var description = ("**Selector Name:** $selectorName" +
                    "\n**Required Permission:** $permission" +
                    "\n**Is Enabled:** $isEnabled" +
                    "\n─────────────────────────────" +
                    "\n**Placeholder Text:** $placeHolder" +
                    "\n─────────────────────────────" +
                    "\n**Message Text:** $messageText" +
                    "\n─────────────────────────────" +
                    "\n**Roles:** $roles").take(3000)
            if (description.length == 3000) description += "..."

            return StandardInfoResponse("Role Selector Info - $selectorName", description)
        }
    }

    private enum class SelectorProperty(
        override val synopsisName: String,
        val synopsis: Synopsis,
        val applicator: (Selector, Member, Arguments.Processed) -> Response
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
                StandardSuccessResponse("Success!", "The placeholder text has been updated to $placeholder")
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
        }, { selector, member, args ->
            val property = args.required<RoleProperties>("property", "You did not specify a property!")
            val processed = property.synopsis.process(args.parent.slice())
            property.applicator(selector, member, processed)
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
        }),
        PERMISSION("permission", Synopsis {
            optParam(
                "permission",
                "The required permission to be able to use the role selector message (If blank there previous permission will be cleared).",
                Adapter.single()
            )
        }, { selector, _, args ->
            val permission: String? = args.optional("permission")
            var message = "Members will now need the roleselector.use.$permission to use this selector!"
            if (permission == null) {
                selector.permission = ""
                message = "Members no longer need a permission to use this selector!"
            } else {
                selector.permission = "roleselector.use.$permission"
            }

            StandardSuccessResponse("Success!", message)
        }),
        MESSAGE("message", Synopsis {
            reqParam(
                "message",
                "The message that will be sent with the role selector menu.",
                Adapter.slurp(" ")
            )
        }, { selector, _, args ->
            val message: String = args.optional("message", "You must provide a message!")
            if (message.length > 1024 || message.isEmpty()) {
                StandardErrorResponse(
                    "Invalid Message!",
                    "The message must be 1024 characters or fewer and at least 1 character!"
                )
            } else {
                selector.message = message
                StandardSuccessResponse("Success!", "Successfully updated the message!")
            }
        })
    }

    private enum class RoleProperties(
        override val synopsisName: String,
        val synopsis: Synopsis,
        val applicator: (Selector, Member, Arguments.Processed) -> Response
    ) : Group.Option {
        ADD("add", Synopsis {
            reqParam("roleIds", "The role ids to add to the role selector menu.", Adapter.slurp(" "))
        }, logic@{ selector, member, args ->
            val guild = member.guild
            val idString: String =
                args.required("roleIds", "You must specify at least one role id to add to the role selector!")

            val idList: MutableList<String> = idString.split(" ").toMutableList()
            val invalidList: MutableList<String> = mutableListOf()

            val source = guild.getMember(guild.jda.selfUser)!!
            val sourceHighest = source.getHighestRole()
            val senderHighest = member.getHighestRole()

            val failReasons = "__Roles may fail to add for the following reasons:__ " +
                    "\n1. There is no role with the supplied id." +
                    "\n2. The role is the everyone role." +
                    "\n3. The role is a managed role." +
                    "\n4. The role is hoisted above your highest role." +
                    "\n5. The role is hoisted above my highest role." +
                    "\n6. The role is already in the selector."

            idList.removeIf {
                try {
                    val role = guild.getRoleById(it) ?: return@removeIf true
                    if (role.isManaged
                        || role.isPublicRole
                        || role.position >= sourceHighest.position
                        || role.position >= senderHighest.position
                        || selector.roleIds.contains(it)
                    ) {
                        invalidList.add(it)
                        return@removeIf true
                    }
                    return@removeIf false
                } catch (ex: Exception) {
                    invalidList.add(it)
                    return@removeIf true
                }
            }

            if (idList.isEmpty()) {
                return@logic StandardErrorResponse("Failed To Add Roles", failReasons)

            }
            val currentIds = selector.roleIds
            currentIds.addAll(idList)
            selector.roleIds = currentIds.distinct().toMutableList()

            val failIds = " except for the following ids: ${invalidList.joinToString()}".take(300)
            val failStr = if (invalidList.isNotEmpty()) " $failIds!\n\n$failReasons" else "!"

            return@logic StandardSuccessResponse("Success!", "Successfully added the requested roles${failStr}")

        }),
        REMOVE("remove", Synopsis {
            reqParam("roleIds", "The role ids to remove from the role selector menu.", Adapter.slurp(" "))
        }, logic@{ selector, _, args ->
            val idString: String =
                args.required("roleIds", "You must specify at least one role id to add to the role selector!")
            val idList: MutableList<String> = idString.split(" ").toMutableList()
            val invalidList: MutableList<String> = mutableListOf()

            val currentIds = selector.roleIds
            idList.removeIf {
                return@removeIf if (!currentIds.contains(it)) {
                    invalidList.add(it)
                    true
                } else false
            }

            if (idList.isEmpty()) {
                return@logic StandardErrorResponse(
                    "Invalid Roles",
                    "The are no roles in the selector that have the given role ids!"
                )
            }

            if (idList.size == selector.roleIds.size) {
                return@logic StandardErrorResponse(
                    "Cannot Remove Roles!",
                    "A role selector must have at least one role!"
                )
            }

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

            return@logic StandardSuccessResponse(
                "Success!",
                "Successfully removed the requested roles${str}!"
            )

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