package net.sourcebot.api.response.error

import net.sourcebot.api.response.ErrorResponse

class GlobalAdminOnlyResponse : ErrorResponse(
    "Global Admin Only!",
    "That command is reserved for global administrators!"
)