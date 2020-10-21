package net.sourcebot.module.documentation

import net.sourcebot.api.module.SourceModule
import net.sourcebot.module.documentation.commands.*

class Documentation : SourceModule() {

    override fun onEnable() {
        val menuHandler = source.menuHandler
        registerCommands(
            JDACommand(menuHandler),
            JavaCommand(menuHandler),
            SpigotCommand(menuHandler),
            BungeeCordCommand(menuHandler),
            DJSCommand(),
            MDNCommand(),
            KotlinCommand()
        )
    }

}