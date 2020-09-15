package net.sourcebot.api.command

import net.sourcebot.api.command.argument.Arguments

abstract class InputTransformer(val regex: Regex) {
    abstract fun transformArguments(label: String, arguments: Arguments): Arguments
}