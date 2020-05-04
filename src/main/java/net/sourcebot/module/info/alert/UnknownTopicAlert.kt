package net.sourcebot.module.info.alert

import net.sourcebot.api.alert.ErrorAlert

class UnknownTopicAlert(private val topic: String) : ErrorAlert(
    "Unknown Topic!",
    "There is no such command or module named `$topic`!"
)