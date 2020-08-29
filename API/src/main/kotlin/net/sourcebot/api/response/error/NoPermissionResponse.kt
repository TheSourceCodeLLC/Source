package net.sourcebot.api.response.error

import net.sourcebot.api.response.ErrorResponse

class NoPermissionResponse : ErrorResponse(
    "No Permission!",
    "You do not have permission to use that command!"
)