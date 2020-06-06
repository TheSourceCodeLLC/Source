package net.sourcebot.api.alert.error

import net.sourcebot.api.alert.ErrorAlert

/**
 * Renders an [ErrorAlert] for the provided [Throwable] and outputs the original stacktrace.
 *
 * @param[throwable] The [Throwable] that was caught to fire this alert.
 */
class ExceptionAlert(throwable: Throwable) : ErrorAlert("Exception!", throwable.toString()) {
    init {
        throwable.printStackTrace()
    }
}