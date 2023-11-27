package net.sourcebot.module.documentation.commands.bootstrap

import net.sourcebot.api.command.RootCommand

abstract class DocumentationCommand(
    final override val name: String,
    final override val description: String
) : RootCommand() {
    final override val cleanupResponse = false
    final override val permission = "documentation.$name"
}