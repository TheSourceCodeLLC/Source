package net.sourcebot.module.moderation

import net.sourcebot.api.module.SourceModule

class Moderation : SourceModule() {
    override fun onEnable() {
        registerCommands(
            KickCommand(),
            MuteCommand(),
            TempbanCommand(),
            BanCommand(),
            UnmuteCommand(),
            UnbanCommand()
        )
    }
}