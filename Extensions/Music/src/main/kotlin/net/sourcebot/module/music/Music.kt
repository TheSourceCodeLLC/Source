package net.sourcebot.module.music

import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager
import net.dv8tion.jda.api.entities.Guild
import net.sourcebot.api.module.SourceModule
import net.sourcebot.module.music.audio.AudioSubsystem
import net.sourcebot.module.music.command.*
import net.sourcebot.module.music.youtube.YoutubeAPI

class Music : SourceModule() {

    override fun enable() {
        YOUTUBE_API = YoutubeAPI(config.required("api-key"))
        AudioSourceManagers.registerRemoteSources(PLAYER_MANAGER)
        registerCommands(
            PauseCommand(),
            ResumeCommand(),
            SkipCommand(),
            PlayCommand(),
            VolumeCommand(),
            SearchCommand()
        )
    }

    override fun disable() {
        PLAYER_MANAGER.shutdown()
    }

    companion object {
        @JvmStatic
        lateinit var YOUTUBE_API: YoutubeAPI
            internal set

        @JvmStatic
        val PLAYER_MANAGER = DefaultAudioPlayerManager().also {
            it.registerSourceManager(YoutubeAudioSourceManager())
        }

        @JvmStatic
        val SUBSYSTEM_CACHE = HashMap<String, AudioSubsystem>()

        @JvmStatic
        fun getSubsystem(
            guild: Guild
        ): AudioSubsystem = SUBSYSTEM_CACHE.computeIfAbsent(guild.id) {
            AudioSubsystem(PLAYER_MANAGER.createPlayer()).also { it.applyTo(guild) }
        }
    }
}