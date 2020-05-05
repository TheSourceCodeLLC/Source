package net.sourcebot.base

import net.dv8tion.jda.api.entities.Message
import net.sourcebot.api.alert.Alert
import net.sourcebot.api.alert.InfoAlert
import net.sourcebot.api.command.Command
import net.sourcebot.api.command.argument.Argument
import net.sourcebot.api.command.argument.ArgumentInfo
import net.sourcebot.api.command.argument.Arguments

class OngCommand : Command() {
    override val name = "ong"
    override val description = "Use the Ong language."
    override val argumentInfo = ArgumentInfo(
        Argument("encode|decode", "The mode you wish to use Ong with.")
    )

    private class OngEncodeCommand : Command() {
        override val name = "encode"
        override val description = "Encode an input string into Ong."
        override val argumentInfo = ArgumentInfo(
            Argument("input", "The string to encode into Ong.")
        )
        override var cleanupResponse = false
        override fun execute(message: Message, args: Arguments): Alert {
            val input = args.slurp(" ", "Missing argument for parameter `input`!")
            val output = input.replace("([^\\W\\daeiou])".toRegex(), "$1ong")
            return InfoAlert("Ong Encode Result:", output)
        }
    }

    private class OngDecodeCommand : Command() {
        override val name = "decode"
        override val description = "Decode an input string out of Ong."
        override val argumentInfo = ArgumentInfo(
            Argument("input", "The string to decode from Ong.")
        )
        override var cleanupResponse = false
        override fun execute(message: Message, args: Arguments): Alert {
            val input = args.slurp(" ", "Missing argument for parameter `input`!")
            val output = input.replace("ong", "")
            return InfoAlert("Ong Decode Result:", output)
        }
    }

    init {
        addChild(OngEncodeCommand())
        addChild(OngDecodeCommand())
    }
}