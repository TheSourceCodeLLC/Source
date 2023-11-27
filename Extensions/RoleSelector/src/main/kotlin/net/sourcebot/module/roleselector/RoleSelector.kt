package net.sourcebot.module.roleselector

import net.sourcebot.api.database.MongoSerial
import net.sourcebot.api.module.SourceModule
import net.sourcebot.module.roleselector.command.SelectorCommand
import net.sourcebot.module.roleselector.data.Selector
import net.sourcebot.module.roleselector.data.SelectorHandler

class RoleSelector : SourceModule() {

    override fun enable() {
        MongoSerial.register(Selector.Serial())
        val selectorHandler = SelectorHandler()
        registerCommands(SelectorCommand(selectorHandler))

        subscribeEvents(
            SelectorHandler()
        )
    }

}