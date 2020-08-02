package net.sourcebot.module.cryptography.commands.digest

class MD5Command : HashCommand("MD5") {
    override val name = "md5"
    override val permission = "cryptography.$name"
}