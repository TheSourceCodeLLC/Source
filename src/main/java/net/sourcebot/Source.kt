package net.sourcebot

import net.sourcebot.api.EventSubsystem
import net.sourcebot.api.command.CommandMap
import java.nio.file.Path

class Source {
    private val pluginsFolder = Path.of("plugins")
    val eventSubsystem = EventSubsystem()
    val commandMap = CommandMap()

    fun main() {

    }
}