package net.sourcebot.module.cryptography.commands

import me.hwiggy.kommander.arguments.Adapter
import me.hwiggy.kommander.arguments.Arguments
import me.hwiggy.kommander.arguments.Synopsis
import net.dv8tion.jda.api.entities.Message
import net.sourcebot.api.command.RootCommand
import net.sourcebot.api.command.SourceCommand
import net.sourcebot.api.response.Response
import net.sourcebot.api.response.StandardInfoResponse

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

    inner class OngEncodeCommand : SourceCommand() {
        override val name = "encode"
        override val description = "Encode using the Ong language."
        override val synopsis = Synopsis {
            reqParam("input", "The text to encode into Ong.", Adapter.slurp(" "))
        }
        override val permission by lazy { "${parent!!.permission}.$name" }

        override fun execute(sender: Message, arguments: Arguments.Processed): Response {
            val input = arguments.required<String>("input", "You did not specify text to encode!")
            val encoded = input.replace("([^aeiou\\W\\d])".toRegex(), "$1ong")
            return StandardInfoResponse("Ong Encode Result", encoded)
        }
    }

    inner class OngDecodeCommand : SourceCommand() {
        override val name = "decode"
        override val description = "Decode from the Ong language."
        override val synopsis = Synopsis {
            reqParam("input", "The text to decode from Ong.", Adapter.slurp(" "))
        }
        override val permission by lazy { "${parent!!.permission}.$name" }

        override fun execute(sender: Message, arguments: Arguments.Processed): Response {
            val input = arguments.required<String>("input", "You did not specify text to decode!")
            val decoded = input.replace("ong", "")
            return StandardInfoResponse("Ong Decode Result", decoded)
        }
    }
}