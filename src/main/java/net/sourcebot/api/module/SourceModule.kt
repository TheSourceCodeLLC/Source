package net.sourcebot.api.module

import net.sourcebot.api.command.Command
import net.sourcebot.api.event.EventSubsystem

interface SourceModule {
    val name: String
    val description: String
    val commands: Set<Command>
        get() = emptySet()

    @JvmDefault
    fun registerEvents(eventSubsystem: EventSubsystem) = Unit
}