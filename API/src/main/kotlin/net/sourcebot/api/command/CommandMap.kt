package net.sourcebot.api.command

class CommandMap<C : Command> {
    private val labels = HashMap<String, C>()
    private val aliases = HashMap<String, C>()

    fun register(command: C) {
        labels[command.name] = command
        command.aliases.forEach { aliases[it] = command }
    }

    fun unregister(command: C) {
        labels.entries.removeIf { it.value == command }
        aliases.entries.removeIf { it.value == command }
    }

    operator fun get(identifier: String) = labels[identifier] ?: aliases[identifier]

    fun getCommands(): Collection<C> = labels.values
    fun getCommandNames(): Collection<String> = labels.keys

    fun removeIf(predicate: (C) -> Boolean) {
        labels.entries.removeIf { predicate(it.value) }
        aliases.entries.removeIf { predicate(it.value) }
    }
}