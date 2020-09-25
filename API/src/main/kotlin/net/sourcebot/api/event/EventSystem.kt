package net.sourcebot.api.event

import com.google.common.collect.Multimaps
import com.google.common.collect.SetMultimap
import net.sourcebot.api.module.SourceModule
import java.util.*

class EventSystem<E : Any> {
    private val bus: SetMultimap<Class<out E>, RegisteredListener<out E>> = Multimaps.newSetMultimap(
        IdentityHashMap(), ::HashSet
    )

    fun fireEvent(event: E) = bus[event.javaClass]?.forEach {
        try {
            it(event)
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    } ?: Unit

    fun <T : E> listen(
        module: SourceModule,
        type: Class<T>,
        listener: (T) -> Unit
    ) {
        bus.put(type, RegisteredListener(module, listener))
    }

    inline fun <reified T : E> listen(
        module: SourceModule,
        noinline listener: (T) -> Unit
    ) = listen(module, T::class.java, listener)

    fun <T> unregister(
        type: Class<T>
    ) = bus.entries().removeIf { it.key == type }

    fun unregister(
        module: SourceModule
    ) = bus.entries().removeIf { it.value.module == module }

    fun <T : E> unregister(
        type: Class<T>,
        module: SourceModule
    ) = bus[type]?.removeIf { it.module == module } ?: false
}