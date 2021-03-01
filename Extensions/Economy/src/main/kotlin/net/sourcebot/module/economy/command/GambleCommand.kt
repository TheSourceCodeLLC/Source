package net.sourcebot.module.economy.command

import net.dv8tion.jda.api.entities.Message
import net.sourcebot.api.command.GuildCooldown
import net.sourcebot.api.command.argument.Adapter
import net.sourcebot.api.command.argument.Argument
import net.sourcebot.api.command.argument.ArgumentInfo
import net.sourcebot.api.command.argument.Arguments
import net.sourcebot.api.durationOf
import net.sourcebot.api.response.Response
import net.sourcebot.api.response.StandardErrorResponse
import net.sourcebot.api.response.StandardSuccessResponse
import net.sourcebot.module.economy.Economy
import java.util.concurrent.ThreadLocalRandom

class GambleCommand : EconomyRootCommand(
    "gamble", "Wager some of your coins for a chance to win more."
) {
    override val aliases = arrayOf("bet", "g", "wager")
    override val argumentInfo = ArgumentInfo(
        Argument("wager", "The amount of coins you want to wager.")
    )

    private val cooldown = GuildCooldown(durationOf("2m30s"))
    override fun execute(message: Message, args: Arguments): Response {
        val member = message.member!!
        val economy = Economy[member]
        if (economy.balance < 1) return StandardErrorResponse(
            "Gamble Failure!",
            "You do not have enough money to wager!"
        )
        val wager = args.next(
            Adapter.long(1, error = "You must wager at least 1 coin!"),
            "You did not specify a valid amount of coins to wager!"
        )
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