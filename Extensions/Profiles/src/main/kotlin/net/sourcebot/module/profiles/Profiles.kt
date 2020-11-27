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
import net.sourcebot.module.profiles.listener.ProfileListener
import org.bson.Document
import java.time.Instant
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

class Profiles : SourceModule() {
    private lateinit var cleanup: ScheduledFuture<*>
    override fun onEnable() {
        profiles = CacheBuilder.newBuilder().weakKeys()
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .removalListener<Guild, ProfileHandler> { (_, v) -> v.saveAll() }
            .build(object : CacheLoader<Guild, ProfileHandler>() {
                override fun load(key: Guild) = ProfileHandler(key)
            })
        registerCommands(ProfileCommand())
        subscribeEvents(ProfileListener())
        cleanup = Source.SCHEDULED_EXECUTOR_SERVICE.scheduleAtFixedRate({
            Source.SHARD_MANAGER.guilds.forEach { guild ->
                Source.EXECUTOR_SERVICE.submit {
                    val profiles = Source.MONGODB.getCollection(guild.id, "profiles")
                    profiles.find(Document("data.expiry", Document("\$exists", true))).forEach {
                        val expiry = it.getEmbedded(listOf("data", "expiry"), 0L).let(
                            Instant::ofEpochMilli
                        )
                        if (Instant.now().isAfter(expiry)) profiles.deleteOne(Document("_id", it["_id"]))
                    }
                }
            }
        }, 0L, 10L, TimeUnit.SECONDS)
    }

    override fun onDisable() {
        cleanup.cancel(true)
    }

    companion object {
        private lateinit var profiles: LoadingCache<Guild, ProfileHandler>

        @JvmStatic operator fun get(member: Member) = profiles[member.guild][member.id]
        @JvmStatic operator fun get(guild: Guild, id: String) = profiles[guild][id]
    }
}