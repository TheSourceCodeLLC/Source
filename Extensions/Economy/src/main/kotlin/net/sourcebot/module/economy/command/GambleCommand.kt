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
import net.sourcebot.api.round
import net.sourcebot.module.economy.Economy
import java.util.concurrent.ThreadLocalRandom
import kotlin.math.ceil

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
            val booster = economy.booster
            val multiplier = (booster?.multiplier ?: 1.0).round(2)
            val amount = ceil(multiplier * wager).toLong()
            val delta = if (won) amount else -wager
            val response = if (won) {
                StandardSuccessResponse(
                    "Gamble Win!",
                    "You won $amount coins!"
                ).also {
                    if (booster != null) it.appendDescription(
                        "\n\nYou have received an extra ${amount - wager} coins due to your $multiplier coin booster!"
                    )
                }
            } else StandardErrorResponse("Gamble Loss!", "You lost $wager coins!")
            economy.balance += delta
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