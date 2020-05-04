package net.sourcebot.api.misc

import com.google.common.collect.Multimaps
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.hooks.EventListener
import java.util.*
import java.util.function.Consumer

class EventSubsystem : EventListener {
    private val listeners = Multimaps.newSetMultimap<Class<*>, Consumer<GenericEvent>>(IdentityHashMap()) { HashSet() }

    override fun onEvent(event: GenericEvent) = listeners[event.javaClass]?.forEach { it.accept(event) } ?: Unit

    fun <T : GenericEvent> listen(
        type: Class<T>,
        listener: Consumer<T>
    ) = listeners.put(type, listener as Consumer<GenericEvent>)

    inline fun <reified T : GenericEvent> listen(
        noinline listener: (T) -> Unit
    ) = listen(T::class.java, Consumer(listener))
}