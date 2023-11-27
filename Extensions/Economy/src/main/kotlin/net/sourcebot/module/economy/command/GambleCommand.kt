package net.sourcebot.module.economy.command

import me.hwiggy.kommander.arguments.Adapter
import me.hwiggy.kommander.arguments.Arguments
import me.hwiggy.kommander.arguments.Synopsis
import net.dv8tion.jda.api.entities.Message
import net.sourcebot.api.command.GuildCooldown
import net.sourcebot.api.durationOf
import net.sourcebot.api.response.Response
import net.sourcebot.api.response.StandardErrorResponse
import net.sourcebot.api.response.StandardSuccessResponse
import net.sourcebot.module.economy.Economy
import java.util.concurrent.ThreadLocalRandom

class GambleCommand : EconomyRootCommand(
    "gamble", "Wager some of your coins for a chance to win more."
) {
    override val aliases = listOf("bet", "g", "wager")
    override val synopsis = Synopsis {
        reqParam(
            "wager", "The amount of coins you want to wager.",
            Adapter.long(1, error = "You must wager at least 1 coin!")
        )
    }

    private val cooldown = GuildCooldown(durationOf("2m30s"))
    override fun execute(sender: Message, arguments: Arguments.Processed): Response {
        val member = sender.member!!
        val economy = Economy[member]
        if (economy.balance < 1) return StandardErrorResponse(
            "Gamble Failure!",
            "You do not have enough money to wager!"
        )
        val wager = arguments.required<Long>("wager", "You did not specify a valid amount of coins to wager!")
        if (wager > economy.balance) return StandardErrorResponse(
            "Gamble Failure!", "You may not wager more than your balance!"
        )
        return cooldown.test(member, {
            val won = ThreadLocalRandom.current().nextInt(1, 100) < 40
            val amount = if (won) wager else -wager
            val (delta, changelog) = economy.addBalance(amount)
            val response = if (won) {
                StandardSuccessResponse(
                    "Gamble Win!",
                    "You won $wager coins!"
                ).also {
                    if (delta != wager) {
                        val additional = changelog.joinToString("\n") { (amount, log) ->
                            var lineItem = "$amount ($log)"
                            if (amount > 0) lineItem = "+$lineItem"
                            return@joinToString lineItem
                        }
                        it.addField("Additional Gains:", additional, false)
                    }
                }
            } else StandardErrorResponse("Gamble Loss!", "You lost $wager coins!")

            return@test response.also {
                it.appendDescription(
                    """
                
                
                You now have ${economy.balance} coins.
            """.trimIndent()
                )
            }
        }, { formatted ->
            StandardErrorResponse(
                "Gamble Failure!",
                "You must wait another $formatted to use that command!"
            )
        })
    }
}