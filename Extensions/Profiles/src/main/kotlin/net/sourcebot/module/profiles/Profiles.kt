package net.sourcebot.module.profiles

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.cache.LoadingCache
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import net.sourcebot.Source
import net.sourcebot.api.configuration.JsonConfiguration
import net.sourcebot.api.module.SourceModule
import net.sourcebot.module.profiles.data.ProfileHandler
import java.util.concurrent.TimeUnit

class Profiles : SourceModule() {
    override fun onEnable() {
        guildProfiles = CacheBuilder.newBuilder().weakKeys()
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .removalListener<Guild, ProfileHandler> { (_, v) -> v.saveAll() }
            .build(object : CacheLoader<Guild, ProfileHandler>() {
                override fun load(key: Guild) =
                    ProfileHandler(Source.MONGODB.getCollection(key.id, "profiles"))
            })
    }

    companion object {
        private lateinit var guildProfiles: LoadingCache<Guild, ProfileHandler>

        @JvmStatic
        fun getProfile(member: Member) = guildProfiles[member.guild][member.id]
        @JvmStatic
        fun saveProfile(
            member: Member,
            profile: JsonConfiguration
        ) = guildProfiles[member.guild].save(member.id, profile)
    }
}