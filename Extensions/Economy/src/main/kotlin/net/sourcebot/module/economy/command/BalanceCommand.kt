package net.sourcebot.module.economy.command

import me.hwiggy.kommander.arguments.Adapter
import me.hwiggy.kommander.arguments.Arguments
import me.hwiggy.kommander.arguments.Synopsis
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Message
import net.sourcebot.api.command.InvalidSyntaxException
import net.sourcebot.api.command.argument.SourceAdapter
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
    override val aliases = listOf("bal", "coins")

    override val synopsis = Synopsis {
        optParam("target", "The Member who's balance you wish to view.", Adapter.single())
    }

    override fun execute(sender: Message, arguments: Arguments.Processed): Response {
        val target = arguments.optional<String, Member>("target", sender.member!!) {
            SourceAdapter.member(sender.guild, it)
        }
        val economy = Economy[target]
        return StandardInfoResponse(
            "${target.effectiveName}'s Balance:",
            ":moneybag: **${target.formatLong()}** has ${economy.balance} coins."
        ).wrapped(target)
    }

    private class BalanceSetCommand : EconomyCommand(
        "set", "Set a Member's balance."
    ) {
        override val synopsis = Synopsis {
            optParam("target", "The Member who's balance you wish to update.", Adapter.single())
            reqParam(
                "balance", "The new balance for the Member.",
                Adapter.long(0, error = "New balance may not be negative!")
            )
        }

        override fun execute(sender: Message, arguments: Arguments.Processed): Response {
            val target =
                arguments.optional<String, Member>("target", sender.member!!) { SourceAdapter.member(sender.guild, it) }
            val balance = arguments.required<Long>("balance", "You did not specify a valid balance for the member!")
            val economy = Economy[target].also { it.balance = balance }
            return StandardSuccessResponse(
                "Balance Updated!",
                "${target.formatLong()}'s balance has been set to ${economy.balance}!"
            )
        }
    }

    private class BalanceAddCommand : EconomyCommand(
        "add", "Add coins to a Member's balance."
    ) {
        override val synopsis = Synopsis {
            optParam("target", "The Member who's balance you wish to add to.", Adapter.single())
            reqParam(
                "amount", "The amount of coins to add to the Member's balance.",
                Adapter.long(1, error = "Amount to add must not be less than 1!")
            )
        }

        override fun execute(sender: Message, arguments: Arguments.Processed): Response {
            val target =
                arguments.optional<String, Member>("target", sender.member!!) { SourceAdapter.member(sender.guild, it) }
            val amount = arguments.required<Long>("amount", "You did not specify a valid number of coins to add!")
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
        override val synopsis = Synopsis {
            optParam("target", "The Member who's balance you wish to subtract from.", Adapter.single())
            reqParam(
                "amount", "The amount of coins to subtract from the Member's balance.",
                Adapter.long(1, error = "The amount to subtract may not be less than 1!")
            )
        }

        override fun execute(sender: Message, arguments: Arguments.Processed): Response {
            val target = arguments.optional<String, Member>("target", sender.member!!) {
                SourceAdapter.member(sender.guild, it)
            }
            val economy = Economy[target]
            val balance = economy.balance
            if (balance < 1) return StandardErrorResponse(
                "Balance Update Failure!",
                "${target.formatLong()} does not have any coins to subtract!"
            )
            val amount = arguments.required<Long>("amount", "You did not specify a valid number of coins to subtract!")
            if (amount > balance) throw InvalidSyntaxException("The amount to subtract may not exceed the target balance!")
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