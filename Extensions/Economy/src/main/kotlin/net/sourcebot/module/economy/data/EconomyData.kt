package net.sourcebot.module.economy.data

import net.sourcebot.api.configuration.JsonConfiguration
import java.time.Instant

class EconomyData(json: JsonConfiguration) {
    var balance by json.delegateRequired { 0L }
    var booster by json.delegateOptional<CoinBooster>()
    var daily by json.delegateOptional<DailyRecord>()
}

data class CoinBooster(val multiplier: Double, val expiry: Instant)
data class DailyRecord(val count: Long, val expiry: Instant)