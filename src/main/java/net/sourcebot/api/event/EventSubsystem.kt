package net.sourcebot.api.event

import com.google.common.collect.Multimaps
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.hooks.EventListener
import net.sourcebot.api.module.SourceModule
import java.util.*
import java.util.function.Consumer

class EventSubsystem : EventListener {
    private val listeners = Multimaps.newSetMultimap<Class<*>, RegisteredListener<GenericEvent>>(IdentityHashMap()) { HashSet() }

    override fun onEvent(event: GenericEvent) = listeners[event.javaClass]?.forEach { it.consumer.accept(event) }
                                                ?: Unit

    fun <T : GenericEvent> listen(
        owner: SourceModule,
        type: Class<T>,
        listener: Consumer<T>
    ) = listeners.put(type, RegisteredListener(owner, listener) as RegisteredListener<GenericEvent>)

    inline fun <reified T : GenericEvent> listen(
        owner: SourceModule,
        noinline listener: (T) -> Unit
    ) = listen(owner, T::class.java, Consumer(listener))

    fun unregister(module: SourceModule) = listeners.values().removeIf { it.owner == module }
}