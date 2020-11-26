package net.sourcebot.module.economy

import net.dv8tion.jda.api.entities.Member
import net.sourcebot.api.configuration.ConfigurationInfo
import net.sourcebot.api.configuration.JsonConfiguration
import net.sourcebot.api.module.SourceModule
import net.sourcebot.module.economy.command.BalanceCommand
import net.sourcebot.module.economy.command.CoinLeaderboardCommand
import net.sourcebot.module.economy.command.DailyCommand
import net.sourcebot.module.economy.command.GambleCommand
import net.sourcebot.module.economy.data.EconomyData
import net.sourcebot.module.economy.listener.EconomyListener
import net.sourcebot.module.profiles.Profiles

class Economy : SourceModule() {
    override val configurationInfo = ConfigurationInfo("economy") {
        node("nickname-cost", "The amount of coins a nickname change costs")
    }

    override fun onEnable() {
        registerCommands(
            BalanceCommand(),
            GambleCommand(),
            DailyCommand(),
            CoinLeaderboardCommand()
        )
        subscribeEvents(EconomyListener())
    }

    companion object {
        @JvmStatic operator fun get(member: Member): EconomyData {
            val profile = Profiles[member]
            val config = profile.required("economy", ::JsonConfiguration)
            val proxy = object : JsonConfiguration(config) {
                override fun onChange() {
                    profile["economy"] = this
                }
            }
            return EconomyData(proxy)
        }
    }
}