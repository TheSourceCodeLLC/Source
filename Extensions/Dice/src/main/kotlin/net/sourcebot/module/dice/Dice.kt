package net.sourcebot.module.dice

import net.sourcebot.api.module.SourceModule

class Dice : SourceModule() {
    override fun onEnable() {
        registerCommands(
            RollCommand()
        )
    }
}