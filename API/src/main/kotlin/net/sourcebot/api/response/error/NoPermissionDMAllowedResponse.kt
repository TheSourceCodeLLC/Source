package net.sourcebot.api.response.error

import net.sourcebot.api.response.ErrorResponse

class NoPermissionDMAllowedResponse : ErrorResponse(
    "No Permission!",
    "You don't have permission to use that command here, but you do in DMs!"
)