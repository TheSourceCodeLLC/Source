package net.sourcebot.module.economy

import net.sourcebot.api.configuration.ConfigurationInfo
import net.sourcebot.api.module.SourceModule
import net.sourcebot.module.economy.command.BalanceCommand
import net.sourcebot.module.economy.command.GambleCommand
import net.sourcebot.module.economy.listener.EconomyListener

class Economy : SourceModule() {
    override val configurationInfo = ConfigurationInfo("economy") {
        node("nickname-cost", "The amount of coins a nickname change costs")
    }

    override fun onEnable() {
        registerCommands(
            BalanceCommand(),
            GambleCommand()
        )
        subscribeEvents(EconomyListener())
    }
}