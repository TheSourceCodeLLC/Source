package net.sourcebot.api.database

import org.bson.Document
import java.util.*

interface MongoSerial<T> {
    fun queryDocument(obj: T) = Document()
    fun deserialize(document: Document): T
    fun serialize(obj: T): Document

    companion object {
        private val serializers = IdentityHashMap<Class<*>, MongoSerial<*>>()

        @JvmStatic
        fun register(type: Class<*>, serial: MongoSerial<*>) {
            serializers[type] = serial
        }

        @JvmStatic
        inline fun <reified T> register(
            serial: MongoSerial<T>
        ) = register(T::class.java, serial)

        @JvmStatic
        fun unregister(type: Class<*>) {
            serializers.remove(type)
        }

        @JvmStatic
        fun <T> fromDocument(document: Document, type: Class<T>) =
            getSerializerThrowing(type).deserialize(document)

        @JvmStatic
        inline fun <reified T> fromDocument(document: Document) =
            fromDocument(document, T::class.java)

        @JvmStatic
        fun <T> toDocument(obj: T, type: Class<T>) =
            getSerializerThrowing(type).serialize(obj)

        @JvmStatic
        inline fun <reified T> toDocument(obj: T) =
            toDocument(obj, T::class.java)

        @JvmStatic
        fun <T> getQueryDocument(obj: T, type: Class<T>) =
            getSerializerThrowing(type).queryDocument(obj)

        @JvmStatic
        inline fun <reified T> getQueryDocument(obj: T) =
            getQueryDocument(obj, T::class.java)

        @Suppress("UNCHECKED_CAST")
        private fun <T> getSerializer(type: Class<T>) = serializers[type] as? MongoSerial<T>
        private fun <T> getSerializerThrowing(type: Class<T>) =
            getSerializer(type) ?: throw IllegalArgumentException("No MongoSerial available for $type !")
    }
}