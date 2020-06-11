package net.sourcebot.api.alert.error

import net.sourcebot.api.alert.ErrorAlert

class NoPermissionDMAllowedAlert : ErrorAlert(
    "No Permission!",
    "You don't have permission to use that command here, but you do in DMs!"
)