package net.sourcebot.api.configuration

import com.fasterxml.jackson.core.type.TypeReference
import net.sourcebot.api.typeRefOf

interface Configuration {
    operator fun <T> set(path: String, obj: T): T
    fun <T> optRef(path: String, typeRef: TypeReference<T>): T?
    fun <T> reqRef(path: String, typeRef: TypeReference<T>, supplier: () -> T? = { null }): T
}

@JvmOverloads
inline fun <reified T> Configuration.required(path: String, noinline supplier: () -> T? = { null }): T =
    reqRef(path, typeRefOf(), supplier)

inline fun <reified T> Configuration.optional(path: String): T? = optRef(path, typeRefOf())