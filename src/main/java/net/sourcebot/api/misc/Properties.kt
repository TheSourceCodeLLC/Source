package net.sourcebot.api.misc

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.stream.JsonReader
import java.io.FileReader
import java.nio.file.Path
import java.util.function.Function

class Properties(private val json: JsonObject) {

    private operator fun <T> get(path: String, adapter: (JsonElement) -> T): T? = get(path, Function(adapter))
    private operator fun <T> get(path: String, adapter: Function<JsonElement, T>): T? {
        return adapter.apply(json[path] ?: return null)
    }

    fun getBoolean(path: String) = this[path, JsonElement::getAsBoolean]
    fun getBooleanRequired(path: String) = getRequired(path, Properties::getBoolean)

    fun getByte(path: String) = this[path, JsonElement::getAsByte]
    fun getByteRequired(path: String) = getRequired(path, Properties::getByte)

    fun getShort(path: String) = this[path, JsonElement::getAsShort]
    fun getShortRequired(path: String) = getRequired(path, Properties::getShort)

    fun getInt(path: String) = this[path, JsonElement::getAsInt]
    fun getIntRequired(path: String) = getRequired(path, Properties::getInt)

    fun getLong(path: String) = this[path, JsonElement::getAsLong]
    fun getLongRequired(path: String) = getRequired(path, Properties::getLong)

    fun getFloat(path: String) = this[path, JsonElement::getAsFloat]
    fun getFloatRequired(path: String) = getRequired(path, Properties::getFloat)

    fun getDouble(path: String) = this[path, JsonElement::getAsDouble]
    fun getDoubleRequired(path: String) = getRequired(path, Properties::getDouble)

    fun getString(path: String) = this[path, JsonElement::getAsString]
    fun getStringRequired(path: String) = getRequired(path, Properties::getString)

    fun getJsonArray(path: String) = this[path, JsonElement::getAsJsonArray]
    fun getJsonArrayRequired(path: String) = getRequired(path, Properties::getJsonArray)

    fun getJsonObject(path: String) = this[path, JsonElement::getAsJsonObject]
    fun getJsonObjectRequired(path: String) = getRequired(path, Properties::getJsonObject)

    private fun <T> getRequired(path: String, supplier: (Properties, String) -> T?): T =
        supplier(this, path) ?: throw IllegalArgumentException("Could not load value at `${path}`!")

    companion object {
        @JvmStatic fun fromPath(path: Path) = JsonReader(FileReader(path.toFile())).use {
            Properties(JsonParser.parseReader(it) as JsonObject)
        }
    }
}