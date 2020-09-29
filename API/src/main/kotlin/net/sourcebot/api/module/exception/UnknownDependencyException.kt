package net.sourcebot.api.module.exception

class UnknownDependencyException(
    dependencies: Set<String>
) : RuntimeException("Unknown Dependencies: ${dependencies.joinToString()}")