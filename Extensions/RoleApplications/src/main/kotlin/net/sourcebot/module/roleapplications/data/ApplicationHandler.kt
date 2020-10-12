package net.sourcebot.module.roleapplications.data

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import net.dv8tion.jda.api.entities.Guild
import net.sourcebot.api.database.MongoDB
import java.util.concurrent.TimeUnit

class ApplicationHandler(
    private val mongodb: MongoDB
) {

    private val applications = CacheBuilder.newBuilder().weakKeys()
        .expireAfterWrite(10, TimeUnit.MINUTES)
        .build(object : CacheLoader<Guild, ApplicationCache>() {
            override fun load(guild: Guild) = ApplicationCache(mongodb.getCollection(guild.id, "applications"))
        })

    operator fun get(guild: Guild): ApplicationCache = applications[guild]

}