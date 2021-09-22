package net.sourcebot.api.command

import me.hwiggy.kommander.arguments.Arguments
import net.sourcebot.api.module.SourceModule

abstract class RootCommand : SourceCommand() {
    internal lateinit var module: SourceModule
    open val transformer: InputTransformer? = null

    abstract class InputTransformer {
        abstract fun matches(input: String): Boolean
        abstract fun transformArguments(label: String, arguments: Arguments): Arguments
    }

    abstract class RegexTransformer(
        protected val regex: Regex
    ) : InputTransformer() {
        final override fun matches(input: String) = regex.matches(input)
    }
}