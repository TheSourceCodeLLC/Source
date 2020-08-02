package net.sourcebot.api.module

import com.google.common.io.ByteStreams
import java.io.File
import java.io.InputStream
import java.net.URLClassLoader
import java.util.concurrent.ConcurrentHashMap
import java.util.jar.JarFile

class ModuleClassLoader(
    private val moduleHandler: ModuleHandler,
    val file: File
) : URLClassLoader(
    arrayOf(file.toURI().toURL()), moduleHandler
) {
    private val jar = JarFile(file)

    private val classes = ConcurrentHashMap<String, Class<*>>()

    @JvmOverloads
    fun findClass(name: String, searchParent: Boolean = true): Class<*> = classes.computeIfAbsent(name) {
        val className = name.replace(".", "/").plus(".class")
        val entry = jar.getJarEntry(className) ?: throw ClassNotFoundException(name)
        val classBytes = try {
            jar.getInputStream(entry).use(ByteStreams::toByteArray)
        } catch (ex: Exception) {
            throw ClassNotFoundException(name, ex)
        }
        var found = defineClass(name, classBytes, 0, classBytes.size)
        if (found == null && searchParent) found = moduleHandler.findClass(name)
        found ?: throw ClassNotFoundException(name)
    }

    override fun getResourceAsStream(name: String): InputStream? =
        jar.getJarEntry(name).let(jar::getInputStream)

    override fun close() {
        jar.close()
        classes.clear()
        super.close()
    }
}