package net.sourcebot.module.cryptography.commands.digest

class SHACommand : HashCommand("SHA") {
    override val name = "sha"
    override val permission = "cryptography.$name"
}