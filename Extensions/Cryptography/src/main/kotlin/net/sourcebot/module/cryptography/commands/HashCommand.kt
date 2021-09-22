package net.sourcebot.module.cryptography.commands

import com.mongodb.internal.HexUtils
import me.hwiggy.kommander.arguments.Adapter
import me.hwiggy.kommander.arguments.Arguments
import me.hwiggy.kommander.arguments.Synopsis
import net.dv8tion.jda.api.entities.Message
import net.sourcebot.api.command.RootCommand
import net.sourcebot.api.response.Response
import net.sourcebot.api.response.StandardInfoResponse
import java.security.MessageDigest

abstract class HashCommand(
    final override val name: String,
    private val algorithm: String
) : RootCommand() {
    private val digest = MessageDigest.getInstance(algorithm)
    override val description = "Hash text using $algorithm."
    override val synopsis = Synopsis {
        reqParam("input", "The text to be hashed.", Adapter.slurp(" "))
    }

    final override val permission = "cryptography.$name"

    override fun execute(sender: Message, arguments: Arguments.Processed): Response {
        val input = arguments.required<String>("input", "You did not provide text to hash!")
        val digested = digest.digest(input.toByteArray())
        val hexStr = HexUtils.toHex(digested)
        return StandardInfoResponse("$algorithm Hash Result", hexStr)
    }
}

class MD2Command : HashCommand("md2", "MD2")
class MD5Command : HashCommand("md5", "MD5")
class SHACommand : HashCommand("sha", "SHA")
class SHA224Command : HashCommand("sha224", "SHA-224")
class SHA256Command : HashCommand("sha256", "SHA-256")
class SHA384Command : HashCommand("sha384", "SHA-384")
class SHA512Command : HashCommand("sha512", "SHA-512")