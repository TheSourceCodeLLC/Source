package net.sourcebot.module.economy.command

import net.dv8tion.jda.api.entities.Message
import net.sourcebot.api.command.argument.Adapter
import net.sourcebot.api.command.argument.Argument
import net.sourcebot.api.command.argument.ArgumentInfo
import net.sourcebot.api.command.argument.Arguments
import net.sourcebot.api.formatted
import net.sourcebot.api.response.Response
import net.sourcebot.api.response.StandardSuccessResponse
import net.sourcebot.module.economy.Economy

class PayCommand : EconomyRootCommand("pay", "Pay members using your coin balance.") {
    override val argumentInfo = ArgumentInfo(
        Argument("target", "The member you want to pay"),
        Argument("amount", "The amount you want to pay")
    )

    override fun execute(message: Message, args: Arguments): Response {
        val senderEco = Economy[message.member!!]
        val target = args.next(Adapter.member(message.guild), "You did not specify a member to pay!")
        val targetEco = Economy[target]
        val amount = args.next(
            Adapter.long(1, senderEco.balance, "Amount to pay must be between 1 and ${senderEco.balance}!"),
            "You did not specify an amount to pay!"
        )
        senderEco.balance -= amount
        targetEco.balance += amount
        return StandardSuccessResponse(
            "Pay Success", "You have sent $amount coins to ${target.formatted()}!"
        )
    }
}