package net.sourcebot.module.experience.data

import net.sourcebot.api.configuration.JsonConfiguration

class ExperienceData(json: JsonConfiguration) {
    var amount by json.delegateRequired { 0L }
}