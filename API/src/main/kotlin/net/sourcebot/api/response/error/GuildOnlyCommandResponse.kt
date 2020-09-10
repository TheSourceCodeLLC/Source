package net.sourcebot.api.response.error

import net.sourcebot.api.response.ErrorResponse

/**
 * Called when a user uses a command marked as guildOnly outside of a Guild (i.e Direct Message)
 */
class GuildOnlyCommandResponse : ErrorResponse(
    "Guild Only Command!", "This command may not be used outside of a guild!"
)