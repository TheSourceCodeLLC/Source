package net.sourcebot.api.properties

import com.google.gson.*
import java.lang.reflect.Type
import java.util.*

interface JsonSerial<T> : JsonSerializer<T>, JsonDeserializer<T> {
    override fun serialize(
        obj: T,
        type: Type,
        context: JsonSerializationContext
    ): JsonElement

    override fun deserialize(
        element: JsonElement,
        type: Type,
        context: JsonDeserializationContext
    ): T

    companion object {
        private val serializers = IdentityHashMap<Class<*>, JsonSerial<*>>()
        @JvmStatic var gson = Gson()

        @JvmStatic fun <T> register(type: Class<T>, serial: JsonSerial<T>) {
            serializers[type] = serial
            rebuildGson()
        }

        @JvmStatic inline fun <reified T> register(
            serial: JsonSerial<T>
        ) = register(T::class.java, serial)

        @JvmStatic fun unregister(type: Class<*>) =
            if (serializers.remove(type) != null) rebuildGson() else Unit

        private fun rebuildGson() {
            val builder = GsonBuilder()
            serializers.forEach { (type, serial) -> builder.registerTypeAdapter(type, serial) }
            gson = builder.create()
        }
    }
}