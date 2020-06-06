package net.sourcebot.api.alert.error

import net.sourcebot.api.alert.ErrorAlert

class GuildOnlyCommandAlert : ErrorAlert(
    "Guild Only Command!", "This command may not be used outside of a guild!"
)