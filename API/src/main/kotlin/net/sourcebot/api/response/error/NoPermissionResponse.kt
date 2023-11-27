package net.sourcebot.api.response.error

import net.sourcebot.api.response.StandardErrorResponse

class NoPermissionResponse : StandardErrorResponse(
    "No Permission!",
    "You do not have permission to use that command!"
)