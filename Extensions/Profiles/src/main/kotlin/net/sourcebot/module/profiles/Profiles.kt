package net.sourcebot.module.profiles

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.cache.LoadingCache
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import net.sourcebot.Source
import net.sourcebot.api.module.SourceModule
import net.sourcebot.module.profiles.command.ProfileCommand
import net.sourcebot.module.profiles.data.ProfileHandler
import java.util.concurrent.TimeUnit

class Profiles : SourceModule() {
    override fun onEnable() {
        profiles = CacheBuilder.newBuilder().weakKeys()
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .removalListener<Guild, ProfileHandler> { (_, v) -> v.saveAll() }
            .build(object : CacheLoader<Guild, ProfileHandler>() {
                override fun load(key: Guild) =
                    ProfileHandler(Source.MONGODB.getCollection(key.id, "profiles"))
            })
        registerCommands(ProfileCommand())
    }

    companion object {
        private lateinit var profiles: LoadingCache<Guild, ProfileHandler>

        @JvmStatic operator fun get(member: Member) = profiles[member.guild][member]
    }
}