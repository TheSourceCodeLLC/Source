package net.sourcebot.impl.command

import me.hwiggy.kommander.InvalidSyntaxException
import me.hwiggy.kommander.arguments.Adapter
import me.hwiggy.kommander.arguments.Arguments
import me.hwiggy.kommander.arguments.Arguments.Processed
import me.hwiggy.kommander.arguments.Group
import me.hwiggy.kommander.arguments.Synopsis
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.GuildChannel
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.TextChannel
import net.sourcebot.Source
import net.sourcebot.api.command.RootCommand
import net.sourcebot.api.command.argument.SourceAdapter
import net.sourcebot.api.permission.Permissible
import net.sourcebot.api.response.Response
import net.sourcebot.api.response.StandardInfoResponse
import net.sourcebot.api.response.StandardSuccessResponse

class PermissionsCommand : RootCommand() {
    override val name = "permissions"
    override val description = "Modify Source permissions."
    override val guildOnly = true
    override val cleanupResponse = false
    override val aliases = listOf("permission", "perms", "perm")
    override val permission = name

    private val permissionHandler = Source.PERMISSION_HANDLER

    override fun execute(sender: Message, arguments: Arguments.Processed): Response {
        val permissionData = permissionHandler.getData(sender.guild)
        val user = permissionData.getUser(sender.member!!)
        val type = arguments.required<PermissibleType>("type", "You did not specify a type to modify!")
        val target = arguments.required<String>("target", "You did not specify a target to modify!")
        val permissible = type.converter(sender.guild, target)
        val operation = arguments.required<Operation>("operation", "You did not specify an operation!")
        val processed = operation.synopsis.process(arguments.parent.slice())
        return operation.executor(type, user, permissible, sender.textChannel, processed).also {
            permissible.update(permissionData)
        }
    }

    override val synopsis = Synopsis {
        reqGroup("type", "The type of entity to modify.", Group.Option.byName<PermissibleType>()) {
            choice(PermissibleType.ROLE, "Switches the command into role mode.")
            choice(PermissibleType.USER, "Switches the command into user mode.")
        }
        reqParam("target", "The target to modify.", Adapter.single())
        reqGroup("operation", "The operation you wish to perform.", Group.Option.byName<Operation>()) {
            choice(Operation.SET, "Set node flags with optional context.")
            choice(Operation.UNSET, "Unset node flags with optional context.")
            choice(Operation.INFO, "View target permission info.")
            choice(Operation.CHECK, "Check target permission info for specific nodes.")
            choice(Operation.CLEAR, "Clear target permission info for optional context.")
            choice(Operation.TEST, "Test target permission info for specific nodes.")
        }
    }
}

private fun performSubcommand(
    type: PermissibleType,
    label: String,
    permissible: Permissible,
    channel: TextChannel,
    supplier: () -> Response
): Response =
    Source.PERMISSION_HANDLER.checkPermission(permissible, "permission.${type.synopsisName}.$label", channel, supplier)

