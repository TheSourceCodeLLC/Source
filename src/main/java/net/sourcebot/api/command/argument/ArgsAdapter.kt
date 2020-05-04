package net.sourcebot.api.command.argument

class ArgsAdapter<T>(adapter: (Arguments) -> T?) : (Arguments) -> T? by adapter {
    companion object {
        @JvmStatic val BOOLEAN = ArgsAdapter { it.next()?.toBoolean() }
        @JvmStatic val SHORT = ArgsAdapter { it.next()?.toShort() }
        @JvmStatic val INTEGER = ArgsAdapter { it.next()?.toInt() }
        @JvmStatic val LONG = ArgsAdapter { it.next()?.toLong() }
        @JvmStatic val FLOAT = ArgsAdapter { it.next()?.toFloat() }
        @JvmStatic val DOUBLE = ArgsAdapter { it.next()?.toDouble() }
    }
}