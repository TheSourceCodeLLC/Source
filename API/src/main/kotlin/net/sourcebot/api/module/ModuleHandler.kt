package net.sourcebot.api.module

import com.fasterxml.jackson.databind.node.ObjectNode
import net.sourcebot.api.configuration.JsonSerial
import net.sourcebot.api.module.exception.AmbiguousModuleException
import net.sourcebot.api.module.exception.InvalidModuleException
import net.sourcebot.api.module.exception.ModuleLoadException
import net.sourcebot.api.module.exception.UnknownDependencyException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileFilter
import java.util.concurrent.ConcurrentHashMap
import java.util.jar.JarFile

@Suppress("DEPRECATION", "UNCHECKED_CAST", "UNUSED")
class ModuleHandler : ClassLoader() {
    private val logger: Logger = LoggerFactory.getLogger(ModuleHandler::class.java)
    private val classCache = ConcurrentHashMap<String, Class<*>>()
    private val moduleIndex = HashMap<String, SourceModule>()

    fun getModules() = moduleIndex.values

    fun <T : SourceModule> findModule(
        name: String
    ) = moduleIndex.values.find { it.name.startsWith(name, true) } as T?

    fun <T : SourceModule> getModule(
        type: Class<T>
    ) = moduleIndex.values.find { it.javaClass == type } as T?

    inline fun <reified T : SourceModule> getModule() = getModule(T::class.java)

    public override fun findClass(
        name: String
    ): Class<*> {
        val cached = classCache[name]
        if (cached != null) return cached
        var found: Class<*>? = null
        for (it in moduleIndex.values.map(SourceModule::classLoader)) {
            try {
                found = it.findClass(name, false); break
            } catch (ex: Exception) {
            }
        }
        return found?.also { classCache[name] = it } ?: throw ClassNotFoundException(name)
    }

    private fun findClass(
        name: String,
        loader: JarModuleClassLoader
    ): Class<*> = classCache[name] ?: try {
        loader.findClass(name, false)
    } catch (ex: Exception) {
        null
    }?.also { classCache[name] = it } ?: throw ClassNotFoundException(name)

    private fun loadDescriptor(file: File): ModuleDescriptor = JarFile(file).use { jar ->
        jar.getJarEntry("module.json")?.let(jar::getInputStream)?.use {
            JsonSerial.mapper.readTree(it) as ObjectNode
        }
    }?.let(::ModuleDescriptor) ?: throw InvalidModuleException(
        "Module '${file.path}' does not contain module.json!"
    )

    fun loadAndEnable(
        folder: File
    ) = loadModules(folder).also {
        logger.info("Enabling modules from '${folder.path}'...")
        it.forEach(this::enableModule)
    }

    fun loadAndEnable(
        module: SourceModule
    ) {
        loadModule(module)
        enableModule(module)
    }

    private fun loadModules(folder: File): List<SourceModule> {
        logger.info("Loading modules from '${folder.path}'...")
        val loadOrder = ArrayList<File>()
        val fileIndex = HashMap<String, File>()
        val hardDependencies = HashMap<String, MutableSet<String>>()
        val softDependencies = HashMap<String, MutableSet<String>>()
        folder.listFiles(
            FileFilter { it.extension.equals("jar", true) }
        )?.forEach {
            try {
                val descriptor = loadDescriptor(it)
                fileIndex.compute(descriptor.name) { name, indexed ->
                    if (name.equals("Source", true))
                        throw InvalidModuleException("Illegal module name: 'Source' in ${it.path}!")
                    if (indexed == null) it
                    else throw AmbiguousModuleException(name, indexed, it)
                }
                hardDependencies[descriptor.name] = descriptor.hardDepends.toMutableSet()
                softDependencies[descriptor.name] = descriptor.softDepends.toMutableSet()
            } catch (ex: Throwable) {
                ex.printStackTrace()
            }
        }
        do {
            // Find modules with no hard dependencies, preferring ones with no soft dependencies
            // Make sure to exclude those that are already in the load order
            val next = fileIndex.filterValues { it !in loadOrder }.keys.filter {
                hardDependencies[it]?.isEmpty() ?: true
            }.sortedBy { softDependencies[it]?.size ?: 0 }
            // Build the module load order
            loadOrder.addAll(next.map { fileIndex[it]!! })
            // Filter the 'loaded' module from dependency index
            next.forEach {
                hardDependencies.remove(it)
                softDependencies.remove(it)
            }
            // Filter the 'loaded' module from dependency sets
            hardDependencies.values.forEach { it.removeAll(next) }
            softDependencies.values.forEach { it.removeAll(next) }
        } while (next.isNotEmpty())
        hardDependencies.forEach { (module, deps) ->
            logger.error("Could not load module '$module!'", UnknownDependencyException(deps))
        }
        return loadOrder.mapNotNull {
            try {
                loadModule(it)
            } catch (ex: Exception) {
                ex.printStackTrace(); null
            }
        }
    }

    private fun loadModule(file: File): SourceModule = try {
        val descriptor = loadDescriptor(file)
        val name = descriptor.name
        moduleIndex[name]?.let {
            if (name.equals("Source", true))
                throw InvalidModuleException("Illegal module name: 'Source' in ${file.path}!")
            throw AmbiguousModuleException(
                name,
                (it.classLoader as JarModuleClassLoader).file,
                file
            )
        }
        descriptor.hardDepends.toMutableSet()
            .apply { removeIf(moduleIndex::containsKey) }
            .let { if (it.isNotEmpty()) throw UnknownDependencyException(it) }
        val loader = JarModuleClassLoader(this, file)
        val mainClass = findClass(descriptor.main, loader)
        val module = mainClass.newInstance() as SourceModule
        module.apply {
            this.classLoader = loader
            this.descriptor = descriptor
            loadModule(this)
        }
    } catch (err: Throwable) {
        throw ModuleLoadException(file, err)
    }

    private fun loadModuleThrowing(
        module: SourceModule
    ) = module.load { moduleIndex[module.name] = module }

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