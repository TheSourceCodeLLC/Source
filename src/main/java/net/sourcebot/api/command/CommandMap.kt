package net.sourcebot.api.command

class CommandMap {
    private val labels = HashMap<String, Command>()
    private val aliases = HashMap<String, Command>()

    fun register(command: Command) {
        labels[command.name] = command
        command.aliases.forEach { aliases[it] = command }
    }

    fun unregister(command: Command) {
        labels.remove(command.name)
        command.aliases.forEach { aliases.remove(it) }
    }

    operator fun get(identifier: String) = labels[identifier] ?: aliases[identifier]
}