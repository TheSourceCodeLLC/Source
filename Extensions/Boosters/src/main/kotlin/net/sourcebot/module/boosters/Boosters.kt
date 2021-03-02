package net.sourcebot.module.boosters

import net.dv8tion.jda.api.entities.Member
import net.sourcebot.api.configuration.JsonConfiguration
import net.sourcebot.api.module.SourceModule
import net.sourcebot.module.boosters.data.Booster
import net.sourcebot.module.boosters.listener.BoosterListener
import net.sourcebot.module.profiles.Profiles
import java.time.Instant

class Boosters : SourceModule() {
    override fun onEnable() {
        subscribeEvents(BoosterListener())
    }

    companion object {
        @JvmStatic operator fun get(
            member: Member,
            type: String
        ): Booster {
            val booster = Profiles.proxyObject(member, "boosters", ::JsonConfiguration).required(type) {
                Booster(1.0, null)
            }
            return when {
                booster.expiry?.isAfter(Instant.now()) != true -> {
                    set(member, type, Booster(1.0, null))
                }
                else -> booster
            }
        }

        @JvmStatic operator fun set(
            member: Member,
            type: String,
            booster: Booster
        ) = booster.also {
            Profiles.proxyObject(member, "boosters") { it }[type] = it
        }

        @JvmStatic fun getAll(member: Member): Map<String, Booster> {
            val boosters = Profiles.proxyObject(member, "boosters") { it }
            return boosters.asMap().keys.associateWith {
                Boosters[member, it]
            }
        }
    }
}