package net.sourcebot.module.roleapplications

import net.sourcebot.api.database.MongoSerial
import net.sourcebot.api.module.SourceModule
import net.sourcebot.module.roleapplications.command.ApplicationsCommand
import net.sourcebot.module.roleapplications.data.ApplicationHandler
import net.sourcebot.module.roleapplications.data.ApplicationModel

class RoleApplications : SourceModule() {

    override fun onEnable() {
        MongoSerial.register(ApplicationModel.Serial())

        val appHandler = ApplicationHandler(source.mongodb)

        // Add start cmd which sends questions in dms
        // and add guild specific application channels which apps are sent to when finished

        registerCommands(
            ApplicationsCommand(appHandler)
        )


    }
}