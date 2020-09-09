package net.sourcebot.module.documentation

import net.sourcebot.api.module.SourceModule
import net.sourcebot.module.documentation.commands.*
import net.sourcebot.module.documentation.events.DocSelectorEvent

class Documentation : SourceModule() {

    override fun onEnable() {
        registerCommands(
            JDACommand(),
            JavaCommand(),
            SpigotCommand(),
            BungeeCordCommand(),
            DJSCommand(),
            MDNCommand(),
            KotlinCommand()
        )

        val deleteSeconds: Long = source.properties.required("commands.delete-seconds")
        source.jdaEventSystem.listen(this, DocSelectorEvent(deleteSeconds)::onMessageReceived)
    }

}