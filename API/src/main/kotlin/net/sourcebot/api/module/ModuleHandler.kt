package net.sourcebot.api.module

import com.fasterxml.jackson.databind.node.ObjectNode
import net.sourcebot.Source
import net.sourcebot.api.configuration.JsonSerial
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileFilter
import java.util.concurrent.ConcurrentHashMap
import java.util.jar.JarFile

class ModuleHandler(
    private val source: Source
) : ClassLoader() {
    private val logger: Logger = LoggerFactory.getLogger(ModuleHandler::class.java)
    private val classes = ConcurrentHashMap<String, Class<*>>()
    private val moduleIndex = HashMap<String, SourceModule>()

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
            JsonSerial.mapper.readTree(it) as ObjectNode
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

    fun loadModule(file: File): SourceModule = try {
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

    fun loadModule(
        module: SourceModule
    ) = module.load(source) { moduleIndex[module.name] = module }

    fun enableModule(
        module: SourceModule
    ) = module.enable(source) { module.enabled = true }

    fun disableModule(
        module: SourceModule
    ) = module.disable(source) { module.enabled = false }

    fun findModule(name: String): SourceModule? = moduleIndex.values.find {
        it.name.startsWith(name, true)
    }

    fun getModule(name: String): SourceModule? = moduleIndex[name]
    fun getModules() = moduleIndex.values
}

class InvalidModuleException(message: String) : Exception(message)
class UnknownDependencyException(
    dependencies: Set<String>
) : RuntimeException("Unknown Dependencies: ${dependencies.joinToString()}")

class AmbiguousModuleException(
    name: String,
    firstIndexed: File,
    lastIndexed: File
) : RuntimeException("Module '$name' from ${firstIndexed.path} is duplicated by ${lastIndexed.path}!")

class ModuleLoadException(
    file: File,
    err: Throwable
) : RuntimeException("Could not load module '${file.path}'", err)