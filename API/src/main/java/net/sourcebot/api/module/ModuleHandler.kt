package net.sourcebot.api.module

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileFilter
import java.io.InputStreamReader
import java.util.concurrent.ConcurrentHashMap
import java.util.jar.JarFile

class ModuleHandler : ClassLoader() {
    private val logger: Logger = LoggerFactory.getLogger(ModuleHandler::class.java)
    private val classes = ConcurrentHashMap<String, Class<*>>()
    internal val moduleIndex = HashMap<String, SourceModule>()

    public override fun findClass(
        name: String
    ): Class<*> = classes.computeIfAbsent(name) {
        moduleIndex.values.map(SourceModule::classLoader).forEach {
            return@computeIfAbsent try {
                it.findClass(name, false)
            } catch (ex: Exception) {
                null
            } ?: return@forEach
        }
        throw ClassNotFoundException(name)
    }

    fun findClass(
        name: String,
        loader: JarModuleClassLoader
    ): Class<*> = classes.computeIfAbsent(name) { loader.findClass(name, false) }

    fun loadDescriptor(file: File): ModuleDescriptor = JarFile(file).use { jar ->
        jar.getJarEntry("module.json")?.let(jar::getInputStream)?.use {
            JsonParser.parseReader(InputStreamReader(it)) as JsonObject
        }
    }?.let(::ModuleDescriptor) ?: throw InvalidModuleException("JAR does not contain module.json!")

    fun loadModules(folder: File): List<SourceModule> {
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
                    if (indexed == null) it
                    else throw AmbiguousPluginException(name, indexed, it)
                }
                hardDependencies[descriptor.name] = descriptor.hardDepends.toMutableSet()
                softDependencies[descriptor.name] = descriptor.softDepends.toMutableSet()
            } catch (ex: Throwable) {
                ex.printStackTrace()
            }
        }
        do {
            // Find plugins with no hard dependencies, preferring ones with no soft dependencies
            // Make sure to exclude those that are already in the load order
            val next = fileIndex.filterValues { it !in loadOrder }.keys.filter {
                hardDependencies[it]?.isEmpty() ?: true
            }.sortedBy { softDependencies[it]?.size ?: 0 }
            // Build the plugin load order
            loadOrder.addAll(next.map { fileIndex[it]!! })
            // Filter the 'loaded' plugin from dependency index
            next.forEach {
                hardDependencies.remove(it)
                softDependencies.remove(it)
            }
            // Filter the 'loaded' plugin from dependency sets
            hardDependencies.values.forEach { it.removeAll(next) }
            softDependencies.values.forEach { it.removeAll(next) }
        } while (next.isNotEmpty())
        hardDependencies.forEach { (module, deps) ->
            logger.error("Could not load module '$module!'", UnknownDependencyException(deps))
        }
        return loadOrder.map { loadPlugin(it) }
    }

    fun loadPlugin(file: File): SourceModule {
        val descriptor = loadDescriptor(file)
        val name = descriptor.name
        moduleIndex[name]?.let {
            throw AmbiguousPluginException(
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
        val plugin = mainClass.newInstance() as SourceModule
        return plugin.apply {
            val (_, version, _, author) = descriptor
            this.classLoader = loader
            this.descriptor = descriptor

            onLoad()
            moduleIndex[name] = this
            logger.info("Loaded $name v$version by $author.")
        }
    }

    fun enableModule(plugin: SourceModule) {
        plugin.onEnable()
        plugin.enabled = true
        logger.info("Enabled ${plugin.name} v${plugin.version}.")
    }

    fun disableModule(plugin: SourceModule) {
        plugin.onDisable()
        plugin.enabled = false
        logger.info("Disabled ${plugin.name} v${plugin.version}.")
    }

    fun findModule(name: String): SourceModule? = moduleIndex.values.find {
        it.name.startsWith(name, true)
    }

    fun getModule(name: String): SourceModule? = moduleIndex[name]
    fun getModules() = moduleIndex.values
}