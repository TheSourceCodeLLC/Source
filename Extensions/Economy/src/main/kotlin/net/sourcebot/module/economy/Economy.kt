package net.sourcebot.module.economy

import net.dv8tion.jda.api.entities.Member
import net.sourcebot.Source
import net.sourcebot.api.configuration.ConfigurationInfo
import net.sourcebot.api.module.SourceModule
import net.sourcebot.module.economy.command.*
import net.sourcebot.module.economy.data.EconomyData
import net.sourcebot.module.economy.listener.BoosterListener
import net.sourcebot.module.economy.listener.EconomyListener
import net.sourcebot.module.profiles.Profiles

class Economy : SourceModule() {
    override val configurationInfo = ConfigurationInfo("economy") {
        node("nickname-cost", "The amount of coins a nickname change costs")
    }

    override fun enable() {
        registerCommands(
            BalanceCommand(),
            GambleCommand(),
            DailyCommand(),
            CoinLeaderboardCommand(),
            PayCommand()
        )
        subscribeEvents(EconomyListener())

        if (Source.MODULE_HANDLER.moduleExists("Boosters")) {
            subscribeEvents(BoosterListener())
        }
    }

    companion object {
        @JvmStatic operator fun get(member: Member) = Profiles.proxyObject(member, "economy") {
            EconomyData(it, member)
        }
    }
}