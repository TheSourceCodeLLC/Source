package net.sourcebot.module.cryptography.commands.digest

import com.mongodb.internal.HexUtils
import net.dv8tion.jda.api.entities.Message
import net.sourcebot.api.alert.Alert
import net.sourcebot.api.alert.InfoAlert
import net.sourcebot.api.command.RootCommand
import net.sourcebot.api.command.argument.Argument
import net.sourcebot.api.command.argument.ArgumentInfo
import net.sourcebot.api.command.argument.Arguments
import java.security.MessageDigest

abstract class HashCommand(
    private val algorithm: String
) : RootCommand() {
    private val digest = MessageDigest.getInstance(algorithm)
    override val description = "Hash text using $algorithm."
    override val argumentInfo = ArgumentInfo(
        Argument("input", "The text to be hashed.")
    )
    final override val permission by lazy { "cryptography.$name" }

    override fun execute(message: Message, args: Arguments): Alert {
        val input = args.slurp(" ", "You did not provide text to hash!")
        val digested = digest.digest(input.toByteArray())
        val hexStr = HexUtils.toHex(digested)
        return InfoAlert("$algorithm Hash Result", hexStr)
    }
}