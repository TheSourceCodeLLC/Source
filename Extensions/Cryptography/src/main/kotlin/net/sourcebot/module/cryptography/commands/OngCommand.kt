package net.sourcebot.module.cryptography.commands

import net.dv8tion.jda.api.entities.Message
import net.sourcebot.api.response.Response
import net.sourcebot.api.response.InfoResponse
import net.sourcebot.api.command.Command
import net.sourcebot.api.command.RootCommand
import net.sourcebot.api.command.argument.Argument
import net.sourcebot.api.command.argument.ArgumentInfo
import net.sourcebot.api.command.argument.Arguments

class OngCommand : RootCommand() {
    override val name = "ong"
    override val description = "Use the Ong language."
    override val permission = "cryptography.$name"

    init {
        addChildren(
            OngEncodeCommand(),
            OngDecodeCommand()
        )
    }

    private inner class OngEncodeCommand : Command() {
        override val name = "encode"
        override val description = "Encode using the Ong language."
        override val argumentInfo = ArgumentInfo(
            Argument("input", "The text to encode into Ong.")
        )

        override fun execute(message: Message, args: Arguments): Response {
            val input = args.slurp(" ", "You did not specify text to encode!")
            val encoded = input.replace("([^aeiou\\W\\d])".toRegex(), "$1ong")
            return InfoResponse("Ong Encode Result", encoded)
        }
    }

    private inner class OngDecodeCommand : Command() {
        override val name = "decode"
        override val description = "Decode from the Ong language."
        override val argumentInfo = ArgumentInfo(
            Argument("input", "The text to decode from Ong.")
        )

        override fun execute(message: Message, args: Arguments): Response {
            val input = args.slurp(" ", "You did not specify text to decode!")
            val decoded = input.replace("ong", "")
            return InfoResponse("Ong Decode Result", decoded)
        }
    }
}