package net.sourcebot.api.module

import net.sourcebot.Source
import net.sourcebot.api.command.RootCommand
import org.slf4j.Logger
import kotlin.properties.Delegates

abstract class SourceModule {
    internal lateinit var classLoader: ModuleClassLoader
    internal lateinit var source: Source
    lateinit var logger: Logger
        internal set

    val moduleDescription: ModuleDescription by lazy { classLoader.moduleDescription }

    var enabled: Boolean by Delegates.observable(false) { _, _, enable ->
        if (enable) onEnable(source) else onDisable()
    }
        internal set

    /**
     * Fired when this Module is being loaded; before it is enabled.
     * Methods in this scope should not utilize API from other Modules.
     */
    open fun onLoad(source: Source) = Unit

    /**
     * Fired when this Module is being unloaded; after it is disabled.
     */
    open fun onUnload() = Unit

    /**
     * Fired when this Module is being enabled, after it is loaded.
     */
    open fun onEnable(source: Source) = Unit

    /**
     * Fired when this Module is being disabled, before it is unloaded.
     */
    open fun onDisable() = Unit

    fun registerCommands(vararg command: RootCommand) {
        command.forEach {
            source.commandHandler.registerCommand(this, it)
        }
    }
}