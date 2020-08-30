package net.sourcebot.api.module

import net.sourcebot.Source
import net.sourcebot.api.configuration.JsonSerial
import net.sourcebot.api.configuration.Properties
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Files

abstract class SourceModule {
    val logger: Logger = LoggerFactory.getLogger(javaClass)

    lateinit var classLoader: ModuleClassLoader
        internal set

    lateinit var descriptor: ModuleDescriptor
        internal set

    val name by lazy { descriptor.name }
    val version by lazy { descriptor.version }
    val description by lazy { descriptor.description }
    val author by lazy { descriptor.author }

    var enabled = false
        internal set

    val dataFolder: File by lazy {
        File("modules", descriptor.name).apply {
            if (!exists()) mkdirs()
        }
    }

    val config: Properties by lazy {
        val file = File(dataFolder, "config.json")
        if (!file.exists()) saveResource("/config.json")
        return@lazy file.let { JsonSerial.mapper.readValue(it, Properties::class.java) }
    }

    fun saveResource(absPath: String): File {
        if (!absPath.startsWith("/")) throw IllegalArgumentException("Resource path must be absolute!")
        val relPath = absPath.substring(1)
        return this::class.java.getResourceAsStream(absPath).use {
            if (it == null) throw IllegalStateException("Could not locate '$relPath' in module JAR!")
            val asPath = dataFolder.toPath().resolve(relPath)
            dataFolder.mkdirs()
            Files.copy(it, asPath)
            asPath.toFile()
        }
    }

    fun load(source: Source, postLoad: () -> Unit) {
        onLoad(source)
        postLoad()
        logger.info("Loaded $name v$version by $author.")
    }

    /**
     * Fired when this Module is being loaded; before it is enabled.
     * Methods in this scope should not utilize API from other Modules.
     */
    open fun onLoad(source: Source) = Unit

    fun enable(source: Source, postEnable: () -> Unit) {
        onEnable(source)
        postEnable()
        logger.info("Enabled $name v${version}.")
    }

    /**
     * Fired when this Module is being enabled, after it is loaded.
     */
    open fun onEnable(source: Source) = Unit

    fun disable(source: Source, postDisable: () -> Unit) {
        onDisable(source)
        postDisable()
        logger.info("Disabled $name v${version}.")
    }

    /**
     * Fired when this Module is being disabled, before it is unloaded.
     */
    open fun onDisable(source: Source) = Unit
}