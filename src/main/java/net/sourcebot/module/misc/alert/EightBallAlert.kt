package net.sourcebot.module.misc.alert

import net.sourcebot.api.alert.InfoAlert

class EightBallAlert(question: String, answer: String) : InfoAlert(
    "Magic 8-Ball",
    "**You asked:** _${question}_\n" +
    "**Source says:** _${answer}_"
)