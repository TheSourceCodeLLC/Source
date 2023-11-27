package net.sourcebot.api.module

import me.hwiggy.extensible.binding.jvm.contract.JarExtension
import net.sourcebot.Source
import net.sourcebot.api.command.RootCommand
import net.sourcebot.api.configuration.ConfigurationInfo
import net.sourcebot.api.configuration.JsonConfiguration
import net.sourcebot.api.event.EventSubscriber
import net.sourcebot.api.insert
import net.sourcebot.api.module.exception.ModuleLifecycleException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.net.URI
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes

abstract class SourceModule : JarExtension<ModuleDescriptor>() {
    val logger: Logger = LoggerFactory.getLogger(javaClass)

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
        if (!file.exists()) saveResource("config.json")
        return@lazy file.let(JsonConfiguration::fromFile)
    }

    open val configurationInfo = ConfigurationInfo.EMPTY

    fun saveResource(source: String, target: String = source) {
        val targetPath = dataFolder.toPath().resolve(target)
        var jarRoot = this::class.java.getResource("").toString().replace("\\", "/")
        if (jarRoot[9] != '/') {
            jarRoot = jarRoot.insert(9, "/")
        }
        val extFs = dataFolder.toPath().fileSystem
        val fileSystem = FileSystems.newFileSystem(URI(jarRoot), emptyMap<String, String>())
        val sourceFolder = fileSystem.getPath(source)
        Files.walkFileTree(sourceFolder, object : SimpleFileVisitor<Path>() {
            override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult {
                Files.createDirectories(targetPath.resolve(pathTransform(extFs, sourceFolder.relativize(dir))))
                return FileVisitResult.CONTINUE
            }

            override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                Files.copy(file, targetPath.resolve(pathTransform(extFs, sourceFolder.relativize(file))))
                return FileVisitResult.CONTINUE
            }
        })
    }

    private fun pathTransform(fs: FileSystem, path: Path): Path {
        var ret = fs.getPath(if (path.isAbsolute) fs.separator else "")
        path.forEach { component ->
            ret = ret.resolve(component.fileName?.toString() ?: "")
        }
        return ret
    }

    fun load(postLoad: () -> Unit) = try {
        load()
        postLoad()
        logger.info("Loaded $name v$version by $author.")
    } catch (err: Throwable) {
        throw ModuleLifecycleException(name, err)
    }

    fun enable(postEnable: () -> Unit) = try {
        enable()
        postEnable()
        logger.info("Enabled $name v${version}.")
    } catch (err: Throwable) {
        throw ModuleLifecycleException(name, err)
    }

    fun disable(postDisable: () -> Unit) = try {
        disable()
        postDisable()
        logger.info("Disabled $name v${version}.")
    } catch (err: Throwable) {
        throw ModuleLifecycleException(name, err)
    }

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