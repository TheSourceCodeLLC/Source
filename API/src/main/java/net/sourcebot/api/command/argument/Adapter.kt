package net.sourcebot.api.command.argument

/**
 * Represents an adapter to convert command [Arguments] to a desired [T]
 */
class Adapter<T>(adapter: (Arguments) -> T?) : (Arguments) -> T? by adapter {
    companion object {
        @JvmStatic val BOOLEAN = ofSingleArg(String::toBoolean)
        @JvmStatic val SHORT = ofSingleArg(String::toShort)
        @JvmStatic val INTEGER = ofSingleArg(String::toInt)
        @JvmStatic val LONG = ofSingleArg(String::toLong)
        @JvmStatic val FLOAT = ofSingleArg(String::toFloat)
        @JvmStatic val DOUBLE = ofSingleArg(String::toDouble)

        @JvmStatic fun <T> ofSingleArg(adapter: (String) -> T): Adapter<T> = Adapter {
            val arg = it.next() ?: return@Adapter null
            adapter(arg)
        }
    }
}