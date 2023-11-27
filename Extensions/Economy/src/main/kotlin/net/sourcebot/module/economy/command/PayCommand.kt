package net.sourcebot.module.economy.command

import me.hwiggy.kommander.InvalidSyntaxException
import me.hwiggy.kommander.arguments.Adapter
import me.hwiggy.kommander.arguments.Arguments
import me.hwiggy.kommander.arguments.Synopsis
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Message
import net.sourcebot.api.command.argument.SourceAdapter
import net.sourcebot.api.formatLong
import net.sourcebot.api.response.Response
import net.sourcebot.api.response.StandardSuccessResponse
import net.sourcebot.module.economy.Economy

class PayCommand : EconomyRootCommand("pay", "Pay members using your coin balance.") {
    override val synopsis = Synopsis {
        reqParam("target", "The Member you want to pay.", SourceAdapter.member())
        reqParam(
            "amount", "The amount you want to pay.",
            Adapter.long(1, error = "The amount to pay must be at least 1!")
        )
    }

    override fun execute(sender: Message, arguments: Arguments.Processed): Response {
        val senderEco = Economy[sender.member!!]
        val target = arguments.required<Member>("target", "You did not specify a member to pay!")
        val targetEco = Economy[target]
        val amount = arguments.required<Long>("amount", "You did not specify an amount to pay!")
        if (amount > senderEco.balance) throw InvalidSyntaxException("You may not pay more than your balance!")
        senderEco.balance -= amount
        targetEco.balance += amount
        return StandardSuccessResponse(
            "Pay Success", "You have sent $amount coins to ${target.formatLong()}!"
        )
    }
}