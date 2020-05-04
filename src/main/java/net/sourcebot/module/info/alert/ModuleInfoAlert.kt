package net.sourcebot.module.info.alert

import net.sourcebot.api.alert.InfoAlert
import net.sourcebot.api.module.SourceModule

class ModuleInfoAlert(module: SourceModule) : InfoAlert(
    "Module Information",
    "${module.name} - ${module.description}"
) {
    init {
        addField("Command Listing:", module.commands.joinToString("\n") {
            "**${it.name}**: ${it.description}"
        }, false)
    }
}