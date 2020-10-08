package net.sourcebot.module.cryptography.commands

import net.dv8tion.jda.api.entities.Message
import net.sourcebot.api.command.Command
import net.sourcebot.api.command.RootCommand
import net.sourcebot.api.command.argument.Argument
import net.sourcebot.api.command.argument.ArgumentInfo
import net.sourcebot.api.command.argument.Arguments
import net.sourcebot.api.response.Response
import net.sourcebot.api.response.StandardInfoResponse
import java.util.*

class Base64Command : RootCommand() {
    override val name = "base64"
    override val description = "Use Base64."
    override val permission = "cryptography.$name"

    init {
        addChildren(
            Base64EncodeCommand(),
            Base64DecodeCommand()
        )
    }

    private class Base64EncodeCommand : Command() {
        private val encoder = Base64.getEncoder()
        override val name = "encode"
        override val description = "Encode using Base64."
        override val argumentInfo = ArgumentInfo(
            Argument("input", "The text to encode in Base64.")
        )
        override val permission by lazy { "${parent!!.permission}.${name}" }

        override fun execute(message: Message, args: Arguments): Response {
            val input = args.slurp(" ", "You did not specify text to encode!")
            val encoded = encoder.encodeToString(input.toByteArray())
            return StandardInfoResponse("Base64 Encode Result", encoded)
        }
    }

    private class Base64DecodeCommand : Command() {
        private val decoder = Base64.getDecoder()
        override val name = "decode"
        override val description = "Decode using Base64."
        override val argumentInfo = ArgumentInfo(
            Argument("input", "The text to decode from Base64.")
        )
        override val permission by lazy { "${parent!!.permission}.${name}" }

        override fun execute(message: Message, args: Arguments): Response {
            val input = args.slurp(" ", "You did not specify text to decode!")
            val decoded = String(decoder.decode(input))
            return StandardInfoResponse("Base64 Decode Result", decoded)
        }
    }
}