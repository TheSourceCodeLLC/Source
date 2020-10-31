package net.sourcebot.api.module

import net.sourcebot.Source
import net.sourcebot.api.command.RootCommand
import net.sourcebot.api.configuration.ConfigurationInfo
import net.sourcebot.api.configuration.JsonConfiguration
import net.sourcebot.api.event.EventSubscriber
import net.sourcebot.api.module.exception.ModuleLifecycleException
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

    val config: JsonConfiguration by lazy {
        val file = File(dataFolder, "config.json")
        if (!file.exists()) saveResource("/config.json")
        return@lazy file.let(JsonConfiguration::fromFile)
    }

    open val configurationInfo: ConfigurationInfo? = null

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

    fun load(postLoad: () -> Unit) = try {
        onLoad()
        postLoad()
        logger.info("Loaded $name v$version by $author.")
    } catch (err: Throwable) {
        throw ModuleLifecycleException(name, err)
    }

    /**
     * Fired when this Module is being loaded; before it is enabled.
     * Methods in this scope should not utilize API from other Modules.
     */
    open fun onLoad() = Unit

    fun enable(postEnable: () -> Unit) = try {
        onEnable()
        postEnable()
        logger.info("Enabled $name v${version}.")
    } catch (err: Throwable) {
        throw ModuleLifecycleException(name, err)
    }

    /**
     * Fired when this Module is being enabled, after it is loaded.
     */
    open fun onEnable() = Unit

    fun disable(postDisable: () -> Unit) = try {
        onDisable()
        postDisable()
        logger.info("Disabled $name v${version}.")
    } catch (err: Throwable) {
        throw ModuleLifecycleException(name, err)
    }

    /**
     * Fired when this Module is being disabled, before it is unloaded.
     */
    open fun onDisable() = Unit

    fun registerCommands(vararg commands: RootCommand) {
        Source.COMMAND_HANDLER.registerCommands(this, *commands)
    }

    @Suppress("UNCHECKED_CAST")
    fun <M : SourceModule> subscribeEvents(
        vararg subscribers: EventSubscriber<M>
    ) = subscribers.forEach {
        it.subscribe(
            this as M,
            Source.JDA_EVENTS,
            Source.SOURCE_EVENTS
        )
    }
}