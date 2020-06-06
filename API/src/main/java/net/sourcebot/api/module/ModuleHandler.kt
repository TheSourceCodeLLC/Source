package net.sourcebot.api.module

import net.sourcebot.Source
import net.sourcebot.api.database.MongoSerial
import net.sourcebot.api.properties.JsonSerial
import java.io.File
import java.util.concurrent.ConcurrentHashMap

class ModuleHandler(internal val source: Source) : ClassLoader() {
    private val logger = Source.logger
    private val classes = ConcurrentHashMap<String, Class<*>>()

    private val loaderIndex = HashMap<String, ModuleClassLoader>()
    private val moduleIndex = HashMap<String, SourceModule>()

    fun indexModule(file: File): String? = try {
        val moduleClassLoader = ModuleClassLoader(file, this)
        val moduleDescription = moduleClassLoader.moduleDescription
        val (name, version, description, author) = moduleDescription

        if (loaderIndex[name] != null) throw InvalidModuleException("Duplicate module '$name' !")
        loaderIndex[name] = moduleClassLoader

        logger.debug("Found '$name v$version: $description' by $author")
        name
    } catch (ex: Exception) {
        logger.error("Could not load module '${file.path}'!", ex)
        null
    }

    fun loadModule(name: String): SourceModule? {
        if (moduleIndex[name] != null) return moduleIndex[name]
        val loader = loaderIndex[name] ?: return null
        val description = loader.moduleDescription
        description.hardDepends.filter { it !in moduleIndex }.forEach {
            if (loadModule(it) == null) throw UnknownDependencyException("$name depends on unresolved dependency $it")
        }
        description.softDepends.filter { it !in moduleIndex }.forEach { loadModule(it) }
        val module = loader.initialize()
        moduleIndex[name] = module
        module.logger.info("Loading $name v${description.version}")
        module.onLoad(source)
        return module
    }

    fun unloadModule(name: String) {
        val loader = loaderIndex.remove(name)
        if (loader != null) {
            moduleIndex.remove(name)?.let {
                source.jdaEventSystem.unregister(it)
                source.sourceEventSystem.unregister(it)
                source.commandHandler.unregister(it)
                disableModule(it)
                it.onUnload()
            }
            loader.classes.values.forEach {
                JsonSerial.unregister(it)
                MongoSerial.unregister(it)
            }
            classes.entries.removeAll(loader.classes.entries)
            loader.close()
        }
        logger.debug("Unloaded module '$name'")
    }

    fun enableModule(module: SourceModule) {
        if (module.enabled) return
        val (name, version) = module.moduleDescription
        module.logger.info("Enabling $name v$version")
        module.enabled = true
    }

    fun disableModule(module: SourceModule) {
        if (!module.enabled) return
        val (name, version) = module.moduleDescription
        module.logger.info("Disabling $name v$version")
        module.enabled = false
    }

    public override fun findClass(name: String): Class<*> = classes.computeIfAbsent(name) {
        loaderIndex.values.forEach {
            val found = try {
                it.findClass(name, false)
            } catch (ex: Exception) {
                null
            }
            if (found != null) return@computeIfAbsent found
        }
        throw ClassNotFoundException(name)
    }

    fun getModules(): Collection<SourceModule> = moduleIndex.values
    fun getModule(name: String): SourceModule? = moduleIndex.values.firstOrNull {
        it.moduleDescription.name.startsWith(name, true)
    }
}