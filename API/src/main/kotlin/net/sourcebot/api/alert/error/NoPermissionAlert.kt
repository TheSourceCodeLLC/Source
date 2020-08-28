package net.sourcebot.api.alert.error

import net.sourcebot.api.alert.ErrorAlert

class NoPermissionAlert : ErrorAlert(
    "No Permission!",
    "You do not have permission to use that command!"
)