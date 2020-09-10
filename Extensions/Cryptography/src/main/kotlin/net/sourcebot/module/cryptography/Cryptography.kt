package net.sourcebot.module.cryptography

import net.sourcebot.api.module.SourceModule
import net.sourcebot.module.cryptography.commands.*

class Cryptography : SourceModule() {
    override fun onEnable() {
        registerCommands(
            Base64Command(),
            OngCommand(),
            MD2Command(),
            MD5Command(),
            SHACommand(),
            SHA224Command(),
            SHA256Command(),
            SHA384Command(),
            SHA512Command()
        )
    }
}