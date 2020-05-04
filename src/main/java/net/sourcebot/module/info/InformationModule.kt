package net.sourcebot.module.info

import net.sourcebot.api.command.CommandMap
import net.sourcebot.api.module.SourceModule

class InformationModule(
    modules: Set<SourceModule>,
    commandMap: CommandMap
) : SourceModule {
    override val name = "Information"
    override val description = "Commands relating to general bot information."
    override val commands = setOf(
        HelpCommand(modules, commandMap),
        TimingsCommand()
    )
}