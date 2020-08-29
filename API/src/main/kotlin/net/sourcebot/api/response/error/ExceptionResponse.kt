package net.sourcebot.api.response.error

import net.sourcebot.api.response.ErrorResponse

/**
 * Renders an [ErrorResponse] for the provided [Throwable] and outputs the original stacktrace.
 *
 * @param[throwable] The [Throwable] that was caught to fire this alert.
 */
class ExceptionResponse(throwable: Throwable) : ErrorResponse(
    "Exception!",
    throwable.toString()
)