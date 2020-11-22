package net.sourcebot.module.economy.data

import net.sourcebot.api.configuration.JsonConfiguration
import java.time.Instant

class EconomyData(json: JsonConfiguration) {
    var balance by json.delegateRequired { 0L }
    var daily by json.delegateRequired { 0L }
    var booster by json.delegateOptional<CoinBooster>()
}

data class CoinBooster(val multiplier: Double = 1.0, val expiry: Instant? = null)