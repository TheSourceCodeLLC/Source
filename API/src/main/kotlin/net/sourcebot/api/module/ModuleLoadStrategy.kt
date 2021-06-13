package net.sourcebot.api.module

import com.fasterxml.jackson.databind.node.ObjectNode
import me.hwiggy.extensible.contract.LoadStrategy
import net.sourcebot.api.configuration.JsonSerial
import net.sourcebot.api.module.exception.InvalidModuleException
import java.io.File
import java.util.jar.JarFile

class ModuleLoadStrategy(private val moduleHandler: ModuleHandler) : LoadStrategy<ModuleDescriptor, SourceModule> {
    override fun loadExtension(source: File): SourceModule {
        val descriptor = readDescriptor(source)
        val loader = JarModuleClassLoader(moduleHandler, source)
        val mainClass = moduleHandler.findClass(descriptor.main, loader)
        val module = mainClass.newInstance() as SourceModule
        return module.apply {
            this.classLoader = loader
            this.lazySource = loader.file
            this.descriptor = descriptor
        }
    }

    override fun readDescriptor(source: File) = JarFile(source).use { jar ->
        jar.getJarEntry("module.json")?.let(jar::getInputStream)?.use {
            JsonSerial.mapper.readTree(it) as ObjectNode
        }
    }?.let(::ModuleDescriptor) ?: throw InvalidModuleException(
        "Module '${source.path}' does not contain module.json!"
    )
}