package net.sourcebot.api.module

class UnknownDependencyException(
    dependencies: Set<String>
) : RuntimeException("Unknown Dependencies: ${dependencies.joinToString()}")