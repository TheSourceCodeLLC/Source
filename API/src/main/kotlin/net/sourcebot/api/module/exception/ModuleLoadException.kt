package net.sourcebot.api.module.exception

import java.io.File

class ModuleLoadException(
    file: File,
    err: Throwable
) : RuntimeException("Could not load module '${file.path}'", err)