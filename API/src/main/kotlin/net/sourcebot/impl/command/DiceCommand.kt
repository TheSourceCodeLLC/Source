package net.sourcebot.impl.command

import me.hwiggy.kommander.arguments.Adapter
import me.hwiggy.kommander.arguments.Arguments
import me.hwiggy.kommander.arguments.Synopsis
import net.dv8tion.jda.api.entities.Message
import net.sourcebot.api.command.RootCommand
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
    override val aliases = listOf("roll")
    override val permission = name

    override val transformer = object : RegexTransformer(Regex("(\\d+)d(\\d+)")) {
        override fun transformArguments(label: String, arguments: Arguments): Arguments {
            val (times, sides) = regex.matchEntire(label)!!.destructured
            return Arguments.parse("roll $times $sides") + arguments
        }
    }

    override val synopsis = Synopsis {
        optParam(
            "times", "The number of dice to roll.", Adapter.int(
                min = 1, error = "You must roll at least 1 dice!"
            ), 1
        )
        optParam(
            "sides", "The number of sides on each dice.", Adapter.int(
                min = 3, error = "You must roll dice with at least 3 sides!"
            ), 6
        )
    }

    override fun execute(sender: Message, arguments: Arguments.Processed): Response {
        val times = arguments.optional("times", 1)
        val sides = arguments.optional("sides", 6)
        val rolls = IntArray(times)
        (1..times).forEach { rolls[it - 1] = ThreadLocalRandom.current().nextInt(1, sides) }
        return StandardInfoResponse(
            "Dice Roll - ${times}d${sides}",
            "```json\n${rolls.joinToString { it.toString() }}\n```"
        )
    }
}