package net.sourcebot.api.module

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.util.concurrent.ConcurrentHashMap

@Suppress("DEPRECATION", "UNCHECKED_CAST", "UNUSED")
class ModuleHandler : ClassLoader() {
    private val logger: Logger = LoggerFactory.getLogger(ModuleHandler::class.java)
    private val classCache = ConcurrentHashMap<String, Class<*>>()
    val loader = ModuleLoader(this)

    fun findClass(
        name: String, loader: JarModuleClassLoader
    ): Class<*> = classCache[name] ?: try {
        loader.findClass(name, false)
    } catch (ex: Exception) {
        null
    }?.also { classCache[name] = it } ?: throw ClassNotFoundException(name)

    fun moduleExists(name: String) = loader.findExtension(name) != null
    inline fun <reified T : SourceModule> getExtension() = loader.getExtension(T::class.java)

    public override fun findClass(name: String): Class<*> {
        val cached = classCache[name]
        if (cached != null) return cached
        var found: Class<*>? = null
        for (it in loader.getExtensions().map(SourceModule::classLoader)) {
            try {
                found = it.findClass(name, false); break
            } catch (ex: Exception) {
            }
        }
        return found?.also { classCache[name] = it } ?: throw ClassNotFoundException(name)
    }

    fun loadAndEnable(folder: File) = loadModules(folder).also {
        logger.info("Enabling modules from '${folder.path}'...")
        it.forEach(this::enableModule)
    }

    fun loadAndEnable(module: SourceModule) {
        loadModule(module)
        enableModule(module)
    }

    private fun loadModules(folder: File) = loader.loadExtensions(folder) { it.extension.equals("jar", true) }
    private fun loadModule(file: File) = loader.loadExtension(file)

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