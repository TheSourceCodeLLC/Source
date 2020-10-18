package net.sourcebot.module.roleapplications

import net.sourcebot.api.configuration.ConfigurationInfo
import net.sourcebot.api.database.MongoSerial
import net.sourcebot.api.module.SourceModule
import net.sourcebot.module.roleapplications.command.ApplicationsCommand
import net.sourcebot.module.roleapplications.data.ApplicationHandler
import net.sourcebot.module.roleapplications.data.ApplicationModel

class RoleApplications : SourceModule() {
    override val configurationInfo = ConfigurationInfo("applications") {
        node("channel", "The channel in which completed applications are sent")
    }

    override fun onEnable() {
        MongoSerial.register(ApplicationModel.Serial())

        val appHandler = ApplicationHandler(
            this,
            source.mongodb,
            source.configurationManager
        )

        // Add optional customizable cooldowns for each app
        // Add option to open/close apps
        // Add ability to edit apps kekw
        // Save finished apps in DB (not unfinished apps this would not be possible due to the way the system works)

        registerCommands(
            ApplicationsCommand(appHandler)
        )

        subscribeEvents(
            appHandler
        )


    }
}