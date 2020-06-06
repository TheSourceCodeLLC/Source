package net.sourcebot.api.alert.error

import net.sourcebot.api.alert.ErrorAlert

/**
 * Called when a user tries to use a command that they do not have permission to use.
 */
class NoPermissionAlert : ErrorAlert("No Permission!", "You do not have permission to use that command!")