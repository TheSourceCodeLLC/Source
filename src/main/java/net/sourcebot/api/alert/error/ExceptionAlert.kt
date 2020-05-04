package net.sourcebot.api.alert.error

import net.sourcebot.api.alert.ErrorAlert

class ExceptionAlert(exception: Throwable) : ErrorAlert("Exception!", exception.toString()) {
    init {
        exception.printStackTrace()
    }
}