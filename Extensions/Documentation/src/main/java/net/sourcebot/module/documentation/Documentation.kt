package net.sourcebot.module.documentation

import net.sourcebot.Source
import net.sourcebot.api.module.SourceModule
import net.sourcebot.module.documentation.commands.*
import net.sourcebot.module.documentation.events.DocSelectorEvent

class Documentation : SourceModule() {

    override fun onEnable() {
        val source = Source.instance

        registerCommands(
            JDACommand(),
            JavaCommand(),
            SpigotCommand(),
            BungeeCordCommand(),
            DJSCommand(),
            MDNCommand()
        )

        val deleteSeconds: Long = source.properties.required("commands.delete-seconds")
        source.shardManager.addEventListener(DocSelectorEvent(deleteSeconds))
    }

}