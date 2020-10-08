package net.sourcebot.api.response.error

import net.sourcebot.api.response.StandardErrorResponse

class GlobalAdminOnlyResponse : StandardErrorResponse(
    "Global Admin Only!",
    "That command is reserved for global administrators!"
)