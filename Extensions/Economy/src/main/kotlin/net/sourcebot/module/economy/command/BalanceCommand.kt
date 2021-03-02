package net.sourcebot.module.economy.command

import net.dv8tion.jda.api.entities.Message
import net.sourcebot.api.command.argument.*
import net.sourcebot.api.formatLong
import net.sourcebot.api.response.Response
import net.sourcebot.api.response.StandardErrorResponse
import net.sourcebot.api.response.StandardInfoResponse
import net.sourcebot.api.response.StandardSuccessResponse
import net.sourcebot.api.wrapped
import net.sourcebot.module.economy.Economy

class BalanceCommand : EconomyRootCommand(
    "balance", "Manage Member balances."
) {
    override val aliases = arrayOf("bal", "coins")
    override val argumentInfo = ArgumentInfo(
        OptionalArgument("target", "The Member who's balance you wish to view.", "self")
    )

    override fun execute(message: Message, args: Arguments): Response {
        val target = args.next(Adapter.member(message.guild)) ?: message.member!!
        val economy = Economy[target]
        return StandardInfoResponse(
            "${target.effectiveName}'s Balance:",
            ":moneybag: **${target.formatLong()}** has ${economy.balance} coins."
        ).wrapped(target)
    }

    private class BalanceSetCommand : EconomyCommand(
        "set", "Set a Member's balance."
    ) {
        override val argumentInfo = ArgumentInfo(
            OptionalArgument("target", "The Member who's balance you wish to update.", "self"),
            Argument("balance", "The new balance for the Member.")
        )

        override fun execute(message: Message, args: Arguments): Response {
            val target = args.next(Adapter.member(message.guild)) ?: message.member!!
            val balance = args.next(
                Adapter.long(0, error = "New balance may not be negative!"),
                "You did not specify a valid balance for the member!"
            )
            val economy = Economy[target].also {
                it.balance = balance
            }
            return StandardSuccessResponse(
                "Balance Updated!",
                "${target.formatLong()}'s balance has been set to ${economy.balance}!"
            )
        }
    }

    private class BalanceAddCommand : EconomyCommand(
        "add", "Add coins to a Member's balance."
    ) {
        override val argumentInfo = ArgumentInfo(
            OptionalArgument("target", "The Member who's balance you wish to add to.", "self"),
            Argument("amount", "The amount of coins to add to the Member's balance.")
        )

        override fun execute(message: Message, args: Arguments): Response {
            val target = args.next(Adapter.member(message.guild)) ?: message.member!!
            val amount = args.next(
                Adapter.long(1, error = "Amount to add must not be less than 1!"),
                "You did not specify a valid number of coins to add!"
            )
            Economy[target].balance += amount
            return StandardSuccessResponse(
                "Balance Updated!",
                "$amount coins have been added to ${target.formatLong()}'s balance!"
            )
        }
    }

    private class BalanceSubtractCommand : EconomyCommand(
        "subtract", "Subtract coins from a Member's balance."
    ) {
        override val argumentInfo = ArgumentInfo(
            OptionalArgument("target", "The Member who's balance you wish to subtract from.", "self"),
            Argument("amount", "The amount of coins to subtract from the Member's balance.")
        )

        override fun execute(message: Message, args: Arguments): Response {
            val target = args.next(Adapter.member(message.guild)) ?: message.member!!
            val economy = Economy[target]
            val balance = economy.balance
            if (balance < 1) return StandardErrorResponse(
                "Balance Update Failure!",
                "${target.formatLong()} does not have any coins to subtract!"
            )
            val amount = args.next(
                Adapter.long(1, balance, "Amount to subtract must be between 1 and $balance!"),
                "You did not specify a valid number of coins to subtract!"
            )
            economy.balance -= amount
            return StandardSuccessResponse(
                "Balance Updated!",
                "$amount coins have been subtracted from ${target.formatLong()}'s balance!"
            )
        }
    }

    init {
        addChildren(
            BalanceSetCommand(),
            BalanceAddCommand(),
            BalanceSubtractCommand()
        )
    }
}