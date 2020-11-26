package net.sourcebot.module.economy.command

import net.dv8tion.jda.api.entities.Message
import net.sourcebot.Source
import net.sourcebot.api.command.argument.Arguments
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
    override fun execute(message: Message, args: Arguments): Response {
        val economy = Economy[message.member!!]
        val now = Instant.now()
        val daily = if (economy.daily != null) {
            val (count, expiry) = economy.daily!!
            if (now.isAfter(expiry)) {
                updateDaily(economy, 1)
                return StandardErrorResponse(
                    description = "You have lost your daily streak of $count!"
                )
            }
            val runBy = expiry.minus(1, ChronoUnit.DAYS)
            if (now.isBefore(runBy)) return StandardErrorResponse(
                description = "You must wait another ${differenceBetween(now, runBy)} to use that command!"
            )
            updateDaily(economy, count + 1)
        } else updateDaily(economy, 1)
        val baseWin = configManager[message.guild].required("economy.daily.base") { 25L }
        val perDiem = configManager[message.guild].required("economy.daily.bonus") { 5L }
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