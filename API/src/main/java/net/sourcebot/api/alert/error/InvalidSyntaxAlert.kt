package net.sourcebot.api.alert.error

import net.sourcebot.api.alert.ErrorAlert

/**
 * Called when [net.sourcebot.api.command.InvalidSyntaxException] is thrown to provide command syntax information.
 */
class InvalidSyntaxAlert(description: String) : ErrorAlert("Invalid Syntax!", description)