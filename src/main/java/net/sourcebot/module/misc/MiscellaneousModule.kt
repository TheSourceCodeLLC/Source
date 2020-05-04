package net.sourcebot.module.misc

import net.sourcebot.api.module.SourceModule

class MiscellaneousModule : SourceModule {
    override val name = "Miscellaneous"
    override val description = "Commands that dont quite fit anywhere else."
    override val commands = setOf(
        EightBallCommand(),
        OngCommand()
    )
}