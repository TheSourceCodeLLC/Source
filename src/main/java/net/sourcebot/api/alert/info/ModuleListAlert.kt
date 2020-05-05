package net.sourcebot.api.alert.info

import net.sourcebot.api.alert.InfoAlert
import net.sourcebot.api.module.SourceModule

class ModuleListAlert(modules: Set<SourceModule>) : InfoAlert(
    "Module Listing",
    "Pass a module name from below into this command for that module's command listing."
) {
    init {
        addField("Module List:", modules.joinToString("\n") { "**${it.name}**: ${it.description}" }, false)
    }
}