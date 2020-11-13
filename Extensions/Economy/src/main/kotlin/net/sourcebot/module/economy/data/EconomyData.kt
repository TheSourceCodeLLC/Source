package net.sourcebot.module.economy.data

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.databind.node.ObjectNode
import net.dv8tion.jda.api.entities.Member
import net.sourcebot.api.configuration.JsonConfiguration
import net.sourcebot.module.profiles.Profiles
import java.time.Instant

class EconomyData(profile: JsonConfiguration) {
    var balance by profile.delegateRequired("economy.balance") { 0L }
    var booster: CoinBooster? by profile.delegateOptional("economy.booster")

    companion object {
        @JvmStatic operator fun get(member: Member) = EconomyData(Profiles.getProfile(member))
    }
}

class CoinBooster @JsonCreator constructor(node: ObjectNode) : JsonConfiguration(node) {
    var multiplier by delegateRequired { 1.0 }
    var expiry by delegateOptional<Instant>()
}