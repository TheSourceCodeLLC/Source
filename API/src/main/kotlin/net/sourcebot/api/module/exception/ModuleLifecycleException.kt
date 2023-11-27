package net.sourcebot.api.module.exception

class ModuleLifecycleException(
    moduleName: String,
    cause: Throwable
) : RuntimeException("Module '$moduleName' encountered an exception!", cause)