package net.sourcebot.module.info.alert

import net.sourcebot.api.alert.InfoAlert
import net.sourcebot.api.command.Command

class CommandInfoAlert(command: Command) : InfoAlert(
    "Command Information",
    "Command Parameters: `<>` = Required, `()` = Optional"
) {
    init {
        addField("Description:", command.description, false)
        addField("Usage:", command.usage.trim(), false)
        addField("Parameter Detail:", command.argumentInfo.getParameterDetail(), false)
        val aliases = command.aliases
        if (aliases.isNotEmpty()) {
            addField("Aliases:", aliases.joinToString(", "), false)
        }
    }
}