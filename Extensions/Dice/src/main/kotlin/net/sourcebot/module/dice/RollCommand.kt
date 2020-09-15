package net.sourcebot.module.dice

import net.dv8tion.jda.api.entities.Message
import net.sourcebot.api.command.InputTransformer
import net.sourcebot.api.command.InvalidSyntaxException
import net.sourcebot.api.command.RootCommand
import net.sourcebot.api.command.argument.Adapter
import net.sourcebot.api.command.argument.ArgumentInfo
import net.sourcebot.api.command.argument.Arguments
import net.sourcebot.api.command.argument.OptionalArgument
import net.sourcebot.api.response.InfoResponse
import net.sourcebot.api.response.Response
import java.util.concurrent.ThreadLocalRandom

class RollCommand : RootCommand() {
    override val name = "roll"
    override val description = "Roll x number of y sided dice."
    override val argumentInfo = ArgumentInfo(
        OptionalArgument("times", "How many times (>= 1) to roll the dice.", 1),
        OptionalArgument("sides", "The number of sides (>= 3) on each dice.", 6)
    )
    override val transformers = setOf<InputTransformer>(
        object : InputTransformer(Regex("(\\d+)d(\\d+)")) {
            override fun transformArguments(label: String, arguments: Arguments): Arguments {
                val (times, sides) = regex.matchEntire(label)?.destructured!!
                return Arguments.parse("roll $times $sides")
            }
        }
    )

    override fun execute(message: Message, args: Arguments): Response {
        val times = args.next(Adapter.int()) ?: 1
        if (times < 1) throw InvalidSyntaxException("You may not roll a dice less than 1 time!")
        val sides = args.next(Adapter.int()) ?: 6
        if (sides < 3) throw InvalidSyntaxException("You may not roll a dice with less than 3 sides!")
        val rolls = IntArray(times)
        (1..times).forEach { rolls[it - 1] = ThreadLocalRandom.current().nextInt(1, sides) }
        return InfoResponse(
            "Dice Roll - ${times}d${sides}",
            "```json\n${rolls.joinToString { it.toString() }}\n```"
        )
    }
}