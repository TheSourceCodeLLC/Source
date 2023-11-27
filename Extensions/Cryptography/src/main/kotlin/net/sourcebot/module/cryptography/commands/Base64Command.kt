package net.sourcebot.module.cryptography.commands

import me.hwiggy.kommander.arguments.Adapter
import me.hwiggy.kommander.arguments.Arguments
import me.hwiggy.kommander.arguments.Synopsis
import net.dv8tion.jda.api.entities.Message
import net.sourcebot.api.command.RootCommand
import net.sourcebot.api.command.SourceCommand
import net.sourcebot.api.response.Response
import net.sourcebot.api.response.StandardInfoResponse
import java.util.*

class Base64Command : RootCommand() {
    override val name = "base64"
    override val description = "Use Base64."
    override val permission = "cryptography.$name"

    init {
        register(
            Base64EncodeCommand(),
            Base64DecodeCommand()
        )
    }

    private class Base64EncodeCommand : SourceCommand() {
        private val encoder = Base64.getEncoder()
        override val name = "encode"
        override val description = "Encode using Base64."
        override val permission by lazy { "${parent!!.permission}.${name}" }
        override val synopsis = Synopsis {
            reqParam("input", "The text to encode into Base64.", Adapter.slurp(" "))
        }

        override fun execute(sender: Message, arguments: Arguments.Processed): Response {
            val input = arguments.required<String>("input", "You did not specify text to encode!")
            val encoded = encoder.encodeToString(input.toByteArray())
            return StandardInfoResponse("Base64 Encode Result", encoded)
        }
    }

    private class Base64DecodeCommand : SourceCommand() {
        private val decoder = Base64.getDecoder()
        override val name = "decode"
        override val description = "Decode using Base64."
        override val synopsis = Synopsis {
            reqParam("input", "The text to decode from Base64.", Adapter.slurp(" "))
        }

        override val permission by lazy { "${parent!!.permission}.${name}" }

        override fun execute(sender: Message, arguments: Arguments.Processed): Response {
            val input = arguments.required<String>("input", "You did not specify text to decode!")
            val decoded = String(decoder.decode(input))
            return StandardInfoResponse("Base64 Decode Result", decoded)
        }
    }
}