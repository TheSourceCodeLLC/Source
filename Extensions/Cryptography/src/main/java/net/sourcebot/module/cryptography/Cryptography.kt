package net.sourcebot.module.cryptography

import net.sourcebot.Source
import net.sourcebot.api.module.SourceModule
import net.sourcebot.module.cryptography.commands.Base64Command
import net.sourcebot.module.cryptography.commands.OngCommand
import net.sourcebot.module.cryptography.commands.digest.*

class Cryptography : SourceModule() {
    override fun onEnable(source: Source) {
        source.commandHandler.registerCommands(
            this,
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