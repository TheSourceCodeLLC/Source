package net.sourcebot.module.experience.data

import net.sourcebot.api.configuration.JsonConfiguration
import java.time.Instant

class ExperienceData(json: JsonConfiguration) {
    var amount by json.delegateRequired { 0L }
    var booster by json.delegateOptional<ExperienceBooster>()
}

data class ExperienceBooster(val multiplier: Double, val expiry: Instant)