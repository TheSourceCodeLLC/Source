package net.sourcebot.module.documentation

import net.sourcebot.api.module.SourceModule
import net.sourcebot.module.documentation.commands.*

class Documentation : SourceModule() {

    override fun enable() {
        registerCommands(
            JDACommand(),
            JavaCommand(),
            SpigotCommand(),
            BungeeCordCommand(),
            DJSCommand(),
            MDNCommand(),
            KotlinCommand(),
            NPMCommand()
        )
    }

}