package net.sourcebot.module.cryptography.commands.digest

class SHA224Command : HashCommand("SHA-224") {
    override val name = "sha224"
    override val permission = "cryptography.$name"
}