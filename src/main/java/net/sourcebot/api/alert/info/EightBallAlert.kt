package net.sourcebot.api.alert.info

import net.sourcebot.api.alert.InfoAlert

class EightBallAlert(question: String, answer: String) : InfoAlert(
    "Magic 8-Ball",
    "**You asked:** _${question}_\n" +
    "**Source says:** _${answer}_"
)