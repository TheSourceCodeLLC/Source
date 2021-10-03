package net.sourcebot.api.module

import me.hwiggy.extensible.binding.AbstractLoader
import me.hwiggy.extensible.exception.CompositeException
import net.sourcebot.api.getDeclaringArchive
import net.sourcebot.impl.BaseModule
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File

class ModuleLoader(parentClassLoader: ModuleParentClassLoader) : AbstractLoader<ModuleDescriptor, SourceModule>() {
    private val logger: Logger = LoggerFactory.getLogger(ModuleLoader::class.java)
    override val strategy = ModuleLoadStrategy(parentClassLoader)

    override fun findExtension(name: String) = extensionIndex.values.find {
        it.name.startsWith(name, true)
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : SourceModule> getExtension(type: Class<T>) =
        extensionIndex.values.find { it.javaClass == type } as? T?

    override fun getExtensions() = extensionIndex.values.toList()

    override fun handleUncaught(ex: Throwable) {
        when (ex) {
            is CompositeException -> logger.error(ex.message, ex.cause)
            else -> logger.error(ex.stackTraceToString())
        }
    }

    override fun permitExtension(
        file: File,
        descriptor: ModuleDescriptor
    ) = !(descriptor.name == "Source" && file != BaseModule::class.java.getDeclaringArchive())

    /**
     * Override performLoad to call custom load method previously used to index the module.
     */
    override fun performLoad(extension: SourceModule) = extension.load {
        /* Empty block because this functionality has been taken over by Extensible */
    }
}