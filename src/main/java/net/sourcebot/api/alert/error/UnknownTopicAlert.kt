package net.sourcebot.api.alert.error

import net.sourcebot.api.alert.ErrorAlert

class UnknownTopicAlert(topic: String) : ErrorAlert(
    "Unknown Topic!",
    "There is no such command or module named `$topic`!"
)