private enum class Operation(
    override val synopsisName: String,
    val executor: PermissionsSubcommand,
    val synopsis: Synopsis = Synopsis { }
) : Group.Option {
    SET("set", { type, user, target, channel, args ->
        performSubcommand(type, "set", user, channel) {
            val node = args.required<String>("node", "You did not specify a node to set!")
            val flag = args.required<Boolean>("flag", "You did not specify a flag for the node!")
            val context = args.optional<String, GuildChannel>("context") {
                SourceAdapter.guildMessageChannel(channel.guild, it)
            }
            val description = if (context != null) {
                target.setPermission(node, flag, context.id)
                "Set permission for ${target.asMention()}: `$node` = `$flag` @ `$context`"
            } else {
                target.setPermission(node, flag)
                "Set permission for ${target.asMention()}: `$node` = `$flag`"
            }
            StandardSuccessResponse("Permission Set!", description)
        }
    }, Synopsis {
        reqParam("node", "The node to set for the target.", Adapter.single())
        reqParam("flag", "The flag to set for the node.", Adapter.boolean())
        optParam("context", "The context for the node to be set.", Adapter.single())
    }),
    UNSET("unset", { type, user, target, channel, args ->
        performSubcommand(type, "unset", user, channel) {
            val node = args.required<String>("node", "You did not specify a node to unset!")
            val context = args.optional<String, GuildChannel>("context") {
                SourceAdapter.guildMessageChannel(channel.guild, it)
            }
            val description = if (context != null) {
                target.unsetPermission(node, context.id)
                "Unset `$node` for ${target.asMention()} @ `$context`"
            } else {
                target.unsetPermission(node)
                "Unset `$node` for ${target.asMention()}"
            }
            StandardSuccessResponse("Permission Unset!", description)
        }
    }, Synopsis {
        reqParam("node", "The node to unset for the target.", Adapter.single())
        optParam("context", "The context for the node to be unset.", Adapter.single())
    }),
    INFO("info", { type, user, target, channel, _ ->
        performSubcommand(type, "info", user, channel) {
            val permissions = target.getPermissions()
            val infoOutput = permissions.joinToString("\n") {
                if (it.context != null) "`${it.node}`: `${it.flag}` @ `${it.context}`"
                else "`${it.node}`: `${it.flag}`"
            }.ifEmpty { "No permissions set." }
            StandardInfoResponse(
                "Permission Information",
                "Permissions for ${target.asMention()}:\n$infoOutput"
            )
        }
    }),
    CHECK("check", { type, user, target, channel, args ->
        performSubcommand(type, "check", user, channel) {
            val node = args.required<String>("node", "You did not specify a node to check!")
            val context = args.optional<String, GuildChannel>("context") {
                SourceAdapter.guildMessageChannel(channel.guild, it)
            }
            val description = if (context != null) {
                val has = target.hasPermission(node, context.id)
                "Permission check for ${target.asMention()}; `$node` @ `$context`: `$has`"
            } else {
                val has = target.hasPermission(node)
                "Permission check for ${target.asMention()}; `$node`: `$has`"
            }
            StandardInfoResponse("Permission Check", description)
        }
    }, Synopsis {
        reqParam("node", "The node to check for the target.", Adapter.single())
        optParam("context", "The context for the node to be checked.", Adapter.single())
    }),
    CLEAR("clear", { type, user, target, channel, args ->
        performSubcommand(type, "clear", user, channel) {
            val context = args.optional<String, GuildChannel>("context") {
                SourceAdapter.guildMessageChannel(channel.guild, it)
            }
            val description = if (context != null) {
                target.clearPermissions(context.id)
                "Permissions cleared for ${target.asMention()} @ `context`"
            } else {
                target.clearPermissions()
                "Permissions cleared for ${target.asMention()}"
            }
            StandardSuccessResponse("Permissions Cleared!", description)
        }
    }, Synopsis {
        optParam("context", "The context for the nodes to be cleared.", Adapter.single())
    }),
    TEST("test", { type, user, target, channel, args ->
        performSubcommand(type, "test", user, channel) {
            val node = args.required<String>("node", "You did not specify a node to test for!")
            val context = args.optional<String, GuildChannel>("context") {
                SourceAdapter.guildMessageChannel(channel.guild, it)
            }
            val description = if (context != null) {
                val test = Source.PERMISSION_HANDLER.hasPermission(target, node, context.id)
                "Permission test for ${target.asMention()}; `$node` @ `$context`: `$test`"
            } else {
                val test = Source.PERMISSION_HANDLER.hasPermission(target, node, null as String?)
                "Permission test for ${target.asMention()}; `$node`: `$test`"
            }
            StandardInfoResponse("Permission Test", description)
        }
    }, Synopsis {
        reqParam("node", "The node to test the target for.", Adapter.single())
        optParam("context", "The context of the node to be tested for.", Adapter.single())
    })
}

enum class PermissibleType(
    override val synopsisName: String,
    val converter: (Guild, String) -> Permissible
) : Group.Option {
    ROLE("role", { guild, arg ->
        val permissionData = Source.PERMISSION_HANDLER.getData(guild)
        val role = SourceAdapter.role(guild, arg) ?: throw InvalidSyntaxException(
            "You did not specify a valid role!"
        )
        permissionData.getRole(role)
    }),
    USER("user", { guild, arg ->
        val permissionData = Source.PERMISSION_HANDLER.getData(guild)
        val member = SourceAdapter.member(guild, arg) ?: throw InvalidSyntaxException(
            "You did not specify a valid member!"
        )
        permissionData.getUser(member)
    })
}

typealias PermissionsSubcommand = (PermissibleType, Permissible, Permissible, TextChannel, Processed) -> Response