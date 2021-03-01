package net.sourcebot.api.configuration

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import net.sourcebot.api.database.MongoSerial
import net.sourcebot.api.typeRefOf
import org.bson.Document
import java.io.File
import kotlin.reflect.KProperty

open class JsonConfiguration @JsonCreator constructor(
    internal val json: ObjectNode = JsonSerial.newObject()
) {
    constructor(json: JsonConfiguration) : this(json.json)
    constructor(map: Map<String, Any?>) : this() {
        map.forEach(::set)
    }

    operator fun <T> set(path: String, obj: T): T {
        if (path.isBlank()) throw IllegalArgumentException(
            "Argument 'path' may not be empty!"
        )
        val parts = path.split(".")
        if (parts.size == 1) {
            if (obj == null) json.remove(parts[0])
            else json.set(parts[0], JsonSerial.toJson(obj))
        } else {
            val config = required(parts[0], ::JsonConfiguration)
            config[parts.subList(1, parts.size).joinToString(".")] = obj
            set(parts[0], config)
        }
        onChange()
        return obj
    }

    fun <T> optional(path: String, typeReference: TypeReference<T>): T? {
        val levels = path.split(".").iterator()
        var lastElem: JsonNode? = json[levels.next()]
        while (lastElem != null && levels.hasNext()) {
            lastElem = lastElem[levels.next()]
        }
        return lastElem?.let { JsonSerial.fromJson(it, typeReference) }
    }

    inline fun <reified T> optional(path: String): T? = optional(path, typeRefOf())

    @JvmOverloads
    fun <T> required(
        path: String,
        typeReference: TypeReference<T>,
        supplier: () -> T? = { null }
    ): T = optional(path, typeReference) ?: supplier()?.let { set(path, it) }
    ?: throw IllegalArgumentException("Could not load value at '$path'!")

    @JvmOverloads
    inline fun <reified T> required(
        path: String,
        noinline supplier: () -> T? = { null }
    ): T = required(path, typeRefOf(), supplier)

    class JsonSerialization : JsonSerial<JsonConfiguration> {
        override val serializer = object : StdSerializer<JsonConfiguration>(JsonConfiguration::class.java) {
            override fun serialize(
                value: JsonConfiguration,
                gen: JsonGenerator,
                provider: SerializerProvider
            ) = gen.writeTree(value.json)
        }
        override val deserializer = object : StdDeserializer<JsonConfiguration>(JsonConfiguration::class.java) {
            override fun deserialize(
                p: JsonParser,
                ctxt: DeserializationContext
            ): JsonConfiguration = JsonConfiguration(p.readValueAsTree<ObjectNode>())
        }
    }

    class MongoSerialization : MongoSerial<JsonConfiguration> {
        override fun deserialize(document: Document) = JsonConfiguration(document)
        override fun serialize(obj: JsonConfiguration) = Document(obj.asMap())
    }

    fun asMap(): Map<String, Any?> = JsonSerial.fromJson(json)

    open fun onChange() = Unit

    companion object {
        @JvmStatic
        fun fromFile(file: File): JsonConfiguration = file.let {
            if (!it.exists()) JsonSerial.toFile(it, JsonSerial.newObject())
            object : JsonConfiguration(JsonSerial.fromFile<ObjectNode>(it)) {
                override fun onChange() = JsonSerial.toFile(it, this)
            }
        }
    }

    @JvmOverloads inline fun <reified T> delegateOptional(
        path: String? = null,
    ) = OptionalDelegate(path, typeRefOf<T?>())

    @JvmOverloads inline fun <reified T> delegateRequired(
        path: String? = null,
        noinline supplier: () -> T? = { null }
    ) = RequiredDelegate(path, typeRefOf(), supplier)

    inner class OptionalDelegate<T>(
        private val path: String?,
        private val typeReference: TypeReference<T>,
    ) : SimpleVarDelegate<T>(path) {
        operator fun getValue(
            thisRef: Any?, prop: KProperty<*>
        ) = this@JsonConfiguration.optional(path ?: prop.name, typeReference)
    }

    inner class RequiredDelegate<T>(
        private val path: String?,
        private val typeReference: TypeReference<T>,
        private val supplier: () -> T? = { null }
    ) : SimpleVarDelegate<T>(path) {
        operator fun getValue(thisRef: Any?, prop: KProperty<*>): T =
            this@JsonConfiguration.required(path ?: prop.name, typeReference, supplier)
    }

    open inner class SimpleVarDelegate<T>(private val path: String?) {
        operator fun setValue(
            thisRef: Any?, prop: KProperty<*>, value: T
        ) {
            this@JsonConfiguration[path ?: prop.name] = value
        }
    }

    private val proxyCache = HashMap<String, Any>()
    fun <T : Any> proxyObj(path: String, constructor: (JsonConfiguration) -> T): T {
        val exterior = this
        val json = required(path, ::JsonConfiguration)
        return (proxyCache[path] ?: object : JsonConfiguration(json) {
            override fun onChange() {
                exterior[path] = this
            }
        }.let<JsonConfiguration, T>(constructor).also {
            proxyCache[path] = it
        }) as T
    }
}