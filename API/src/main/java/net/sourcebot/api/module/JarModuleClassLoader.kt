package net.sourcebot.api.module

import com.google.common.io.ByteStreams
import java.io.Closeable
import java.io.File
import java.io.InputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.jar.JarFile

abstract class ModuleClassLoader : ClassLoader() {
    final override fun findClass(name: String): Class<*> = findClass(name, true)
    abstract fun findClass(name: String, searchParent: Boolean): Class<*>
}

class JarModuleClassLoader(
    private val moduleHandler: ModuleHandler,
    val file: File
) : ModuleClassLoader(), Closeable {
    private val jar = JarFile(file)

    private val classes = ConcurrentHashMap<String, Class<*>>()

    override fun findClass(name: String, searchParent: Boolean): Class<*> {
        val computed = classes[name]
        if (computed != null) return computed
        val className = name.replace(".", "/").plus(".class")
        val defined = try {
            jar.getJarEntry(className)?.let {
                jar.getInputStream(it)?.use(ByteStreams::toByteArray)
            }?.let { defineClass(name, it, 0, it.size) }
        } catch (ex: Exception) {
            try {
                if (searchParent) moduleHandler.findClass(name) else null
            } catch (ex: Exception) {
                null
            }
        } ?: throw ClassNotFoundException(name)
        classes[name] = defined
        return defined
    }

    override fun getResourceAsStream(name: String): InputStream? =
        jar.getJarEntry(name).let(jar::getInputStream)

    override fun close() {
        jar.close()
        classes.clear()
    }
}