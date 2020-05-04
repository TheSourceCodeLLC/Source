package net.sourcebot.api.module

import net.sourcebot.api.command.Command

interface SourceModule {
    val name: String
    val description: String
    val commands: Set<Command>
        get() = emptySet()
}