package net.sourcebot.module.experience.data

import net.sourcebot.api.configuration.JsonConfiguration
import net.sourcebot.module.experience.Experience

class ExperienceData(json: JsonConfiguration) {
    var amount by json.delegateRequired { 0L }
    val level get() = Experience.getLevel(amount)
}