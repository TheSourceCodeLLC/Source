package net.sourcebot.module.cryptography.commands.digest

class SHA384Command : HashCommand("SHA-384") {
    override val name = "sha384"
    override val permission = "cryptography.$name"
}