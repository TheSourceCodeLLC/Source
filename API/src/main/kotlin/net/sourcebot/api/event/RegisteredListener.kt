package net.sourcebot.api.event

import net.sourcebot.api.module.SourceModule

@Suppress("UNCHECKED_CAST")
internal class RegisteredListener<E>(
    internal val module: SourceModule,
    private val listener: (E) -> Unit
) {
    operator fun <T> invoke(event: T) = listener(event as E)
}