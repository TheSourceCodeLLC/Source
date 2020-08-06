package net.sourcebot.api.module

import com.google.common.io.ByteStreams
import java.io.Closeable
import java.io.File
import java.io.InputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.jar.JarFile

abstract class ModuleClassLoader : ClassLoader() {
    abstract fun findClass(name: String, searchParent: Boolean): Class<*>
    final override fun findClass(name: String): Class<*> = findClass(name, true)
}

class JarModuleClassLoader(
    private val moduleHandler: ModuleHandler,
    val file: File
) : ModuleClassLoader(), Closeable {
    private val jar = JarFile(file)

    private val classes = ConcurrentHashMap<String, Class<*>>()

    override fun findClass(name: String, searchParent: Boolean): Class<*> = classes.computeIfAbsent(name) {
        val className = name.replace(".", "/").plus(".class")
        try {
            var storedClass = jar.getJarEntry(className)?.let {
                jar.getInputStream(it)?.use(ByteStreams::toByteArray)
            }?.let { defineClass(name, it, 0, it.size) }
            if (storedClass == null && searchParent) storedClass = moduleHandler.findClass(name)
            storedClass
        } catch (ex: Exception) {
            null
        } ?: throw ClassNotFoundException(name)
    }

    override fun getResourceAsStream(name: String): InputStream? =
        jar.getJarEntry(name).let(jar::getInputStream)

    override fun close() {
        jar.close()
        classes.clear()
    }
}