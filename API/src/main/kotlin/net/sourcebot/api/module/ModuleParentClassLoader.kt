package net.sourcebot.api.module

import me.hwiggy.extensible.binding.jvm.classloader.JarParentClassLoader
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File

@Suppress("DEPRECATION", "UNCHECKED_CAST", "UNUSED")
class ModuleParentClassLoader(
    parent: ClassLoader
) : JarParentClassLoader<ModuleDescriptor, SourceModule>(parent) {
    private val logger: Logger = LoggerFactory.getLogger(ModuleParentClassLoader::class.java)
    override val loader = ModuleLoader(this)

    fun moduleExists(name: String) = loader.findExtension(name) != null
    inline fun <reified T : SourceModule> getExtension() = loader.getExtension(T::class.java)

    fun loadAndEnable(folder: File) = loadModules(folder).also {
        logger.info("Enabling modules from '${folder.path}'...")
        it.forEach(this::enableModule)
    }

    fun loadAndEnable(module: SourceModule) {
        loadModule(module)
        enableModule(module)
    }

    private fun loadModules(folder: File) = loader.loadExtensions(folder) { it.extension.equals("jar", true) }
    fun loadModule(file: File) = loader.loadExtension(file)

    /**
     * Manually indexes a module.
     * Should only be used by the base module.
     */
    fun index(module: SourceModule) {
        loader.indexExtension(module.name, module)
    }

    private fun loadModuleThrowing(
        module: SourceModule
    ) = module.load()

    private fun loadModule(
        module: SourceModule
    ) = performLifecycle(module, ::loadModuleThrowing)

    private fun enableModuleThrowing(
        module: SourceModule
    ) = module.enable { module.enabled = true }

    private fun enableModule(
        module: SourceModule
    ) = performLifecycle(module, ::enableModuleThrowing)

    private fun disableModuleThrowing(
        module: SourceModule
    ) = module.disable { module.enabled = false }

    fun disableModule(
        module: SourceModule
    ) = performLifecycle(module, ::disableModuleThrowing)

    private fun performLifecycle(
        module: SourceModule,
        lifecycleTask: (SourceModule) -> Unit
    ) = try {
        lifecycleTask(module)
    } catch (err: Throwable) {
        err.printStackTrace()
    }
}