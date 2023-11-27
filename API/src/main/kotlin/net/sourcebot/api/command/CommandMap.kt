package net.sourcebot.api.command

class CommandMap<C : SourceCommand> {
    private val labels = HashMap<String, C>()
    private val aliases = HashMap<String, C>()

    fun register(command: C) {
        labels[command.name] = command
        command.aliases.forEach { aliases[it] = command }
    }

    operator fun get(identifier: String) = labels[identifier] ?: aliases[identifier]

    fun getCommands(): Collection<C> = labels.values
    fun getCommandNames(): Collection<String> = labels.keys

    fun removeIf(predicate: (C) -> Boolean) {
        labels.entries.removeIf { predicate(it.value) }
        aliases.entries.removeIf { predicate(it.value) }
    }

    fun find(predicate: (C) -> Boolean): C? = labels.values.find(predicate)
}