package net.sourcebot.api.module

import me.hwiggy.extensible.AbstractLoader
import me.hwiggy.extensible.exception.CompositeException
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class ModuleLoader(moduleHandler: ModuleHandler) : AbstractLoader<ModuleDescriptor, SourceModule>() {
    private val logger: Logger = LoggerFactory.getLogger(ModuleLoader::class.java)
    override val strategy = ModuleLoadStrategy(moduleHandler)

    override fun findExtension(name: String) = extensionIndex.values.find {
        it.name.startsWith(name, true)
    }

    override fun <T : SourceModule> getExtension(type: Class<T>) =
        extensionIndex.values.find { it.javaClass == type } as? T?

    override fun getExtensions() = extensionIndex.values.toList()

    override fun handleUncaught(ex: Throwable) {
        when (ex) {
            is CompositeException -> logger.error(ex.message, ex.cause)
            else -> logger.error(ex.stackTraceToString())
        }
    }

    override fun permitExtension(name: String) = !name.equals("Source", true)

    /**
     * Override performLoad to call custom load method previously used to index the module.
     */
    override fun performLoad(extension: SourceModule) = extension.load {
        /* Empty block because this functionality has been taken over by Extensible */
    }
}