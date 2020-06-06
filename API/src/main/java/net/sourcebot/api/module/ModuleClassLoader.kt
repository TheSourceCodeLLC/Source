package net.sourcebot.api.module

import com.google.common.io.ByteStreams
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.slf4j.LoggerFactory
import java.io.File
import java.io.InputStreamReader
import java.net.URLClassLoader
import java.util.concurrent.ConcurrentHashMap
import java.util.jar.JarFile

class ModuleClassLoader(
    file: File,
    private val moduleHandler: ModuleHandler
) : URLClassLoader(
    arrayOf(file.toURI().toURL()), moduleHandler
) {
    internal val moduleDescription: ModuleDescription = getResourceAsStream("module.json").use {
        if (it == null) throw InvalidModuleException("Could not find module.json!")
        else JsonParser.parseReader(InputStreamReader(it)) as JsonObject
    }.let(::ModuleDescription)

    internal val classes = ConcurrentHashMap<String, Class<*>>()
    private val jar = JarFile(file)

    @JvmOverloads
    fun findClass(name: String, searchParent: Boolean = true): Class<*> = classes.computeIfAbsent(name) {
        var found: Class<*>? = null
        if (searchParent) found = moduleHandler.findClass(name)
        if (found == null) {
            val className = name.replace(".", "/").plus(".class")
            val entry = jar.getJarEntry(className) ?: throw ClassNotFoundException(name)
            val classBytes = try {
                jar.getInputStream(entry).use(ByteStreams::toByteArray)
            } catch (ex: Exception) {
                throw ClassNotFoundException(name, ex)
            }
            found = defineClass(name, classBytes, 0, classBytes.size)
        }
        found ?: throw ClassNotFoundException(name)
    }

    override fun close() {
        jar.close()
        classes.clear()
        super.close()
    }

    fun initialize(): SourceModule {
        val main = moduleDescription.main
        val mainClass = Class.forName(main, true, this)
        val moduleClass = mainClass.asSubclass(SourceModule::class.java)
        val module = moduleClass.newInstance()
        module.classLoader = this
        module.source = moduleHandler.source
        module.logger = LoggerFactory.getLogger(mainClass)
        return module
    }
}