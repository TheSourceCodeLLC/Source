package net.sourcebot.api.alert.error

import net.sourcebot.api.alert.ErrorAlert

class InvalidSyntaxAlert(description: String) : ErrorAlert("Invalid Syntax!", description)