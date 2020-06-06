package net.sourcebot.module.permissions.commands.parent

import net.dv8tion.jda.api.entities.Message
import net.sourcebot.api.alert.Alert
import net.sourcebot.api.alert.InfoAlert
import net.sourcebot.api.command.argument.Argument
import net.sourcebot.api.command.argument.ArgumentInfo
import net.sourcebot.api.command.argument.Arguments
import net.sourcebot.api.permission.PermissionHandler
import net.sourcebot.module.permissions.commands.PermissionCommand

class ParentInfoCommand(
    permissionHandler: PermissionHandler
) : PermissionCommand(permissionHandler) {
    override val name = "parentinfo"
    override val description = "Show parents for a specified target."
    override val argumentInfo = ArgumentInfo(
        Argument("user|group|role", "The target's type"),
        Argument("target", "The target to show parents for.")
    )
    override val permission = name

    override fun execute(message: Message, args: Arguments): Alert {
        val type = args.next("You did not specify a target type!")
        val target = args.next("You did not specify a valid target!")
        args.backtrack()
        val targetHolder = getPermissionHolder(type, args, message.guild)
        val parents = targetHolder.getParents()
        val desc = if (parents.isEmpty()) {
            "${type.capitalize()} `$target` does not have any parents."
        } else {
            "${type.capitalize()} `$target`'s parents: ${parents.joinToString(", ") {
                "`${it.name}`"
            }}"
        }
        return InfoAlert("Parent Information:", desc)
    }
}