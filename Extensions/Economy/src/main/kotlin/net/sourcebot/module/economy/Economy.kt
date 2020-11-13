package net.sourcebot.module.economy

import net.sourcebot.api.module.SourceModule
import net.sourcebot.module.economy.command.BalanceCommand
import net.sourcebot.module.economy.command.GambleCommand
import net.sourcebot.module.economy.listener.EconomyListener

class Economy : SourceModule() {
    override fun onEnable() {
        registerCommands(
            BalanceCommand(),
            GambleCommand()
        )
        subscribeEvents(EconomyListener())
    }
}