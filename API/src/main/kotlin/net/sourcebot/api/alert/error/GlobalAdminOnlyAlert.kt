package net.sourcebot.api.alert.error

import net.sourcebot.api.alert.ErrorAlert

class GlobalAdminOnlyAlert : ErrorAlert(
    "Global Admin Only!",
    "That command is reserved for global administrators!"
)