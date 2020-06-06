package net.sourcebot.api.alert.error

import net.sourcebot.api.alert.ErrorAlert

/**
 * Called when a user uses a command marked as guildOnly outside of a Guild (i.e Direct Message)
 */
class GuildOnlyCommandAlert : ErrorAlert(
    "Guild Only Command!", "This command may not be used outside of a guild!"
)