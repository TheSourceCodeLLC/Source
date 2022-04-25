package net.sourcebot.module.economy.command

import me.hwiggy.kommander.arguments.Arguments
import net.dv8tion.jda.api.entities.Message
import net.sourcebot.Source
import net.sourcebot.api.configuration.required
import net.sourcebot.api.differenceBetween
import net.sourcebot.api.formatPlural
import net.sourcebot.api.response.Response
import net.sourcebot.api.response.StandardErrorResponse
import net.sourcebot.api.response.StandardSuccessResponse
import net.sourcebot.module.economy.Economy
import net.sourcebot.module.economy.data.DailyRecord
import net.sourcebot.module.economy.data.EconomyData
import java.time.Instant
import java.time.temporal.ChronoUnit

class DailyCommand : EconomyRootCommand(
    "daily", "Progress your daily streak."
) {
    private val configManager = Source.CONFIG_MANAGER
    override fun execute(sender: Message, arguments: Arguments.Processed): Response {
        val economy = Economy[sender.member!!]
        val now = Instant.now()
        val baseWin = configManager[sender.guild].required("economy.daily.base") { 25L }
        val perDiem = configManager[sender.guild].required("economy.daily.bonus") { 5L }
        val daily = if (economy.daily != null) {
            val (count, expiry) = economy.daily!!
            if (now.isAfter(expiry)) {
                updateDaily(economy, 1)
                return StandardErrorResponse(
                    description = "You claimed your daily reward of ${
                        formatPlural(
                            baseWin,
                            "coin"
                        )
                    }, but lost your daily streak of $count!"
                ).also {
                    economy.balance += baseWin
                }
            }
            val runBy = expiry.minus(1, ChronoUnit.DAYS)
            if (now.isBefore(runBy)) return StandardErrorResponse(
                description = "You must wait another ${differenceBetween(now, runBy)} to use that command!"
            )
            updateDaily(economy, count + 1)
        } else updateDaily(economy, 1)
        val winnings = baseWin + if (daily.count > 1) (perDiem * daily.count) else 0
        return StandardSuccessResponse(
            description = "You have claimed your daily reward of ${formatPlural(baseWin, "coin")}!"
        ).also {
            if (winnings > baseWin) {
                val difference = winnings - baseWin
                it.appendDescription(
                    "\nYou won an additional ${
                        formatPlural(
                            difference,
                            "coin"
                        )
                    } due to your daily streak of ${daily.count}!"
                )
            }
            economy.balance += winnings
        }
    }

    private fun updateDaily(
        data: EconomyData, newCount: Long
    ) = DailyRecord(newCount, Instant.now().plus(2, ChronoUnit.DAYS)).also { data.daily = it }
}