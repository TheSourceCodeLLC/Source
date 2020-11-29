package net.sourcebot.impl.command

import net.dv8tion.jda.api.entities.*
import net.sourcebot.Source
import net.sourcebot.api.command.InvalidSyntaxException
import net.sourcebot.api.command.RootCommand
import net.sourcebot.api.command.argument.*
import net.sourcebot.api.permission.Permissible
import net.sourcebot.api.permission.SourceUser
import net.sourcebot.api.response.Response
import net.sourcebot.api.response.StandardInfoResponse
import net.sourcebot.api.response.StandardSuccessResponse

class PermissionsCommand : RootCommand() {
    override val name = "permissions"
    override val description = "Modify Source permissions."
    override val guildOnly = true
    override val cleanupResponse = false
    override val aliases = arrayOf("permission", "perms", "perm")
    override val permission = name

    private val permissionHandler = Source.PERMISSION_HANDLER

    override fun execute(message: Message, args: Arguments): Response {
        val permissionData = permissionHandler.getData(message.guild)
        val user = permissionData.getUser(message.member!!)
        val type = args.next("You did not specify a type to modify!").toLowerCase()
        val permissible = when (type) {
            "role" -> {
                val role = args.next(Adapter.role(message.guild), "You did not specify a valid role!")
                permissionData.getRole(role)
            }
            "user" -> {
                val member = args.next(Adapter.member(message.guild), "You did not specify a valid member!")
                permissionData.getUser(member)
            }
            else -> throw InvalidSyntaxException("You did not specify a valid type!")
        }
        val valid = subcommands.keys.joinToString(", ") { "`$it`" }
        val operation = args.next(
            "You did not specify an operation!\nValid operations: $valid"
        ).toLowerCase()
        val command = subcommands[operation] ?: throw InvalidSyntaxException(
            "Invalid Operation!\nValid operations: $valid"
        )
        return command(type, user, permissible, message.textChannel, args).also { permissible.update(permissionData) }
    }

    private fun performSubcommand(
        type: String,
        label: String,
        permissible: Permissible,
        channel: TextChannel,
        supplier: () -> Response
    ) = permissionHandler.checkPermission(permissible, "permission.$type.$label", channel, supplier)

    private val setCommand: PermissionsSubcommand = { type, user, target, channel, args ->
        performSubcommand(type, "set", user, channel) {
            val node = args.next("You did not specify a node to set!")
            val flag = args.next(Adapter.boolean(), "You did not specify a flag for the node!")
            val context = args.next(
                Adapter.textChannel(channel.guild) or Adapter.category(channel.guild)
            )?.id
            val description = if (context != null) {
                target.setPermission(node, flag, context)
                "Set permission for ${target.asMention()}: `$node` = `$flag` @ `$context`"
            } else {
                target.setPermission(node, flag)
                "Set permission for ${target.asMention()}: `$node` = `$flag`"
            }
            StandardSuccessResponse("Permission Set!", description)
        }
    }
    private val unsetCommand: PermissionsSubcommand = { type, user, target, channel, args ->
        performSubcommand(type, "unset", user, channel) {
            val node = args.next("You did not specify a node to unset!")
            val context = args.next(
                Adapter.textChannel(channel.guild) or Adapter.category(channel.guild)
            )?.id
            val description = if (context != null) {
                target.unsetPermission(node, context)
                "Unset `$node` for ${target.asMention()} @ `$context`"
            } else {
                target.unsetPermission(node)
                "Unset `$node` for ${target.asMention()}"
            }
            StandardSuccessResponse("Permission Unset!", description)
        }
    }
    private val infoCommand: PermissionsSubcommand = { type, user, target, channel, _ ->
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
    }
    private val checkCommand: PermissionsSubcommand = { type, user, target, channel, args ->
        performSubcommand(type, "check", user, channel) {
            val node = args.next("You did not specify a node to check!")
            val context = args.next(
                Adapter.textChannel(channel.guild) or Adapter.category(channel.guild)
            )?.id
            val description = if (context != null) {
                val has = target.hasPermission(node, context)
                "Permission check for ${target.asMention()}; `$node` @ `$context`: `$has`"
            } else {
                val has = target.hasPermission(node)
                "Permission check for ${target.asMention()}; `$node`: `$has`"
            }
            StandardInfoResponse("Permission Check", description)
        }
    }
    private val clearCommand: PermissionsSubcommand = { type, user, target, channel, args ->
        performSubcommand(type, "clear", user, channel) {
            val context = args.next(
                Adapter.textChannel(channel.guild) or Adapter.category(channel.guild)
            )?.id
            val description = if (context != null) {
                target.clearPermissions(context)
                "Permissions cleared for ${target.asMention()} @ `context`"
            } else {
                target.clearPermissions()
                "Permissions cleared for ${target.asMention()}"
            }
            StandardSuccessResponse("Permissions Cleared!", description)
        }
    }
    private val testCommand: PermissionsSubcommand = { type, user, target, channel, args ->
        performSubcommand(type, "test", user, channel) {
            val node = args.next("You did not specify a node to test!")
            val context = args.next(
                Adapter.textChannel(channel.guild) or Adapter.category(channel.guild)
            )?.id
            val description = if (context != null) {
                val test = permissionHandler.hasPermission(target, node, context)
                "Permission test for ${target.asMention()}; `$node` @ `$context`: `$test`"
            } else {
                val test = permissionHandler.hasPermission(target, node, null as String?)
                "Permission test for ${target.asMention()}; `$node`: `$test`"
            }
            StandardInfoResponse("Permission Test", description)
        }
    }

    private val subcommands = hashMapOf(
        "set" to setCommand,
        "unset" to unsetCommand,
        "info" to infoCommand,
        "check" to checkCommand,
        "clear" to clearCommand,
        "test" to testCommand
    )
    override val argumentInfo = ArgumentInfo(
        Argument("role|user", "The type of entity to modify."),
        Argument("target", "The target to modify."),
        Argument(subcommands.keys.joinToString("|"), "The operation you wish to perform."),
        Argument("data...", "The data supplied to the chosen operation.")
    )
}

typealias PermissionsSubcommand = (String, SourceUser, Permissible, TextChannel, Arguments) -> Response