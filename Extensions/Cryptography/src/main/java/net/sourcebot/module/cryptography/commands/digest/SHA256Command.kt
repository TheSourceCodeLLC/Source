package net.sourcebot.module.cryptography.commands.digest

class SHA256Command : HashCommand("SHA-256") {
    override val name = "sha256"
    override val permission = "cryptography.$name"
}