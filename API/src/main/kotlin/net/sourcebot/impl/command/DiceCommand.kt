package net.sourcebot.impl.command

import net.dv8tion.jda.api.entities.Message
import net.sourcebot.api.command.RootCommand
import net.sourcebot.api.command.argument.Adapter
import net.sourcebot.api.command.argument.ArgumentInfo
import net.sourcebot.api.command.argument.Arguments
import net.sourcebot.api.command.argument.OptionalArgument
import net.sourcebot.api.response.Response
import net.sourcebot.api.response.StandardInfoResponse
import java.util.concurrent.ThreadLocalRandom

/**
 * This command is used to showcase the InputTransformer system
 * This command may also be used to test the function of [ThreadLocalRandom]
 */
class DiceCommand : RootCommand() {
    override val name = "dice"
    override val description = "Roll some dice."
    override val aliases = arrayOf("roll")
    override val permission = name

    override val transformer = object : RegexTransformer(Regex("(\\d+)d(\\d+)")) {
        override fun transformArguments(label: String, arguments: Arguments): Arguments {
            val (times, sides) = regex.matchEntire(label)!!.destructured
            return Arguments.parse("roll $times $sides") + arguments
        }
    }

    override val argumentInfo = ArgumentInfo(
        OptionalArgument("times", "The number of dice to roll.", 1),
        OptionalArgument("sides", "The number of sides on each dice.", 6)
    )

    override fun execute(message: Message, args: Arguments): Response {
        val times = args.next(
            Adapter.int(1, error = "You may not roll a dice less than one time!")
        ) ?: 1
        val sides = args.next(
            Adapter.int(3, error = "You may not roll a dice with less than 3 sides!")
        ) ?: 6
        val rolls = IntArray(times)
        (1..times).forEach { rolls[it - 1] = ThreadLocalRandom.current().nextInt(1, sides) }
        return StandardInfoResponse(
            "Dice Roll - ${times}d${sides}",
            "```json\n${rolls.joinToString { it.toString() }}\n```"
        )
    }
}